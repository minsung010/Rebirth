document.addEventListener('DOMContentLoaded', () => {
	const snapContainer = document.getElementById('snap-container');
	const header = document.getElementById('main-header');
	const sceneBackground = document.getElementById('scene-background');

	// Section colors for background transitions
	const sceneThemes = {
		intro: 'linear-gradient(135deg, rgba(139, 92, 246, 0.35) 0%, rgba(196, 181, 253, 0.2) 100%)',
		story: 'linear-gradient(135deg, rgba(191, 219, 254, 0.45) 0%, rgba(129, 140, 248, 0.25) 100%)',
		metrics: 'linear-gradient(135deg, rgba(74, 222, 128, 0.35) 0%, rgba(16, 185, 129, 0.25) 100%)',
		cta: 'linear-gradient(135deg, rgba(248, 113, 113, 0.35) 0%, rgba(251, 191, 36, 0.25) 100%)'
	};

	// Observer for Active Section (Background & Header)
	const sectionObserver = new IntersectionObserver((entries) => {
		entries.forEach(entry => {
			if (entry.isIntersecting) {
				const section = entry.target;
				const sceneKey = section.dataset.scene;

				// 1. Update Background
				if (sceneBackground && sceneThemes[sceneKey]) {
					sceneBackground.style.background = sceneThemes[sceneKey];
				}

				// 2. Trigger Animations (Specifically for Scene Sections)
				const animatable = section.querySelectorAll('.animate-fade-in-up');
				animatable.forEach(el => el.classList.add('visible'));

				// 3. Header Visibility (Hide on Intro)
				if (header) {
					if (sceneKey === 'intro') {
						header.classList.remove('visible');
					} else {
						header.classList.add('visible');
					}
				}
			}
		});
	}, { threshold: 0.5 }); // Trigger when 50% visible

	document.querySelectorAll('section[data-scene]').forEach(sec => {
		sectionObserver.observe(sec);
	});

	// GLOBAL Animation Observer (For Main.html and non-scene pages)
	// targeting elements that are NOT inside a [data-scene] section
	// or just observe ALL .animate-fade-in-up elements generally as a fallback
	const globalRevealObserver = new IntersectionObserver((entries) => {
		entries.forEach(entry => {
			if (entry.isIntersecting) {
				entry.target.classList.add('visible');
				// Optional: Unobserve after revealing to save performance?
				globalRevealObserver.unobserve(entry.target);
			}
		});
	}, { threshold: 0.1 });

	document.querySelectorAll('.animate-fade-in-up').forEach(el => {
		globalRevealObserver.observe(el);
	});

	// Failsafe: Ensure elements are visible if Observer fails or is slow
	setTimeout(() => {
		document.querySelectorAll('.animate-fade-in-up:not(.visible)').forEach(el => {
			el.classList.add('visible');
			el.style.opacity = '1';
			el.style.transform = 'translateY(0)';
		});
	}, 500);


	// Header Scroll Effect (for standard pages or internal scrolling)
	if (header) {
		window.addEventListener('scroll', () => {
			if (window.scrollY > 10) header.classList.add('header-scrolled');
			else header.classList.remove('header-scrolled');
		});
	}

	// Interactive Counters (Impact Section)
	const counters = document.querySelectorAll('.counter');
	if (counters.length > 0) {
		const counterObserver = new IntersectionObserver((entries, observer) => {
			entries.forEach(entry => {
				if (entry.isIntersecting) {
					const counter = entry.target;
					const target = +counter.getAttribute('data-target');
					const duration = 2000;
					const start = 0;
					const startTime = performance.now();

					const updateCounter = (currentTime) => {
						const elapsed = currentTime - startTime;
						const progress = Math.min(elapsed / duration, 1);
						const ease = 1 - Math.pow(1 - progress, 4);
						const current = Math.floor(start + (target - start) * ease);
						counter.innerText = current.toLocaleString();

						if (progress < 1) requestAnimationFrame(updateCounter);
						else counter.innerText = target.toLocaleString();
					};
					requestAnimationFrame(updateCounter);
					observer.unobserve(counter);
				}
			});
		}, { threshold: 0.5 });
		counters.forEach(counter => counterObserver.observe(counter));
	}

	// Scroll Down Buttons Logic
	document.querySelectorAll('[data-scene-trigger="next"]').forEach(btn => {
		btn.addEventListener('click', () => {
			if (snapContainer) {
				// For Scroll Snap Container
				snapContainer.scrollBy({ top: window.innerHeight, behavior: 'smooth' });
			} else {
				// Fallback
				const currentSection = btn.closest('section');
				const nextSection = currentSection.nextElementSibling;
				if (nextSection) nextSection.scrollIntoView({ behavior: 'smooth' });
			}
		});
	});

	// Nav Menu Interactions
	const navGroups = document.querySelectorAll('[data-mega-menu="true"]');
	navGroups.forEach((group) => {
		const megaMenu = group.querySelector('.mega-menu-content');
		if (!megaMenu) return;

		let closeTimeout = null;
		const openMenu = () => {
			if (closeTimeout) clearTimeout(closeTimeout);
			group.classList.add('mega-menu-open');
		};
		const closeMenu = () => {
			closeTimeout = setTimeout(() => {
				group.classList.remove('mega-menu-open');
			}, 100);
		};
		group.addEventListener('mouseenter', openMenu);
		group.addEventListener('mouseleave', closeMenu);
	});
});