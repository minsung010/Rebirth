/**
 * Mypage Javascript
 * - 탭 전환 로직
 * - 프로필 이미지 업로드
 * - 에코 미션 토글 및 서버 연동 (UI 스타일 완벽 동기화)
 */

document.addEventListener('DOMContentLoaded', () => {

    /* ==========================================
       1. 탭 전환 (Dashboard, Activity, etc.)
       ========================================== */
    const tabs = Array.from(document.querySelectorAll('#nav-dashboard, #nav-activity, #nav-wishlist, #nav-settings'));
    const contents = {
        'nav-dashboard': document.getElementById('content-dashboard'),
        'nav-activity': document.getElementById('content-activity'),
        'nav-wishlist': document.getElementById('content-wishlist'),
        'nav-settings': document.getElementById('content-settings')
    };

    const switchTab = (activeNavId) => {
        // 모든 네비 버튼 비활성화
        tabs.forEach(tab => {
            tab.classList.remove('font-bold', 'active', 'bg-gray-50', 'dark:bg-background-dark');
        });

        // 클릭된 버튼 활성화
        const activeTab = document.getElementById(activeNavId);
        if (activeTab) {
            activeTab.classList.add('font-bold', 'active');
        }

        // 모든 컨텐츠 숨김
        Object.values(contents).forEach(content => {
            if (content) content.classList.add('hidden');
        });

        // 선택된 컨텐츠 표시
        if (contents[activeNavId]) {
            contents[activeNavId].classList.remove('hidden');
        }
    };

    // 탭 버튼 클릭 이벤트 바인딩
    tabs.forEach(tab => {
        tab.addEventListener('click', () => switchTab(tab.id));
    });

    // 글로벌 함수 등록 (HTML에서의 onclick 대응용)
    window.switchTab = (tabName) => switchTab('nav-' + tabName);


    /* ==========================================
       2. 프로필 이미지 업로드
       ========================================== */
    const profileContainer = document.getElementById('profileImageContainer');
    const fileInput = document.getElementById('fileInput');
    const form = document.getElementById('profileUploadForm');

    if (profileContainer && fileInput && form) {
        profileContainer.addEventListener('click', () => fileInput.click());
        fileInput.addEventListener('change', () => form.submit());
    }


    /* ==========================================
       3. 에코 미션 토글 (UI + 서버 연동)
       ========================================== */
    document.querySelectorAll('label[data-task-id]').forEach(label => {
        label.addEventListener('click', async (e) => {
            e.preventDefault(); // label 클릭 시 중복 발생 방지

            const taskId = label.dataset.taskId;
            const isCurrentlyCompleted = label.dataset.completed === 'true';
            const newState = !isCurrentlyCompleted;

            // 1. UI 즉시 업데이트
            updateMissionUI(label, newState);
            label.dataset.completed = newState;

            // 2. 서버 연동
            try {
                const response = await fetch('/eco-mission/check', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        taskId: taskId,
                        checked: newState
                    })
                });

                if (!response.ok) {
                    throw new Error('Server response was not ok');
                }
            } catch (err) {
                console.error('미션 업데이트 실패:', err);
                // 실패 시 UI 복구 (선택 사항)
                // updateMissionUI(label, isCurrentlyCompleted);
                // label.dataset.completed = isCurrentlyCompleted;
            }
        });
    });
});

/**
 * 미션 스타일을 HTML(Thymeleaf)과 완벽하게 일치시키는 함수
 */
function updateMissionUI(label, isChecked) {
    const checkbox = label.querySelector('.mission-checkbox');
    const text = label.querySelector('.mission-text');

    if (isChecked) {
        // 체크 상태: 보라색 배경 + 체크 아이콘 + 취소선 + 흐린 글씨
        checkbox.className = "mission-checkbox size-6 rounded-full border-2 border-primary bg-primary flex items-center justify-center transition-colors flex-shrink-0";
        checkbox.innerHTML = '<span class="material-symbols-outlined text-white text-sm">check</span>';

        text.className = "mission-text text-sm font-medium flex-1 text-gray-400 dark:text-text-dark/40 line-through";
    } else {
        // 미체크 상태: 비어있는 원 + 검정/흰색 글씨 (복구 시 Thymeleaf 클래스와 정확히 일치)
        checkbox.className = "mission-checkbox size-6 rounded-full border-2 border-[#e4e5eb] dark:border-border-dark group-hover:border-primary flex items-center justify-center transition-colors flex-shrink-0";
        checkbox.innerHTML = '<div class="size-3 rounded-full bg-primary opacity-0 group-hover:opacity-100 transition-opacity"></div>';

        // 텍스트 색상을 라이트/다크모드 기본값으로 복구
        text.className = "mission-text text-sm font-medium flex-1 text-[#1e1e23] dark:text-text-dark";
    }
}
