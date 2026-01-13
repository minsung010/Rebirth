package com.rebirth.my.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import com.rebirth.my.chat.service.ChatbotService;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private com.rebirth.my.mapper.UserMapper userMapper;

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private com.rebirth.my.market.MarketService marketService;

    @GetMapping("/list")
    public String chatList(org.springframework.ui.Model model, java.security.Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        if (userId != null) {
            model.addAttribute("userId", userId);
        }
        return "chat/list";
    }

    @GetMapping("/bot")
    public String chatBot() {
        return "chat/bot";
    }

    @PostMapping("/api/message")
    @ResponseBody
    public String sendMessage(@RequestParam String message, java.security.Principal principal) {
        if (principal == null || "anonymousUser".equals(principal.getName())) {
            if (message.contains("옷장") || message.contains("옷") || message.contains("목록")) {
                return "PC: 나만의 옷장을 관리하고 추천을 받으려면 로그인이 필요합니다.<br><a href='/auth/login' class='underline font-bold text-primary'>로그인하고 옷장 만들기</a>";
            } else if (message.contains("포인트") || message.contains("점수") || message.contains("에코")) {
                return "PC: 나의 에코 포인트를 확인하려면 로그인이 필요합니다.<br><a href='/auth/login' class='underline font-bold text-primary'>로그인하고 포인트 쌓기</a>";
            } else if (message.contains("분석") || message.contains("진단")) {
                return "PC: AI 의류 분석을 받으려면 로그인이 필요합니다.<br><a href='/auth/login' class='underline font-bold text-primary'>로그인하고 분석 받기</a>";
            }
            return "PC: 로그인이 필요합니다.";
        }

        // Try to get User ID directly from Principal to avoid DB lookup mismatch
        // (especially for OAuth2)
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken token = (org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal;
            if (token.getPrincipal() instanceof com.rebirth.my.auth.CustomUserDetails) {
                Long userId = ((com.rebirth.my.auth.CustomUserDetails) token.getPrincipal()).getId();
                return chatbotService.processUserMessage(String.valueOf(userId), message);
            }
        } else if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) {
            org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken token = (org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) principal;
            if (token.getPrincipal() instanceof com.rebirth.my.auth.CustomOAuth2User) {
                Long userId = ((com.rebirth.my.auth.CustomOAuth2User) token.getPrincipal()).getId();
                return chatbotService.processUserMessage(String.valueOf(userId), message);
            }
        }

        // Fallback to name-based lookup
        String username = principal.getName();
        return userMapper.findByEmailOrLoginId(username)
                .map(user -> chatbotService.processUserMessage(String.valueOf(user.getId()), message))
                .orElse("PC: 로그인이 필요합니다.");
    }

    @GetMapping("")
    public String chatMain() {
        return "redirect:/chat/list"; // Default to list
    }

    // Real-time Chat
    @GetMapping("/room")
    public String chatRoom(org.springframework.ui.Model model, java.security.Principal principal,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long targetUserId,
            @RequestParam(required = false) Long itemId) {

        Long currentUserId = null;
        if (principal != null) {
            model.addAttribute("username", principal.getName());
            currentUserId = getUserIdFromPrincipal(principal);

            if (currentUserId != null) {
                com.rebirth.my.domain.User currentUser = userMapper.getUserById(currentUserId);
                if (currentUser != null) {
                    model.addAttribute("nickname", currentUser.getName());
                    model.addAttribute("userId", currentUser.getId());
                }
            }
        }

        // targetUserId가 있으면 1:1 채팅방 조회 혹은 생성
        if (targetUserId != null && currentUserId != null) {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("userId1", currentUserId);
            params.put("userId2", targetUserId);
            params.put("userId1", currentUserId);
            params.put("userId2", targetUserId);
            params.put("itemId", itemId); // [NEW] Pass itemId for filtering
            Long existingRoomId = chatDao.selectPrivateRoomId(params);

            if (existingRoomId != null) {
                roomId = existingRoomId;
            } else {
                // 방 생성
                java.util.Map<String, Object> createParams = new java.util.HashMap<>();
                createParams.put("id", null);
                createParams.put("title", "1:1 Chat");
                createParams.put("createdBy", currentUserId);
                createParams.put("id", null);
                createParams.put("title", "1:1 Chat");
                createParams.put("createdBy", currentUserId);
                createParams.put("itemId", itemId); // [NEW] Save itemId
                chatDao.createPrivateRoom(createParams);
                roomId = (Long) createParams.get("id");

                // 멤버 추가
                java.util.Map<String, Object> member1 = new java.util.HashMap<>();
                member1.put("roomId", roomId);
                member1.put("userId", currentUserId);
                chatDao.insertRoomMember(member1);

                java.util.Map<String, Object> member2 = new java.util.HashMap<>();
                member2.put("roomId", roomId);
                member2.put("userId", targetUserId);
                chatDao.insertRoomMember(member2);
            }
        }

        // 1:1 채팅방인 경우 상대방 정보 조회
        if (targetUserId != null) {
            com.rebirth.my.domain.User partner = userMapper.getUserById(targetUserId);
            if (partner != null) {
                model.addAttribute("partnerName", partner.getName());
                model.addAttribute("partnerEmail", partner.getEmail());
                model.addAttribute("partnerImg", partner.getMemImg());
                model.addAttribute("partnerId", partner.getId());
            }
        } else if (roomId != null && currentUserId != null) {
            // roomId로 상대방 조회 (채팅방 멤버 중 나 제외)
            com.rebirth.my.domain.User partner = chatDao.getPartnerInRoom(roomId, currentUserId);
            if (partner != null) {
                model.addAttribute("partnerName", partner.getName());
                model.addAttribute("partnerEmail", partner.getEmail());
                model.addAttribute("partnerImg", partner.getMemImg());
                model.addAttribute("partnerId", partner.getId());

                // 상대방의 마지막 읽은 시간 조회
                java.util.Map<String, Object> memberParam = new java.util.HashMap<>();
                memberParam.put("roomId", roomId);
                memberParam.put("userId", partner.getId());
                java.util.Map<String, Object> memberInfo = chatDao.getRoomMember(memberParam);
                if (memberInfo != null && memberInfo.get("lastReadAt") != null) {
                    model.addAttribute("partnerLastReadAt", memberInfo.get("lastReadAt").toString());
                }
            }
        }

        if (roomId != null) {
            model.addAttribute("roomId", roomId);
            // 현재 유저의 마지막 읽은 시간 업데이트
            if (currentUserId != null) {
                java.util.Map<String, Object> updateParam = new java.util.HashMap<>();
                updateParam.put("roomId", roomId);
                updateParam.put("userId", currentUserId);
                chatDao.updateLastReadAt(updateParam);
            }
        }

        // [NEW] Logic to determine Item ID (Priority: Request Param > DB Room Info)
        Long currentItemId = itemId;

        if (currentItemId == null && roomId != null) {
            try {
                // Must ensure selectRoomById is in ChatDao
                java.util.Map<String, Object> roomInfo = chatDao.selectRoomById(roomId);
                if (roomInfo != null && roomInfo.get("itemId") != null) {
                    currentItemId = Long.parseLong(String.valueOf(roomInfo.get("itemId")));
                }
            } catch (Exception e) {
                // ignore
                // Maybe selectRoomById is not yet in DAO interface, need to add it safely
            }
        }

        // [NEW] Display Product Info if itemId is present
        if (currentItemId != null) {
            try {
                // userId might be null if viewing as guest (though login required for chat
                // usually),
                // pass 0L or null to getItemDetail if logic allows, or handle exception.
                // Assuming getItemDetail handles null userId gracefully for read-only fields.
                com.rebirth.my.domain.MarketVo item = marketService.getItemDetail(currentItemId,
                        currentUserId != null ? currentUserId : 0L);
                if (item != null) {
                    model.addAttribute("itemTitle", item.getName());
                    model.addAttribute("itemPrice", item.getTargetPrice());
                    model.addAttribute("itemImage", item.getImageUrl());
                    model.addAttribute("itemTradeLocation", item.getTradeLocation());
                    model.addAttribute("itemId", item.getId());
                    model.addAttribute("itemStatus", item.getStatus());

                    boolean isSeller = item.getUserId() != null && item.getUserId().equals(currentUserId);
                    model.addAttribute("isSeller", isSeller);
                }
            } catch (Exception e) {
                // Ignore if item lookup fails, just don't show header
                System.err.println("Failed to load item info for chat: " + currentItemId);
            }
        }

        return "chat/room";
    }

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @org.springframework.messaging.handler.annotation.MessageMapping("/chat.readMessage")
    public void readMessage(
            @org.springframework.messaging.handler.annotation.Payload com.rebirth.my.domain.ChatMessage chatMessage,
            java.security.Principal principal) {

        Long currentUserId = getUserIdFromPrincipal(principal);
        if (currentUserId != null && chatMessage.getRoomId() != null) {
            // 1. DB 업데이트 (마지막 읽은 시간)
            java.util.Map<String, Object> updateParam = new java.util.HashMap<>();
            updateParam.put("roomId", chatMessage.getRoomId());
            updateParam.put("userId", currentUserId);
            chatDao.updateLastReadAt(updateParam);

            // 2. 방 멤버들에게 '읽음' 이벤트 브로드캐스트
            chatMessage.setType(com.rebirth.my.domain.ChatMessage.MessageType.READ);
            messagingTemplate.convertAndSend("/topic/room/" + chatMessage.getRoomId(), chatMessage);

            // 3. 나에게도 알림 (헤더 읽음 처리 동기화용)
            messagingTemplate.convertAndSend("/topic/user/" + currentUserId + "/chat", chatMessage);
        }
    }

    @org.springframework.messaging.handler.annotation.MessageMapping("/chat.sendMessage")
    public void sendRealTimeMessage(
            @org.springframework.messaging.handler.annotation.Payload com.rebirth.my.domain.ChatMessage chatMessage,
            java.security.Principal principal) {

        // Validate & Set Sender ID from Principal (Secure)
        Long currentUserId = getUserIdFromPrincipal(principal);
        if (currentUserId != null) {
            chatMessage.setSenderId(String.valueOf(currentUserId));
        }

        // Ensure Type is CHAT if not provided
        if (chatMessage.getType() == null) {
            chatMessage.setType(com.rebirth.my.domain.ChatMessage.MessageType.CHAT);
        }

        // Set Timestamp
        chatMessage.setTimestamp(java.time.Instant.now().toString());

        // Save to DB (1:1 채팅방인 경우만 - roomId가 있을 때만)
        if (chatMessage.getRoomId() != null) {
            chatbotService.saveMessage(chatMessage);

            // [NEW] 메시지 발생 시 모든 멤버 재입장 처리 (상대방이 나갔어도 목록에 다시 표시)
            chatDao.rejoinRoomMembers(chatMessage.getRoomId());

            messagingTemplate.convertAndSend("/topic/room/" + chatMessage.getRoomId(), chatMessage);

            // [NEW] 목록 갱신을 위한 개인 알림 전송 (User ID 기반 Topic 사용 - Principal 불일치 문제 해결)
            // 1. 나에게 (다른 탭/창 동기화)
            if (currentUserId != null) {
                messagingTemplate.convertAndSend("/topic/user/" + currentUserId + "/chat", chatMessage);
            }

            // 2. 상대방에게 (새 메시지 알림)
            try {
                com.rebirth.my.domain.User partner = chatDao.getPartnerInRoom(chatMessage.getRoomId(), currentUserId);
                if (partner != null && partner.getId() != null) {
                    messagingTemplate.convertAndSend("/topic/user/" + partner.getId() + "/chat", chatMessage);
                }
            } catch (Exception e) {
                System.err.println("Failed to notify partner topic: " + e.getMessage());
            }

        } else {
            // 전체 채팅방은 저장하지 않고 브로드캐스트만
            messagingTemplate.convertAndSend("/topic/public", chatMessage);
        }
    }

    @org.springframework.messaging.handler.annotation.MessageMapping("/chat.addUser")
    public void addUser(
            @org.springframework.messaging.handler.annotation.Payload com.rebirth.my.domain.ChatMessage chatMessage,
            org.springframework.messaging.simp.SimpMessageHeaderAccessor headerAccessor) {

        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        if (chatMessage.getRoomId() != null) {
            headerAccessor.getSessionAttributes().put("roomId", chatMessage.getRoomId());

            // JOIN 메시지는 DB에 저장하지 않음 (목록 순서 변경 방지)
            // 실시간 상태 업데이트용으로만 브로드캐스트

            messagingTemplate.convertAndSend("/topic/room/" + chatMessage.getRoomId(), chatMessage);
        } else {
            messagingTemplate.convertAndSend("/topic/public", chatMessage);
        }
    }

    @GetMapping("/api/users")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> searchUsers(@RequestParam String keyword,
            java.security.Principal principal) {
        Long currentUserId = getUserIdFromPrincipal(principal);
        if (currentUserId == null)
            return java.util.Collections.emptyList();

        return userMapper.searchUsers(keyword, currentUserId).stream()
                .map(u -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", u.getId());
                    map.put("name", u.getName());
                    map.put("email", u.getEmail());
                    map.put("memImg", u.getMemImg());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Autowired
    private com.rebirth.my.chat.ChatDao chatDao;

    @GetMapping("/api/rooms")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> getMyChatRooms(java.security.Principal principal) {
        Long currentUserId = getUserIdFromPrincipal(principal);
        if (currentUserId == null)
            return java.util.Collections.emptyList();
        return chatDao.selectMyRoomList(currentUserId);
    }

    @GetMapping("/api/messages")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> getMessagesByRoom(
            @RequestParam Long roomId,
            java.security.Principal principal) {
        Long currentUserId = getUserIdFromPrincipal(principal);
        if (currentUserId == null || roomId == null) {
            return java.util.Collections.emptyList();
        }

        // 채팅 메시지 조회 및 발신자 정보 포함
        java.util.List<com.rebirth.my.chat.ChatVo> messages = chatDao.selectMessagesByRoomId(roomId);
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();

        for (com.rebirth.my.chat.ChatVo msg : messages) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("content", msg.getContent());
            map.put("senderId", msg.getSenderId());
            map.put("createdAt", msg.getCreatedAt());
            map.put("type", msg.getMessageType());
            map.put("imageUrl", msg.getImageUrl());

            // 발신자 닉네임 조회
            if (msg.getSenderId() != null) {
                com.rebirth.my.domain.User sender = userMapper.getUserById(msg.getSenderId());
                map.put("sender", sender != null ? sender.getName() : "Unknown");
            }
            result.add(map);
        }
        return result;
    }

    @PostMapping("/api/room")
    @ResponseBody
    public Long createOrGetPrivateRoom(@RequestParam Long targetUserId,
            @RequestParam(required = false) Long itemId,
            java.security.Principal principal) {
        Long currentUserId = getUserIdFromPrincipal(principal);
        if (currentUserId == null)
            throw new RuntimeException("Login required");

        // 1. Check if room exists
        java.util.Map<String, Object> params = new java.util.HashMap<>();

        params.put("userId1", currentUserId);
        params.put("userId2", targetUserId);
        params.put("itemId", itemId); // [NEW] Match specifically for this item
        Long existingRoomId = chatDao.selectPrivateRoomId(params);

        if (existingRoomId != null) {

            return existingRoomId;
        }

        // 2. Create new room if not exists
        java.util.Map<String, Object> createParams = new java.util.HashMap<>();
        createParams.put("id", null); // Will be set by selectKey
        createParams.put("title", "1:1 Chat");
        createParams.put("createdBy", currentUserId);
        createParams.put("itemId", itemId); // [NEW]

        chatDao.createPrivateRoom(createParams);
        Long newRoomId = (Long) createParams.get("id"); // Make sur e mapper sets this

        // 3. Add members
        java.util.Map<String, Object> member1 = new java.util.HashMap<>();
        member1.put("roomId", newRoomId);
        member1.put("userId", currentUserId);
        chatDao.insertRoomMember(member1);

        java.util.Map<String, Object> member2 = new java.util.HashMap<>();
        member2.put("roomId", newRoomId);
        member2.put("userId", targetUserId);
        chatDao.insertRoomMember(member2);

        return newRoomId;
    }

    @PostMapping("/api/room/leave")
    @ResponseBody
    public String leaveChatRoom(@RequestParam Long roomId, java.security.Principal principal) {
        Long currentUserId = getUserIdFromPrincipal(principal);
        if (currentUserId == null) {
            return "PC: 로그인이 필요합니다.";
        }

        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("roomId", roomId);
        params.put("userId", currentUserId);
        chatDao.updateLeaveRoom(params);

        return "success";
    }

    @GetMapping("/api/message/search")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> searchChatMessages(
            @RequestParam String keyword,
            @RequestParam(required = false) Long roomId,
            java.security.Principal principal) {
        Long currentUserId = getUserIdFromPrincipal(principal);
        if (currentUserId == null || keyword == null || keyword.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }

        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("userId", currentUserId);
        params.put("keyword", keyword);
        if (roomId != null) {
            params.put("roomId", roomId);
        }
        return chatDao.selectSearchMessages(params);
    }

    @GetMapping("/api/unread-count")
    @ResponseBody
    public int getTotalUnreadCount(java.security.Principal principal) {
        Long currentUserId = getUserIdFromPrincipal(principal);
        if (currentUserId == null)
            return 0;
        return chatDao.selectTotalUnreadCount(currentUserId);
    }

    private Long getUserIdFromPrincipal(java.security.Principal principal) {
        if (principal == null)
            return null;
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken token = (org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal;
            if (token.getPrincipal() instanceof com.rebirth.my.auth.CustomUserDetails) {
                return ((com.rebirth.my.auth.CustomUserDetails) token.getPrincipal()).getId();
            }
        } else if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) {
            org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken token = (org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) principal;
            if (token.getPrincipal() instanceof com.rebirth.my.auth.CustomOAuth2User) {
                return ((com.rebirth.my.auth.CustomOAuth2User) token.getPrincipal()).getId();
            }
        }
        // Fallback or Anonymous
        return null;
    }

    @PostMapping("/api/chat/upload")
    @ResponseBody
    public java.util.Map<String, Object> uploadChatImage(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            java.security.Principal principal) {

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        if (getUserIdFromPrincipal(principal) == null) {
            result.put("error", "Unauthorized");
            return result;
        }

        if (file.isEmpty()) {
            result.put("error", "Empty file");
            return result;
        }

        try {
            // 저장 경로: 프로젝트 루트/uploads/chat/ (UploadConfig와 일치)
            String projectPath = System.getProperty("user.dir");
            String uploadDir = projectPath + "/uploads/chat/";
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String savedFilename = java.util.UUID.randomUUID().toString() + extension;

            java.io.File dest = new java.io.File(uploadDir + savedFilename);
            file.transferTo(dest);

            String fileUrl = "/uploads/chat/" + savedFilename;
            result.put("url", fileUrl);
            result.put("success", true);

        } catch (java.io.IOException e) {
            e.printStackTrace();
            result.put("error", "Upload failed");
        }

        return result;
    }
}
