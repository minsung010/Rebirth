package com.rebirth.my.wardrobe;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class WardrobeService {

    @Autowired
    private com.rebirth.my.chat.component.MilvusClientWrapper milvusClientWrapper;

    @Autowired
    private com.rebirth.my.chat.component.EmbeddingClient embeddingClient;

    @Autowired
    private WardrobeDao wardrobeDao;

    @Autowired
    private com.rebirth.my.service.BadgeService badgeService;

    @Autowired
    private com.rebirth.my.mapper.UserProfileMapper userProfileMapper;

    public void addClothes(WardrobeVo vo) {
        // 1. Save to Oracle
        wardrobeDao.insertClothes(vo);

        // 2. Check for badges
        try {
            Long userId = Long.parseLong(vo.getUserId());

            // 프로필의 의류 개수 동기화 및 뱃지 체크
            userProfileMapper.findById(userId).ifPresent(profile -> {
                profile.setTotalClothingCount(profile.getTotalClothingCount() + 1);
                userProfileMapper.update(profile);
            });

            badgeService.checkAndAwardBadges(userId);
        } catch (Exception e) {
            System.err.println("⚠️ Badge Check Failed: " + e.getMessage());
        }

        // 3. Index to Milvus (Async recommended in production, but Sync for now)
        try {
            Long wardrobeId = Long.parseLong(vo.getClothesId());
            Long userId = Long.parseLong(vo.getUserId());

            // Build Description
            String description = String.format(
                    "Name: %s, Category: %s, Brand: %s, Color: %s, Season: %s",
                    vo.getName() != null ? vo.getName() : "Unknown",
                    vo.getCategory(), vo.getBrand(), vo.getColor(), vo.getSeason());

            // Generate Embedding
            java.util.List<Float> embedding = embeddingClient.getEmbedding(description);
            if (!embedding.isEmpty()) {
                milvusClientWrapper.insertVector(wardrobeId, userId, embedding, description);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Milvus Indexing Failed: " + e.getMessage());
            // Do not rollback Oracle transaction, just log error
        }
    }

    public List<WardrobeVo> getMyWardrobe(String userId) {
        return wardrobeDao.selectClothesList(userId);
    }

    public List<WardrobeVo> getClothesByIds(java.util.List<Long> ids) {
        if (ids == null || ids.isEmpty())
            return java.util.Collections.emptyList();
        return wardrobeDao.selectClothesListByIds(ids);
    }

    public List<WardrobeVo> searchClothesByKeyword(String userId, String keyword) {
        return wardrobeDao.searchClothesByKeyword(userId, keyword);
    }

    // Data Sync Method
    public int syncAllDataToMilvus() {
        System.out.println("🔄 Starting Full Data Sync to Milvus...");
        List<WardrobeVo> allClothes = wardrobeDao.selectAllClothes();
        int count = 0;

        for (WardrobeVo vo : allClothes) {
            try {
                // Check if already exists (Optional, but strict sync involves checking.
                // For now, we assume Milvus is empty or overwrite is okay provided IDs match)

                Long wardrobeId = Long.parseLong(vo.getClothesId());
                Long userId = Long.parseLong(vo.getUserId());

                String description = String.format(
                        "Name: %s, Category: %s, Brand: %s, Color: %s, Season: %s",
                        vo.getName() != null ? vo.getName() : "Unknown",
                        vo.getCategory(), vo.getBrand(), vo.getColor(), vo.getSeason());

                java.util.List<Float> embedding = embeddingClient.getEmbedding(description);
                if (!embedding.isEmpty()) {
                    milvusClientWrapper.insertVector(wardrobeId, userId, embedding, description);
                    count++;
                } else {
                    System.err.println(
                            "❌ Skipping ID " + wardrobeId + ": Failed to generate embedding (Check API Quota/Logs)");
                }

                // Rate Limit Prevention (Increased to 1s for safety)
                Thread.sleep(1000);

            } catch (Exception e) {
                System.err.println("Skipping Sync for ID " + vo.getClothesId() + ": " + e.getMessage());
            }
        }
        System.out.println("✅ Data Sync Complete. Indexed " + count + " items.");
        return count;
    }
}
