package com.rebirth.my.wardrobe;

import com.rebirth.my.auth.CustomOAuth2User;
import com.rebirth.my.auth.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/wardrobe")
public class WardrobeApiController {

    @Autowired
    private WardrobeDao wardrobeDao;

    @Autowired
    private WardrobeService wardrobeService; // Milvus 저장용

    @jakarta.annotation.PostConstruct
    public void init() {
        System.out.println("==========================================");
        System.out.println("✅ WardrobeApiController Initialized");
        System.out.println("✅ Debug Endpoint Added: /api/wardrobe/test/milvus");
        System.out.println("==========================================");
    }

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * Spring Security에서 로그인 사용자 ID 가져오기
     */
    private Long getLoginUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return null;

        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        } else if (principal instanceof CustomOAuth2User) {
            return ((CustomOAuth2User) principal).getId();
        }
        return null;
    }

    /**
     * 분석된 의류를 옷장에 등록
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerClothes(@RequestBody WardrobeVo vo) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 로그인 사용자 ID 가져오기
            Long userId = getLoginUserId();
            if (userId == null) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(result);
            }
            vo.setUserId(String.valueOf(userId));

            // 의류 정보 저장 (Oracle + Milvus 벡터DB)
            wardrobeService.addClothes(vo);

            // 이미지 저장 (Base64 -> 파일 저장 -> CLOTHING_IMAGES 테이블에 URL 저장)
            if (vo.getImageBase64() != null && !vo.getImageBase64().isEmpty()) {
                String imageUrl = saveBase64Image(vo.getImageBase64(), vo.getClothesId());
                if (imageUrl != null) {
                    wardrobeDao.insertClothingImage(vo.getClothesId(), imageUrl);
                }
            }

            // 소재 정보 저장 (CLOTHING_MATERIALS 테이블)
            if (vo.getMaterial() != null && !vo.getMaterial().isEmpty()) {
                // 소재명으로 코드 조회 (면 -> MAT_001, 폴리에스터 -> MAT_002 등)
                String materialCode = wardrobeDao.getMaterialCode(vo.getMaterial());
                if (materialCode == null) {
                    materialCode = "MAT_UNKNOWN"; // 매칭 안되면 기본값
                }
                Integer percentage = vo.getMaterialPercentage() != null ? vo.getMaterialPercentage() : 100;
                wardrobeDao.insertClothingMaterial(vo.getClothesId(), materialCode, percentage);
            }

            result.put("success", true);
            result.put("message", "옷장에 등록되었습니다.");
            result.put("clothesId", vo.getClothesId());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "등록 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 옷장 목록 조회 (필터링 지원)
     */
    @GetMapping("/list")
    public ResponseEntity<?> getWardrobeList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String mode) {

        Long userId = getLoginUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인 필요"));
        }

        String userIdStr = String.valueOf(userId);
        List<WardrobeVo> list;

        boolean isSaleMode = "sale".equals(mode);

        // 필터링 적용
        if (category != null && !category.isEmpty() && !category.equals("전체")) {
            if (isSaleMode) {
                list = wardrobeDao.selectAvailableClothesListByCategory(userIdStr, category);
            } else {
                list = wardrobeDao.selectClothesListByCategory(userIdStr, category);
            }
        } else if (season != null && !season.isEmpty()) {
            if (isSaleMode) {
                list = wardrobeDao.selectAvailableClothesListBySeason(userIdStr, season);
            } else {
                list = wardrobeDao.selectClothesListBySeason(userIdStr, season);
            }
        } else {
            if (isSaleMode) {
                list = wardrobeDao.selectAvailableClothesList(userIdStr);
            } else {
                list = wardrobeDao.selectClothesList(userIdStr);
            }
        }

        return ResponseEntity.ok(Map.of("success", true, "data", list));
    }

    /**
     * 의류 삭제
     */
    @DeleteMapping("/{clothesId}")
    public ResponseEntity<?> deleteClothes(@PathVariable String clothesId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Long userId = getLoginUserId();
            if (userId == null) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(result);
            }

            // 본인 옷인지 확인 후 삭제
            int deleted = wardrobeDao.deleteClothes(clothesId, String.valueOf(userId));

            if (deleted > 0) {
                result.put("success", true);
                result.put("message", "삭제되었습니다.");
            } else {
                result.put("success", false);
                result.put("message", "삭제 권한이 없거나 존재하지 않는 의류입니다.");
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "삭제 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Base64 이미지를 파일로 저장
     */
    private String saveBase64Image(String base64Data, String clothesId) throws Exception {
        // data:image/png;base64, 접두사 제거
        String base64Image = base64Data;
        if (base64Data.contains(",")) {
            base64Image = base64Data.split(",")[1];
        }

        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        // 저장 디렉토리 생성
        File dir = new File(uploadDir + "/wardrobe");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 파일명 생성
        String fileName = "clothes_" + clothesId + "_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
        File file = new File(dir, fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(imageBytes);
        }

        return "/uploads/wardrobe/" + fileName;
    }

    // ==========================================
    // DEBUGGING ENDPOINT
    // ==========================================
    @Autowired
    private com.rebirth.my.chat.component.EmbeddingClient embeddingClient;

    @Autowired
    private com.rebirth.my.chat.component.MilvusClientWrapper milvusClientWrapper;

    @GetMapping("/test/milvus")
    public ResponseEntity<?> testMilvus() {
        Map<String, Object> result = new HashMap<>();
        try {
            String testText = "Red Cotton T-Shirt for Summer";
            result.put("step1_text", testText);

            // 1. Test Embedding
            long start = System.currentTimeMillis();
            List<Float> embedding = embeddingClient.getEmbedding(testText);
            long end = System.currentTimeMillis();

            if (embedding == null || embedding.isEmpty()) {
                result.put("success", false);
                result.put("message", "❌ Embedding Generation Failed (Returned Empty List). Check API Key or Network.");
                return ResponseEntity.status(500).body(result);
            }
            result.put("step2_embedding_status", "Success");
            result.put("step2_embedding_size", embedding.size());
            result.put("step2_latency_ms", end - start);

            // 2. Test Milvus Insert
            // Use a dummy ID for testing
            Long dummyId = 99999L;
            Long dummyUserId = 99999L;
            try {
                milvusClientWrapper.insertVector(dummyId, dummyUserId, embedding, "Test Item from Debug Endpoint");
                result.put("step3_milvus_status", "Success");
            } catch (Exception e) {
                result.put("step3_milvus_status", "Failed");
                result.put("step3_error", e.getMessage());
                throw e;
            }

            result.put("success", true);
            result.put("message", "✅ All Systems Go! Embedding & Milvus are working.");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "System Error: " + e.getMessage());
            result.put("stackTrace", e.getStackTrace());
            return ResponseEntity.status(500).body(result);
        }
    }
}
