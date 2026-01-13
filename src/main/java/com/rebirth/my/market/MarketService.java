package com.rebirth.my.market;

import com.rebirth.my.domain.MarketVo;
import com.rebirth.my.domain.User;
import com.rebirth.my.wardrobe.WardrobeDao;
import com.rebirth.my.wardrobe.WardrobeVo;
import com.rebirth.my.chat.component.GeocodingService;
import com.rebirth.my.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class MarketService {

    @Autowired
    private MarketDao marketDao;

    @Autowired
    private WardrobeDao wardrobeDao;

    @Autowired
    private GeocodingService geocodingService;

    @Autowired
    private UserMapper userMapper;

    @Transactional
    public void registerItem(MarketVo vo) {
        // 프론트엔드에서 전달된 정확한 좌표가 있으면 우선 사용
        if (vo.getLatitude() != null && vo.getLongitude() != null) {
            // 지도에서 직접 선택한 좌표가 있으므로 그대로 사용
        } else if (vo.getTradeLocation() != null && !vo.getTradeLocation().isEmpty()) {
            // 좌표가 없고 거래희망장소만 있으면 위경도로 변환
            // 괄호 안의 주소 추출 (예: "시청역 (대전 중구 은행동)" -> "대전 중구 은행동")
            String address = vo.getTradeLocation();
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\(([^)]+)\\)").matcher(address);
            if (matcher.find()) {
                address = matcher.group(1);
            }

            double[] coords = geocodingService.getCoordinates(address);
            if (coords != null) {
                vo.setLatitude(coords[0]);
                vo.setLongitude(coords[1]);
            }
        }

        Long itemId = null;

        if (vo.getClothesId() != null && !vo.getClothesId().isEmpty()) {
            // 옷장에서 판매하기: 기존 옷의 상태만 업데이트
            marketDao.updateClothesForSale(vo);
            itemId = Long.parseLong(vo.getClothesId());
        } else {
            // 새로 등록: 새 옷 INSERT
            marketDao.insertClothingItem(vo);
            itemId = vo.getId();

            // 이미지 정보가 있다면 등록
            if (vo.getImageUrl() != null && !vo.getImageUrl().isEmpty()) {
                marketDao.insertClothingImage(vo);
            }
        }

        // 추가 이미지 저장 (additionalImagesJson 파싱)
        saveAdditionalImages(itemId, vo.getAdditionalImagesJson());
    }

    /**
     * 추가 이미지 JSON 파싱 및 저장
     */
    private void saveAdditionalImages(Long clothingItemId, String additionalImagesJson) {
        if (additionalImagesJson == null || additionalImagesJson.isEmpty() || "[]".equals(additionalImagesJson)) {
            return;
        }

        try {
            // JSON 배열 파싱 (예: ["data:image/png;base64,...", "data:image/png;base64,..."])
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<String> base64Images = mapper.readValue(additionalImagesJson,
                    mapper.getTypeFactory().constructCollectionType(java.util.List.class, String.class));

            for (String base64Data : base64Images) {
                if (base64Data != null && base64Data.startsWith("data:image")) {
                    // Base64 이미지를 파일로 저장
                    String imageUrl = saveBase64ImageToFile(base64Data, clothingItemId);
                    if (imageUrl != null) {
                        marketDao.insertAdditionalImage(clothingItemId, imageUrl);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("추가 이미지 저장 오류: " + e.getMessage());
        }
    }

    /**
     * Base64 이미지를 파일로 저장하고 URL 반환
     */
    private String saveBase64ImageToFile(String base64Data, Long clothingItemId) {
        try {
            // data:image/png;base64,iVBORw0... 형식에서 실제 데이터 추출
            String[] parts = base64Data.split(",");
            if (parts.length < 2)
                return null;

            byte[] imageBytes = java.util.Base64.getDecoder().decode(parts[1]);

            // 저장 경로 설정
            String uploadDir = System.getProperty("user.dir") + "/uploads/market/";
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = "market_" + clothingItemId + "_" + java.util.UUID.randomUUID().toString().substring(0, 8)
                    + ".png";
            java.io.File file = new java.io.File(dir, fileName);

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                fos.write(imageBytes);
            }

            return "/uploads/market/" + fileName;
        } catch (Exception e) {
            System.err.println("이미지 파일 저장 오류: " + e.getMessage());
            return null;
        }
    }

    /**
     * 판매 목록 조회 (위치 정보 포함)
     */
    public List<MarketVo> getAllItems(Long userId) {
        List<MarketVo> items = marketDao.selectMarketList(userId);

        // 현재 사용자의 위치 기준점 (로그인한 경우)
        double[] userCoords = null;
        if (userId != null) {
            User user = userMapper.getUserById(userId);
            if (user != null && user.getAddress() != null) {
                userCoords = geocodingService.getCoordinates(user.getAddress());
            }
        }

        // 각 상품에 위도/경도 및 거리 설정
        for (MarketVo item : items) {
            // DB에 저장된 위경도가 있으면 사용
            if (item.getLatitude() != null && item.getLongitude() != null) {
                // 이미 DB에서 가져온 값 사용
            } else if (item.getTradeLocation() != null && !item.getTradeLocation().isEmpty()) {
                // tradeLocation으로 위경도 계산
                String address = item.getTradeLocation();
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\(([^)]+)\\)").matcher(address);
                if (matcher.find()) {
                    address = matcher.group(1);
                }
                double[] coords = geocodingService.getCoordinates(address);
                if (coords != null) {
                    item.setLatitude(coords[0]);
                    item.setLongitude(coords[1]);
                }
            } else if (item.getSellerAddress() != null && !item.getSellerAddress().isEmpty()) {
                // 폴백: 판매자 주소 사용
                double[] coords = geocodingService.getCoordinates(item.getSellerAddress());
                if (coords != null) {
                    item.setLatitude(coords[0]);
                    item.setLongitude(coords[1]);
                }
            }

            // 사용자 위치와 거리 계산
            if (userCoords != null && item.getLatitude() != null && item.getLongitude() != null) {
                double distance = calculateDistance(userCoords[0], userCoords[1], item.getLatitude(),
                        item.getLongitude());
                item.setDistance(Math.round(distance * 10.0) / 10.0);
            }
        }

        return items;
    }

    /**
     * Haversine 공식을 사용한 두 좌표 간 거리 계산 (km)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구 반경 (km)

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    public MarketVo getItemDetail(Long id, Long currentUserId) {
        MarketVo item = marketDao.selectMarketItemDetail(id);
        if (item != null) {
            // 찜 개수 설정
            item.setWishCount(marketDao.countWishes(id));

            // 현재 로그인한 사용자의 찜 여부 설정
            if (currentUserId != null) {
                int count = marketDao.checkWishExists(id, currentUserId);
                item.setWished(count > 0);
            }

            // 추가 이미지 조회
            List<String> additionalImages = marketDao.selectAdditionalImages(id);
            item.setAdditionalImages(additionalImages);
        }
        return item;
    }

    @Transactional
    public void deleteItem(Long id, Long userId) {
        int updatedRows = marketDao.cancelSale(id, userId);
        if (updatedRows == 0) {
            throw new RuntimeException("삭제 권한이 없거나 존재하지 않는 상품입니다.");
        }
    }

    // 찜하기 토글
    @Transactional
    public boolean toggleWish(Long marketId, Long userId) {
        int exists = marketDao.checkWishExists(marketId, userId);
        if (exists > 0) {
            marketDao.deleteWish(marketId, userId);
            return false; // 찜 취소됨
        } else {
            marketDao.insertWish(marketId, userId);
            return true; // 찜 추가됨
        }
    }

    // 찜 개수 조회
    public int getWishCount(Long marketId) {
        return marketDao.countWishes(marketId);
    }

    // 옷장에서 옷 정보 조회 (판매 등록 시 사용)
    public WardrobeVo getClothesInfo(String clothesId, String userId) {
        return wardrobeDao.selectClothesById(clothesId, userId);
    }

    @Transactional
    public void updateItem(MarketVo vo) {
        // 프론트엔드에서 전달된 정확한 좌표가 있으면 우선 사용
        if (vo.getLatitude() != null && vo.getLongitude() != null) {
            // 지도에서 직접 선택한 좌표 사용
        } else if (vo.getTradeLocation() != null && !vo.getTradeLocation().isEmpty()) {
            // 좌표가 없고 거래희망장소만 있으면 위경도로 변환
            String address = vo.getTradeLocation();
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\(([^)]+)\\)").matcher(address);
            if (matcher.find()) {
                address = matcher.group(1);
            }

            double[] coords = geocodingService.getCoordinates(address);
            if (coords != null) {
                vo.setLatitude(coords[0]);
                vo.setLongitude(coords[1]);
            }
        }

        marketDao.updateMarketItem(vo);
    }

    @Autowired
    private com.rebirth.my.mypage.MypageService mypageService;

    /**
     * 판매 완료 처리 및 포인트 지급
     */
    @Transactional
    public void completeSale(Long itemId, Long userId) {
        // 1. 상품 상태 '판매완료(SOLD)'로 변경
        int updated = marketDao.updateStatus(itemId, userId, "SOLD");

        if (updated > 0) {
            // 2. 판매자에게 포인트 지급 (50P)
            mypageService.addEcoPoints(userId, 50);
        } else {
            throw new RuntimeException("판매 완료 처리 실패: 해당 상품이 없거나 권한이 없습니다.");
        }
    }
}
