package com.rebirth.my.ranking;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rebirth.my.mypage.MypageVo;

@Service
public class RankingService {

    @Autowired
    private RankingDao rankingDao;

    public List<RankingVo> getRankingList(String type, int page, int size) {
        int offset = (page - 1) * size;

        List<RankingVo> list = rankingDao.selectRankingList(type, offset, size);

        // 포맷팅 및 이미지 경로 처리
        for (RankingVo vo : list) {
            String avatarUrl = vo.getAvatarUrl();

            // 1. Handle Legacy Local Paths
            if (avatarUrl != null) {
                if (avatarUrl.startsWith("C:/profile_upload/")) {
                    // Legacy path used in MypageService
                    vo.setAvatarUrl(avatarUrl.replace("C:/profile_upload/", "/profile-images/"));
                } else if (avatarUrl.startsWith("C:\\")) {
                    // Generic Windows Absolute Path handling
                    // If it contains "uploads" folder, try to serve relatively
                    int uploadsIndex = avatarUrl.indexOf("uploads");
                    if (uploadsIndex != -1) {
                        String relativePath = "/" + avatarUrl.substring(uploadsIndex).replace("\\", "/");
                        vo.setAvatarUrl(relativePath);
                    }
                }
            }

            // 2. Default Image if null or empty
            if (vo.getAvatarUrl() == null || vo.getAvatarUrl().isEmpty()) {
                vo.setAvatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + vo.getNickname());
            }

            // Score Formatting
            if ("donation".equals(type)) {
                vo.setFormattedScore(String.format("%,d 회", vo.getDonationCount()));
            } else if ("sales".equals(type)) {
                vo.setFormattedScore(String.format("%,d 회", vo.getSalesCount()));
            } else {
                // Default: Eco Point
                vo.setFormattedScore(String.format("%,d P", vo.getEcoPoints()));
            }
        }

        return list;
    }

    public int getTotalUserCount() {
        return rankingDao.countTotalUsers();
    }

    public MypageVo getMyRankingInfo(Long userId) {
        MypageVo vo = rankingDao.selectMyRankingDetail(userId);
        if (vo != null) {
            String avatarUrl = vo.getAvatarUrl();
            if (avatarUrl != null) {
                if (avatarUrl.startsWith("C:/profile_upload/")) {
                    vo.setAvatarUrl(avatarUrl.replace("C:/profile_upload/", "/profile-images/"));
                } else if (avatarUrl.startsWith("C:\\")) {
                    int uploadsIndex = avatarUrl.indexOf("uploads");
                    if (uploadsIndex != -1) {
                        String relativePath = "/" + avatarUrl.substring(uploadsIndex).replace("\\", "/");
                        vo.setAvatarUrl(relativePath);
                    }
                }
            }
            if (vo.getAvatarUrl() == null || vo.getAvatarUrl().isEmpty()) {
                vo.setAvatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + vo.getUserName());
            }
        }
        return vo;
    }
}
