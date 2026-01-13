package com.rebirth.my.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ChatService {

    @Autowired
    private ChatDao chatDao;

    public void sendChat(ChatVo vo) {
        chatDao.insertChat(vo);
    }

    public List<ChatVo> getChatHistory(String userId) {
        return chatDao.selectChatListByUserId(userId);
    }
}
