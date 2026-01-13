package com.rebirth.my.ootd;

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
import java.util.*;

@RestController
@RequestMapping("/api/ootd")
public class OotdApiController {

    @Autowired
    private OotdDao ootdDao;

    @Autowired
    private OotdCommentDao commentDao;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * 로그인 사용자 ID 가져오기
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
     * OOTD 게시물 등록
     */
    @PostMapping("/create")
    public ResponseEntity<?> createOotd(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            Long userId = getLoginUserId();
            if (userId == null) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(result);
            }

            OotdVo vo = new OotdVo();
            vo.setUserId(userId);
            vo.setTitle((String) request.get("title"));
            vo.setDescription((String) request.get("description"));
            vo.setTopType((String) request.get("topType"));
            vo.setBottomType((String) request.get("bottomType"));
            vo.setTopColor((String) request.get("topColor"));
            vo.setBottomColor((String) request.get("bottomColor"));
            vo.setShoeColor((String) request.get("shoeColor"));
            vo.setBackgroundTheme((String) request.get("backgroundTheme"));
            vo.setTags((String) request.get("tags"));
            vo.setTopClothesId((String) request.get("topClothesId"));
            vo.setBottomClothesId((String) request.get("bottomClothesId"));

            // 스크린샷 이미지 저장
            String imageBase64 = (String) request.get("imageBase64");
            if (imageBase64 != null && !imageBase64.isEmpty()) {
                String imageUrl = saveBase64Image(imageBase64);
                vo.setImageUrl(imageUrl);
            }

            ootdDao.insertOotd(vo);

