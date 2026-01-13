document.addEventListener("DOMContentLoaded", () => {
	    const darkToggle = document.getElementById("toggle-dark");

	    // 1. 페이지 로드 시 저장된 다크모드 상태 적용
	    const isDark = localStorage.getItem("theme") === "dark";

	    if (isDark) {
	        document.documentElement.classList.add("dark");
	    } else {
	        document.documentElement.classList.remove("dark");
	    }

	    // 토글이 있는 페이지라면 체크 상태도 맞춤
	    if (darkToggle) {
	        darkToggle.checked = isDark;

	        // 2. 토글 변경 시 상태 저장
	        darkToggle.addEventListener("change", () => {
	            if (darkToggle.checked) {
	                document.documentElement.classList.add("dark");
	                localStorage.setItem("theme", "dark");
	            } else {
	                document.documentElement.classList.remove("dark");
	                localStorage.setItem("theme", "light");
	            }
	        });
	    }
	});