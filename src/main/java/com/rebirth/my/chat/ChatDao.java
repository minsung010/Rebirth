package com.rebirth.my.chat;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ChatDao {
    int insertChat(ChatVo vo);

    List<ChatVo> selectChatListByUserId(String userId);

    // Chatbot specific
    Long selectBotRoomId(String userId);

    void createChatRoom(ChatVo vo);

    void createBotRoom(java.util.Map<String, Object> param);

    void insertRoomMember(java.util.Map<String, Object> param);

    // Private 1:1 Chat
    Long selectPrivateRoomId(java.util.Map<String, Object> param);

    void createPrivateRoom(java.util.Map<String, Object> param);

    // Get Room Info by ID (including ITEM_ID)
    java.util.Map<String, Object> selectRoomById(Long roomId);

    // Get List of rooms for user
    List<java.util.Map<String, Object>> selectMyRoomList(Long userId);

    // 특정 방의 메시지 전체 조회
    List<ChatVo> selectMessagesByRoomId(Long roomId);

    // 대화 문맥용: 특정 방의 최근 N개 메시지 조회
    List<ChatVo> selectRecentMessages(java.util.Map<String, Object> param);

    // 채팅방 상대방 조회 (1:1 채팅)
    com.rebirth.my.domain.User getPartnerInRoom(@org.apache.ibatis.annotations.Param("roomId") Long roomId,
            @org.apache.ibatis.annotations.Param("userId") Long userId);

    // 마지막 읽은 시간 업데이트
    void updateLastReadAt(java.util.Map<String, Object> param);

    // 멤버 정보 조회
    java.util.Map<String, Object> getRoomMember(java.util.Map<String, Object> param);

    // 전체 안 읽은 메시지 수 조회
    int selectTotalUnreadCount(Long userId);

    // 채팅방 나가기
    void updateLeaveRoom(java.util.Map<String, Object> param);

    // 메시지 검색
    List<java.util.Map<String, Object>> selectSearchMessages(java.util.Map<String, Object> param);

    // 채팅방 재입장 처리 (멤버 LEFT_AT 초기화)
    void rejoinRoomMembers(Long roomId);
}
