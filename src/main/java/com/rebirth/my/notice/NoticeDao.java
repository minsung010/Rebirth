package com.rebirth.my.notice;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface NoticeDao {
    int insertNotice(NoticeVo vo);

    List<NoticeVo> selectNoticeList();

    NoticeVo selectNoticeDetail(int noticeId);

    int updateNotice(NoticeVo vo);

    int deleteNotice(int noticeId);

    int increaseViewCount(int noticeId);

    int updateNoticeAnswer(NoticeVo vo);
}
