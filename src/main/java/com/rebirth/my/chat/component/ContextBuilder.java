package com.rebirth.my.chat.component;

import com.rebirth.my.wardrobe.WardrobeService;
import com.rebirth.my.wardrobe.WardrobeVo;
import com.rebirth.my.mapper.UserProfileMapper;
import com.rebirth.my.mapper.UserMapper;
import com.rebirth.my.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ContextBuilder {

    @Autowired
    private WardrobeService wardrobeService;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeatherService weatherService;

    public String buildUserContext(String userId) {
        Map<String, Object> context = new HashMap<>();

        // 1. System Context
        context.put("currentTime", LocalDateTime.now().toString());

        // 2. Domain Context
        try {
            Long userPk = Long.valueOf(userId);

            // A. 사용자 주소 조회 및 날씨 정보 가져오기
            User user = userMapper.getUserById(userPk);
            String userAddress = (user != null && user.getAddress() != null) ? user.getAddress() : null;
            context.put("location", userAddress != null ? userAddress : "서울");

            // B. 날씨 정보 조회
            WeatherInfo weather = weatherService.getWeatherByAddress(userAddress);
            context.put("weather", weather.getDescription());
            context.put("temperature", weather.getTemperature() + "°C");
            context.put("sky", weather.getSky());
            context.put("precipitation", weather.getPrecipitation());
            context.put("currentSeason", weatherService.getCurrentSeason(userAddress));

            // C. Wardrobe Context (Clothes List)
            List<WardrobeVo> myClothes = wardrobeService.getMyWardrobe(userId);

            // Filter: Only include available clothes (Exclude Sold, Donated, or For Sale if
            // status changes)
            // Assuming 'IN_CLOSET' is the available status.
            List<WardrobeVo> availableClothes = myClothes.stream()
                    .filter(c -> "IN_CLOSET".equals(c.getStatus()))
                    .collect(Collectors.toList());

            context.put("totalClothes", availableClothes.size());

            // Summarize clothes with season info
            List<String> clothesSummary = availableClothes.stream()
                    .limit(20) // Limit context size
                    .map((WardrobeVo c) -> {
                        String category = c.getCategory() != null ? c.getCategory() : "의류";
                        String brand = c.getBrand() != null ? c.getBrand() : "";
                        String name = c.getName() != null && !c.getName().isEmpty() ? c.getName() : "이름없음";
                        String season = c.getSeason() != null ? c.getSeason() : "사계절";
                        return String.format("%s/%s/%s(%s)", category, brand, name, season);
                    })
                    .collect(Collectors.toList());
            context.put("recentClothes", clothesSummary);

            // D. Profile/Points Context
            context.put("ecoPoints", 0);
            context.put("nickname", "User");

            userProfileMapper.findById(userPk).ifPresent(profile -> {
                context.put("ecoPoints", profile.getEcoPoints() != null ? profile.getEcoPoints() : 0);
                context.put("nickname", profile.getNickname());
                context.put("carbonSaved", profile.getTotalCarbonSavedKg());
            });

            // DEBUG: Print Context
            System.out.println("====== Built User Context ======");
            System.out.println(context);
            System.out.println("================================");

        } catch (Exception e) {
            context.put("error", "Failed to fetch user data: " + e.getMessage());
            context.put("ecoPoints", 0);
            context.put("currentSeason", "봄,가을"); // Fallback
            System.err.println("ContextBuilder Error: " + e.getMessage());
        }

        return context.toString();
    }
}
