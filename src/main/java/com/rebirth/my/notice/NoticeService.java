package com.rebirth.my.notice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NoticeService {

    @Autowired
    private NoticeDao noticeDao;

    public void writeNotice(NoticeVo vo) {
        noticeDao.insertNotice(vo);
    }

    public List<NoticeVo> getNoticeList() {
        return noticeDao.selectNoticeList();
    }

    public NoticeVo getNoticeDetail(int noticeId) {
        return noticeDao.selectNoticeDetail(noticeId);
    }

    public void increaseViewCount(int noticeId) {
        noticeDao.increaseViewCount(noticeId);
    }

    public void updateNotice(NoticeVo vo) {
        noticeDao.updateNotice(vo);
    }

    public void deleteNotice(int noticeId) {
        noticeDao.deleteNotice(noticeId);
    }

    public void registAnswer(int id, String answer) {
        NoticeVo vo = new NoticeVo();
        vo.setId(id);
        vo.setAnswer(answer);
        noticeDao.updateNoticeAnswer(vo);
    }
}