            result.put("success", true);
            result.put("message", "OOTD가 등록되었습니다!");
            result.put("ootdId", vo.getOotdId());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "등록 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * OOTD 목록 조회
     */
    @GetMapping("/list")
    public ResponseEntity<?> getOotdList(@RequestParam(defaultValue = "20") int limit) {
        try {
            List<OotdVo> list = ootdDao.selectOotdList(limit);

            // 로그인 사용자의 좋아요 여부 체크
            Long userId = getLoginUserId();
            if (userId != null) {
                for (OotdVo vo : list) {
                    vo.setLiked(ootdDao.countLike(vo.getOotdId(), userId) > 0);
                }
            }

            return ResponseEntity.ok(Map.of("success", true, "data", list));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 내 OOTD 목록
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyOotdList() {
        Long userId = getLoginUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인 필요"));
        }

        try {
            List<OotdVo> list = ootdDao.selectOotdListByUser(userId);
            return ResponseEntity.ok(Map.of("success", true, "data", list));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * OOTD 상세 조회
     */
    @GetMapping("/{ootdId}")
    public ResponseEntity<?> getOotdDetail(@PathVariable Long ootdId) {
        try {
            OotdVo vo = ootdDao.selectOotdById(ootdId);
            if (vo == null) {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "게시물을 찾을 수 없습니다."));
            }

            // 조회수 증가
            ootdDao.incrementViewCount(ootdId);

            // 좋아요 여부
            Long userId = getLoginUserId();
            if (userId != null) {
                vo.setLiked(ootdDao.countLike(ootdId, userId) > 0);
            }

            return ResponseEntity.ok(Map.of("success", true, "data", vo));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * OOTD 삭제
     */
    @DeleteMapping("/{ootdId}")
    public ResponseEntity<?> deleteOotd(@PathVariable Long ootdId) {
        Long userId = getLoginUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인 필요"));
        }

        try {
            int deleted = ootdDao.deleteOotd(ootdId, userId);
            if (deleted > 0) {
                return ResponseEntity.ok(Map.of("success", true, "message", "삭제되었습니다."));
            } else {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "삭제 권한이 없습니다."));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 좋아요 토글
     */
    @PostMapping("/{ootdId}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long ootdId) {
        Long userId = getLoginUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인 필요"));
        }

        try {
            boolean isLiked = ootdDao.countLike(ootdId, userId) > 0;

            if (isLiked) {
                ootdDao.deleteLike(ootdId, userId);
            } else {
                ootdDao.insertLike(ootdId, userId);
            }

            // 새로운 좋아요 수 조회
            OotdVo vo = ootdDao.selectOotdById(ootdId);
            int newLikeCount = vo != null ? (vo.getLikeCount() != null ? vo.getLikeCount() : 0) : 0;

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "liked", !isLiked,
                    "likeCount", newLikeCount));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Base64 이미지를 파일로 저장
     */
    private String saveBase64Image(String base64Data) throws Exception {
        String base64Image = base64Data;
        if (base64Data.contains(",")) {
            base64Image = base64Data.split(",")[1];
        }

        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        File dir = new File(uploadDir + "/ootd");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = "ootd_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8)
                + ".png";
        File file = new File(dir, fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(imageBytes);
        }

        return "/uploads/ootd/" + fileName;
    }

    // ==================== 댓글 API ====================

    /**
     * 댓글 목록 조회
     */
    @GetMapping("/{ootdId}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long ootdId) {
        try {
            List<OotdCommentVo> comments = commentDao.selectCommentsByOotdId(ootdId);
            int count = commentDao.countCommentsByOotdId(ootdId);
            return ResponseEntity.ok(Map.of("success", true, "data", comments, "count", count));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 댓글 작성
     */
    @PostMapping("/{ootdId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long ootdId, @RequestBody Map<String, Object> request) {
        Long userId = getLoginUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인 필요"));
        }

        try {
            String content = (String) request.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of("success", false, "message", "댓글 내용을 입력해주세요."));
            }

            OotdCommentVo vo = new OotdCommentVo();
            vo.setOotdId(ootdId);
            vo.setUserId(userId);
            vo.setContent(content.trim());

            // 대댓글인 경우
            if (request.get("parentId") != null) {
                vo.setParentId(Long.valueOf(request.get("parentId").toString()));
            }

            commentDao.insertComment(vo);

            // 새 댓글 목록 반환
            List<OotdCommentVo> comments = commentDao.selectCommentsByOotdId(ootdId);
            int count = commentDao.countCommentsByOotdId(ootdId);

            return ResponseEntity
                    .ok(Map.of("success", true, "message", "댓글이 등록되었습니다.", "data", comments, "count", count));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/{ootdId}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long ootdId, @PathVariable Long commentId) {
        Long userId = getLoginUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인 필요"));
        }

        try {
            int deleted = commentDao.deleteComment(commentId, userId);
            if (deleted > 0) {
                int count = commentDao.countCommentsByOotdId(ootdId);
                return ResponseEntity.ok(Map.of("success", true, "message", "삭제되었습니다.", "count", count));
            } else {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "삭제 권한이 없습니다."));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    /* ==================== OOTD 캘린더 API ==================== */

    /**
     * 캘린더 목록 조회
     */
    @GetMapping("/calendar/list")
    public ResponseEntity<?> getCalendarEvents() {
        Long userId = getLoginUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인 필요"));
        }

        try {
            List<OotdCalendarVo> list = ootdDao.selectCalendarEventsByUserId(userId);
            return ResponseEntity.ok(Map.of("success", true, "data", list));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 캘린더 이벤트 저장
     */
    @PostMapping("/calendar/save")
    public ResponseEntity<?> saveCalendarEvent(@RequestBody Map<String, Object> request) {
        Long userId = getLoginUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인 필요"));
        }

        try {
            OotdCalendarVo vo = new OotdCalendarVo();
            vo.setUserId(userId);
            vo.setTitle((String) request.get("title"));

            // 날짜 파싱 (yyyy-MM-dd)
            // 프론트에서 eventDate: "2025-12-31" 로 보냄
            String dateStr = (String) request.get("eventDate");
            if (dateStr != null && !dateStr.isEmpty()) {
                vo.setEventDate(java.sql.Date.valueOf(dateStr));
            }

            // 이미지 데이터 (Base64 -> CLOB 저장)
            String imageBase64 = (String) request.get("imageBase64");
            vo.setImageBase64(imageBase64);

            ootdDao.insertCalendarEvent(vo);

            return ResponseEntity.ok(Map.of("success", true, "message", "저장되었습니다.", "id", vo.getId()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 캘린더 이벤트 삭제
     */
    @DeleteMapping("/calendar/{id}")
    public ResponseEntity<?> deleteCalendarEvent(@PathVariable Long id) {
        Long userId = getLoginUserId();
        if (userId == null)
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인 필요"));

        try {
            int deleted = ootdDao.deleteCalendarEvent(id, userId);
            if (deleted > 0) {
                return ResponseEntity.ok(Map.of("success", true, "message", "삭제되었습니다."));
            } else {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "항목을 찾을 수 없습니다."));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

}
