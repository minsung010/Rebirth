package com.rebirth.my.donation;

import com.rebirth.my.domain.MarketVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DonationService {

    @Autowired
    private DonationDao donationDao;

    @Autowired
    private com.rebirth.my.mypage.MypageService mypageService;

    public List<MarketVo> getClosetItems(Long userId) {
        return donationDao.selectClosetItems(userId);
    }

    @Transactional
    public void processDonation(List<Long> itemIds, Long userId, String disposalMethod,
            org.springframework.web.multipart.MultipartFile receiptImage) {
        // 1. 기부 처리
        for (Long itemId : itemIds) {
            donationDao.updateItemToDonated(itemId, userId, disposalMethod);
        }

        // 2. 영수증 이미지 저장 (있을 경우)
        if (receiptImage != null && !receiptImage.isEmpty()) {
            try {
                // 파일 저장 경로 설정 (사용자별 업로드 폴더 또는 공용 폴더)
                // 예: src/main/resources/static/uploads/receipts/ 날짜/
                // 실제 운영 환경에서는 외부 경로(C:/uploads 등)를 사용하고 WebMvcConfig에서 매핑해야 함
                // 여기서는 간단히 static 폴더에 저장한다고 가정 (주의: 재배포 시 삭제될 수 있음)

                String uploadDir = "src/main/resources/static/uploads/receipts/";
                java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);

                if (!java.nio.file.Files.exists(uploadPath)) {
                    java.nio.file.Files.createDirectories(uploadPath);
                }

                String originalFilename = receiptImage.getOriginalFilename();
                String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
                String newFilename = java.util.UUID.randomUUID().toString() + ext;

                java.nio.file.Path filePath = uploadPath.resolve(newFilename);
                java.nio.file.Files.copy(receiptImage.getInputStream(), filePath,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // DB 저장이 필요하다면 여기에 추가 (현재 테이블 없음)
                // donationDao.insertReceipt(userId, itemIds, "/uploads/receipts/" +
                // newFilename);

            } catch (java.io.IOException e) {
                // 로깅 후 진행 (기부는 완료 처리)
                e.printStackTrace();
            }
        }

        // 3. 에코 포인트 적립
        // 기부(DONATION)인 경우 100P, 그 외(수거 등)는 30P
        int points = "DONATION".equalsIgnoreCase(disposalMethod) ? 100 : 30;
        // 항목 개수만큼 적립할지, 한 번 요청에 적립할지는 정책에 따라 결정 (여기서는 요청 1건당 1회 적립으로 가정)
        // 만약 ID 개수만큼 적립하려면: points = points * itemIds.size();
        mypageService.addEcoPoints(userId, points);
    }
}
