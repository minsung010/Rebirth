package com.rebirth.my.auth;

import com.rebirth.my.user.UserVo;
import org.apache.ibatis.annotations.Mapper;
import java.util.Map;

@Mapper
public interface AuthDao {
    // 회원가입
    void insertUser(UserVo userVo);

    // 로그인 체크 (Login ID로 조회)
    UserVo selectUserByLoginId(String loginId);

    // 이메일로 유저 찾기 (중복 체크 등)
    UserVo selectUserByEmail(String email);

    // 이메일 인증 상태 업데이트
    void updateEmailVerification(Map<String, Object> params);

    // 탈퇴 요청 (WITHDRAWAL_AT 업데이트)
    void updateWithdrawal(Map<String, Object> params);

    // 탈퇴 취소
    void cancelWithdrawal(Long userId);
}
