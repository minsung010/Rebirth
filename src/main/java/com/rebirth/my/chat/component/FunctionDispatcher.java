package com.rebirth.my.chat.component;

import com.rebirth.my.wardrobe.WardrobeService;
import com.rebirth.my.ootd.OotdDao;
import com.rebirth.my.ootd.OotdCalendarVo;
// import com.rebirth.my.point.PointService; // Not yet created
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FunctionDispatcher {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private WardrobeService wardrobeService;

    @Autowired
    private com.rebirth.my.mapper.UserProfileMapper userProfileMapper;

    @Autowired
    private com.rebirth.my.mapper.UserMapper userMapper;

    @Autowired
    private MilvusClientWrapper milvusClientWrapper;

    @Autowired
    private EmbeddingClient embeddingClient;

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private OotdDao ootdDao;

    /**
     * LLM의 Function Call 요청(JSON String)을 받아 실제 메소드를 실행하고 결과를 반환
     */
    public String dispatch(String functionName, Map<String, Object> arguments) {
        try {
            switch (functionName) {
                case "getWardrobeSummary":
                    return "{\"totalItems\": " + wardrobeService.getMyWardrobe((String) arguments.get("userId")).size()
                            + "}";

                case "recommendOutfit":
                    return "{\"top\": \"White Linen Shirt\", \"bottom\": \"Beige Chinos\", \"reason\": \"Good for sunny weather\"}";

                case "getEcoPoints":
                    String userIdStr = (String) arguments.get("userId");
                    Long userPk = Long.valueOf(userIdStr);
                    // Fetch from DB
                    return userProfileMapper.findById(userPk)
                            .map(p -> "{\"currentPoints\": " + (p.getEcoPoints() != null ? p.getEcoPoints() : 0) + "}")
                            .orElse("{\"currentPoints\": 0, \"error\": \"User not found\"}");

                case "searchStyle":
                    String keyword = (String) arguments.get("keyword");
                    String uid = (String) arguments.get("userId");

                    if (keyword == null || uid == null)
                        return "{\"error\": \"Missing keyword or userId\"}";

                    // 1. Vector Search
                    java.util.List<Long> vectorIds = new java.util.ArrayList<>();
                    try {
                        java.util.List<Float> queryVector = embeddingClient.getEmbedding(keyword);
                        if (!queryVector.isEmpty()) {
                            vectorIds = milvusClientWrapper.searchSimilar(Long.valueOf(uid), queryVector, 15);
                        }
                    } catch (Exception e) {
                        System.err.println("Vector search failed: " + e.getMessage());
                    }

                    // 2. Fetch Vector Results from Oracle
                    java.util.List<com.rebirth.my.wardrobe.WardrobeVo> mergedClothes = new java.util.ArrayList<>();
                    if (!vectorIds.isEmpty()) {
                        mergedClothes.addAll(wardrobeService.getClothesByIds(vectorIds));
                    }

                    // 3. Keyword Search (DB Direct) - Hybrid Approach
                    // This ensures specifically named items (e.g. "Zara") are definitely found
                    java.util.List<com.rebirth.my.wardrobe.WardrobeVo> dbMatches = wardrobeService
                            .searchClothesByKeyword(uid, keyword);

                    // 4. Merge & Deduplicate
                    for (com.rebirth.my.wardrobe.WardrobeVo dbItem : dbMatches) {
                        boolean exists = false;
                        for (com.rebirth.my.wardrobe.WardrobeVo existing : mergedClothes) {
                            if (existing.getClothesId().equals(dbItem.getClothesId())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            mergedClothes.add(dbItem);
                        }
                    }

                    // 5. Season Filter (날씨 기반 필터링)
                    String userAddress = getUserAddress(uid);
                    String currentSeason = weatherService.getCurrentSeason(userAddress);
                    System.out.println("🌤️ [FunctionDispatcher] 현재 계절: " + currentSeason);

                    java.util.List<com.rebirth.my.wardrobe.WardrobeVo> seasonFiltered = mergedClothes.stream()
                            .filter(c -> isSeasonMatch(c.getSeason(), currentSeason))
                            .collect(java.util.stream.Collectors.toList());

                    // 만약 계절 필터링 후 결과가 없으면 원본 사용
                    if (!seasonFiltered.isEmpty()) {
                        mergedClothes = seasonFiltered;
                        System.out.println("🌤️ [FunctionDispatcher] 계절 필터링 적용: " + mergedClothes.size() + "개");
                    } else {
                        System.out.println("⚠️ [FunctionDispatcher] 계절 필터링 결과 없음, 전체 결과 사용");
                    }

                    if (mergedClothes.isEmpty())
                        return "{\"results\": [], \"message\": \"No similar items found.\"}";

                    // 5. Serialize to JSON
                    StringBuilder sb = new StringBuilder("{\"results\": [");
                    for (int i = 0; i < mergedClothes.size(); i++) {
                        com.rebirth.my.wardrobe.WardrobeVo c = mergedClothes.get(i);

                        // Robust Name Resolution
                        String displayName = c.getName();

                        // 1. If Name exists and is NOT just "Category" (e.g. not just "Top"), use it.
                        // Only trigger fallback if Name is missing OR it is literally identical to the
                        // category (lazy naming)
                        boolean isLazyName = (displayName != null && c.getCategory() != null
                                && displayName.trim().equalsIgnoreCase(c.getCategory().trim()));

                        if (displayName == null || displayName.trim().isEmpty()
                                || "Unknown".equalsIgnoreCase(displayName) || isLazyName) {

                            // Fallback: Construct name from Color + Category
                            String color = c.getColor() != null ? c.getColor() : "";
                            String category = c.getCategory() != null ? c.getCategory() : "의류";

                            // 2. Hide "Generic" Brand
                            String brand = c.getBrand();
                            if (brand == null || "Generic".equalsIgnoreCase(brand) || "Brand".equalsIgnoreCase(brand)) {
                                brand = ""; // Suppress generic brand
                            }

                            // Construct Name: [Color] [Category]
                            displayName = String.format("%s %s", color, category).trim().replaceAll("\\s+", " ");

                            // If resulting name is empty (rare), just use Category
                            if (displayName.isEmpty())
                                displayName = category;
                        }

                        // JSON Construction
                        String finalBrand = c.getBrand();
                        if (finalBrand == null || "Generic".equalsIgnoreCase(finalBrand)
                                || "Brand".equalsIgnoreCase(finalBrand)) {
                            finalBrand = ""; // Pass empty string for Generic brand
                        }

                        sb.append(String.format(
                                "{\"id\":%s, \"name\":\"%s\", \"category\":\"%s\", \"brand\":\"%s\", \"color\":\"%s\", \"season\":\"%s\"}",
                                c.getClothesId(),
                                displayName.replace("\"", "\\\"").replace("'", ""),
                                c.getCategory().replace("\"", "\\\"").replace("'", ""),
                                finalBrand.replace("\"", "\\\"").replace("'", ""),
                                c.getColor() != null ? c.getColor().replace("\"", "\\\"").replace("'", "") : "",
                                c.getSeason() != null ? c.getSeason().replace("\"", "\\\"").replace("'", "") : ""));
                        if (i < mergedClothes.size() - 1)
                            sb.append(",");
                    }
                    sb.append("]}");
                    String result = sb.toString();
                    System.out.println("🔍 [DEBUG] FunctionDispatcher searchStyle Result:");
                    System.out.println(result);
                    return result;

                case "recommendUpcycling":
                    // 사용자 옷장에서 오래된/안 입는 옷 추천
                    String upcycleUserId = (String) arguments.get("userId");
                    if (upcycleUserId == null)
                        return "{\"error\": \"userId is required\"}";

                    java.util.List<com.rebirth.my.wardrobe.WardrobeVo> allClothes = wardrobeService
                            .getMyWardrobe(upcycleUserId);

                    // 간단히 랜덤 2개 선택 (실제로는 등록일 기준 정렬 가능)
                    java.util.Collections.shuffle(allClothes);
                    java.util.List<com.rebirth.my.wardrobe.WardrobeVo> upcycleCandidates = allClothes.stream().limit(2)
                            .collect(java.util.stream.Collectors.toList());

                    StringBuilder upcycleSb = new StringBuilder();
                    upcycleSb.append("{\"suggestions\": [");
                    for (int i = 0; i < upcycleCandidates.size(); i++) {
                        com.rebirth.my.wardrobe.WardrobeVo item = upcycleCandidates.get(i);
                        String itemName = item.getName() != null ? item.getName() : item.getCategory();
                        String idea = getUpcyclingIdea(item.getCategory());
                        upcycleSb.append(String.format(
                                "{\"item\": \"%s\", \"category\": \"%s\", \"idea\": \"%s\", \"carbonSaved\": \"0.3kg\"}",
                                itemName.replace("\"", ""),
                                item.getCategory(),
                                idea));
                        if (i < upcycleCandidates.size() - 1)
                            upcycleSb.append(",");
                    }
                    upcycleSb.append("]}");
                    return upcycleSb.toString();

                case "getRecentOutfits":
                    // 최근 등록한 옷 조회
                    String outfitUserId = (String) arguments.get("userId");
                    int limit = arguments.containsKey("limit") ? Integer.parseInt(arguments.get("limit").toString())
                            : 5;

                    if (outfitUserId == null)
                        return "{\"error\": \"userId is required\"}";

                    java.util.List<com.rebirth.my.wardrobe.WardrobeVo> recentClothes = wardrobeService
                            .getMyWardrobe(outfitUserId);

                    // 최근 N개만
                    java.util.List<com.rebirth.my.wardrobe.WardrobeVo> limitedClothes = recentClothes.stream()
                            .limit(limit).collect(java.util.stream.Collectors.toList());

                    StringBuilder recentSb = new StringBuilder();
                    recentSb.append("{\"recentItems\": [");
                    for (int i = 0; i < limitedClothes.size(); i++) {
                        com.rebirth.my.wardrobe.WardrobeVo c = limitedClothes.get(i);
                        recentSb.append(String.format(
                                "{\"name\": \"%s\", \"category\": \"%s\", \"brand\": \"%s\"}",
                                c.getName() != null ? c.getName().replace("\"", "") : "",
                                c.getCategory(),
                                c.getBrand() != null ? c.getBrand().replace("\"", "") : ""));
                        if (i < limitedClothes.size() - 1)
                            recentSb.append(",");
                    }
                    recentSb.append("], \"total\": " + recentClothes.size() + "}");
                    return recentSb.toString();

                case "getWeatherByTime":
                    // 외출 시간 기반 날씨 조회
                    String weatherUserId = (String) arguments.get("userId");
                    Object hourObj = arguments.get("hour");
                    int targetHour = 12; // 기본값

                    if (hourObj != null) {
                        if (hourObj instanceof Number) {
                            targetHour = ((Number) hourObj).intValue();
                        } else {
                            targetHour = Integer.parseInt(hourObj.toString());
                        }
                    }

                    if (weatherUserId == null)
                        return "{\"error\": \"userId is required\"}";

                    // 사용자 주소 조회
                    String weatherAddress = getUserAddress(weatherUserId);
                    String weatherForecast = weatherService.getWeatherByTime(weatherAddress, targetHour);

                    return "{\"forecast\": \"" + weatherForecast.replace("\"", "'") + "\", " +
                            "\"targetHour\": " + targetHour + ", " +
                            "\"address\": \"" + (weatherAddress != null ? weatherAddress : "서울") + "\"}";

                case "getItemsForSale":
                    // 판매중인 옷만 조회
                    String saleUserId = (String) arguments.get("userId");
                    if (saleUserId == null)
                        return "{\"error\": \"userId is required\"}";

                    java.util.List<com.rebirth.my.wardrobe.WardrobeVo> allUserClothes = wardrobeService
                            .getMyWardrobe(saleUserId);

                    // IS_FOR_SALE = 'Y'인 옷만 필터링
                    java.util.List<com.rebirth.my.wardrobe.WardrobeVo> forSaleItems = allUserClothes.stream()
                            .filter(c -> "Y".equals(c.getIsForSale()))
                            .collect(java.util.stream.Collectors.toList());

                    if (forSaleItems.isEmpty()) {
                        return "{\"results\": [], \"message\": \"현재 판매중인 옷이 없습니다.\", \"count\": 0}";
                    }

                    StringBuilder saleSb = new StringBuilder();
                    saleSb.append("{\"results\": [");
                    for (int i = 0; i < forSaleItems.size(); i++) {
                        com.rebirth.my.wardrobe.WardrobeVo c = forSaleItems.get(i);
                        String itemName = c.getName() != null ? c.getName() : c.getCategory();
                        String brand = c.getBrand();
                        if (brand == null || "Generic".equalsIgnoreCase(brand)) {
                            brand = "";
                        }
                        saleSb.append(String.format(
                                "{\"id\":%s, \"name\":\"%s\", \"category\":\"%s\", \"brand\":\"%s\"}",
                                c.getClothesId(),
                                itemName.replace("\"", "").replace("'", ""),
                                c.getCategory() != null ? c.getCategory() : "",
                                brand.replace("\"", "").replace("'", "")));
                        if (i < forSaleItems.size() - 1)
                            saleSb.append(",");
                    }
                    saleSb.append("], \"count\": " + forSaleItems.size() + "}");
                    System.out.println("🏷️ [DEBUG] getItemsForSale Result: " + saleSb.toString());
                    return saleSb.toString();

                case "getOotdSchedule":
                    // OOTD 캘린더에서 특정 날짜의 스케줄 조회
                    String ootdUserId = (String) arguments.get("userId");
                    String dateStr = (String) arguments.get("date"); // "2024-12-31" 형식

                    if (ootdUserId == null || dateStr == null) {
                        return "{\"error\": \"userId and date are required\"}";
                    }

                    try {
                        Long ootdUserPk = Long.valueOf(ootdUserId);
                        java.sql.Date eventDate = java.sql.Date.valueOf(dateStr);
                        OotdCalendarVo schedule = ootdDao.selectCalendarEventByDate(ootdUserPk, eventDate);

                        if (schedule != null) {
                            String memo = schedule.getTitle() != null ? schedule.getTitle() : "메모 없음";
                            return String.format(
                                    "{\"found\": true, \"date\": \"%s\", \"memo\": \"%s\", \"hasImage\": %s}",
                                    dateStr,
                                    memo.replace("\"", "'"),
                                    schedule.getImageBase64() != null && !schedule.getImageBase64().isEmpty());
                        } else {
                            return "{\"found\": false, \"date\": \"" + dateStr
                                    + "\", \"message\": \"해당 날짜에 저장된 OOTD가 없습니다.\"}";
                        }
                    } catch (Exception e) {
                        return "{\"error\": \"날짜 형식 오류: " + e.getMessage() + "\"}";
                    }

                default:
                    return "{\"error\": \"Function not found: " + functionName + "\"}";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Execution failed: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 계절 매칭 확인
     */
    private boolean isSeasonMatch(String itemSeason, String currentSeason) {
        if (itemSeason == null || itemSeason.isEmpty() || "사계절".equals(itemSeason)) {
            return true; // 사계절 옷은 항상 매칭
        }
        if (currentSeason.contains(",")) {
            // 현재 계절이 "봄,가을" 같은 복수 계절인 경우
            return java.util.Arrays.stream(currentSeason.split(","))
                    .anyMatch(s -> s.trim().equals(itemSeason));
        }
        return itemSeason.equals(currentSeason);
    }

    /**
     * 사용자 주소 조회
     */
    private String getUserAddress(String userId) {
        try {
            Long userPk = Long.valueOf(userId);
            com.rebirth.my.domain.User user = userMapper.getUserById(userPk);
            return (user != null && user.getAddress() != null) ? user.getAddress() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 카테고리별 업사이클링 아이디어 반환
     */
    private String getUpcyclingIdea(String category) {
        if (category == null)
            return "재활용 가능한 소재로 분리배출하기";

        switch (category.toUpperCase()) {
            case "TOP":
                return "에코백이나 쿠션커버로 리폼하기";
            case "BOTTOM":
                return "파우치나 작은 가방으로 리폼하기";
            case "OUTER":
                return "담요나 러그로 업사이클링하기";
            case "DRESS":
                return "스카프나 헤어밴드로 변신시키기";
            case "SHOES":
                return "화분 커버나 소품함으로 활용하기";
            case "ACCESSORY":
                return "키링이나 장식품으로 재탄생시키기";
            default:
                return "천연 염색 후 새로운 패션 아이템으로 활용하기";
        }
    }
}
