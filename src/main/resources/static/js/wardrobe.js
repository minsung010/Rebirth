document.addEventListener('DOMContentLoaded', function() {
    const startButton = document.getElementById('start-reverse');
    const wardrobeOverlay = document.getElementById('wardrobe-overlay');
    const enterButton = document.getElementById('enter-wardrobe');
    const header = document.getElementById('main-header');
    const menuReveal = document.getElementById('menu-reveal');
    const continueButton = document.getElementById('continue-to-menu');

    // Create particles
    function createParticles() {
        const particlesContainer = document.createElement('div');
        particlesContainer.className = 'wardrobe-particles';
        
        for (let i = 0; i < 50; i++) {
            const particle = document.createElement('div');
            particle.className = 'particle';
            particle.style.left = Math.random() * 100 + '%';
            particle.style.animationDelay = Math.random() * 3 + 's';
            particle.style.animationDuration = (3 + Math.random() * 2) + 's';
            particlesContainer.appendChild(particle);
        }
        
        return particlesContainer;
    }

    if (startButton && wardrobeOverlay) {
        // Add particles to wardrobe overlay
        const particles = createParticles();
        wardrobeOverlay.appendChild(particles);

        startButton.addEventListener('click', function(e) {
            e.preventDefault();
            
            // Show wardrobe overlay
            wardrobeOverlay.classList.add('active');
            
            // Hide the final section during animation
            const finalSection = document.getElementById('final-section');
            if (finalSection) {
                finalSection.style.opacity = '0';
            }
        });

        if (enterButton) {
            enterButton.addEventListener('click', function() {
                // Close wardrobe doors
                wardrobeOverlay.classList.remove('active');
                
                // After doors close, show menu reveal
                setTimeout(() => {
                    if (menuReveal) {
                        menuReveal.classList.add('active');
                        
                        // Show header with menu
                        if (header) {
                            header.classList.add('visible');
                            header.style.transform = 'translateY(0)';
                        }
                    }
                }, 2000);
            });
        }

        if (continueButton) {
            continueButton.addEventListener('click', function() {
                // Hide menu reveal and show the main page
                if (menuReveal) {
                    menuReveal.classList.remove('active');
                }
                
                // Navigate to home or show main content
                setTimeout(() => {
                    // You can add navigation logic here
                    // For example: window.location.href = '/home';
                    // Or show the main content directly
                    console.log('Transitioning to main menu...');
                }, 500);
            });
        }
    }
});
