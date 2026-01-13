// Service Worker for Push Notifications
self.addEventListener('install', function(event) {
    console.log('[SW] Service Worker 설치됨');
    self.skipWaiting();
});

self.addEventListener('activate', function(event) {
    console.log('[SW] Service Worker 활성화됨');
    event.waitUntil(clients.claim());
});

// 알림 클릭 이벤트 처리
self.addEventListener('notificationclick', function(event) {
    console.log('[SW] 알림 클릭됨:', event.notification.data);
    event.notification.close();
    
    // 알림에 저장된 URL로 이동
    if (event.notification.data && event.notification.data.url) {
        event.waitUntil(
            clients.openWindow(event.notification.data.url)
        );
    }
});
