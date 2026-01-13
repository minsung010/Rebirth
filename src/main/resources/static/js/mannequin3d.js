import * as THREE from 'three';
window.THREE = THREE;

/**
 * 3D 가상 피팅룸 - 하이브리드 시스템
 * 기본적으로 앱스트랙트 마네킹을 보여주고, GLTF 파일이 있으면 리얼 마네킹으로 교체
 */
export class Mannequin3D {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        if (!this.container) return;

        this.scene = null;
        this.camera = null;
        this.renderer = null;
        this.mannequin = null;

        // 의상 패널 등을 저장할 객체
        this.clothingMeshes = {
            top: null,
            bottom: null,
            shoes: null,
            dress: null
        };

        this.textureLoader = new THREE.TextureLoader();
        this.isDragging = false;
        this.previousMouseX = 0;

        // 자동 회전 설정
        this.rotationSpeed = 0.0005;
        this.maxRotationAngle = 0.1; // 기본 각도

        this.init();
    }

    setRotationAngle(angle) {
        this.maxRotationAngle = angle;
    }

    init() {
        // 1. 씬 (Scene)
        this.scene = new THREE.Scene();
        this.scene.background = null; // 배경 투명하게 (HTML 배경 사용)

        // 2. 카메라 (Camera)
        const width = this.container.clientWidth;
        const height = this.container.clientHeight;
        this.camera = new THREE.PerspectiveCamera(35, width / height, 0.1, 1000);
        this.camera.position.set(0, 0.9, 3.5); // 카메라 위치 미세 조정
        this.camera.lookAt(0, 0.9, 0);

        // 3. 렌더러 (Renderer)
        this.renderer = new THREE.WebGLRenderer({
            antialias: true,
            alpha: true, // 투명 배경 허용
            preserveDrawingBuffer: true
        });
        this.renderer.setSize(width, height);
        this.renderer.setPixelRatio(window.devicePixelRatio);
        this.renderer.shadowMap.enabled = true;
        this.renderer.shadowMap.type = THREE.PCFSoftShadowMap;

        this.container.innerHTML = '';
        this.container.appendChild(this.renderer.domElement);

        this.createLights();
        this.createFloor();

        // GLTF 마네킹만 사용 (코드 마네킹 사용 안 함)
        this.loadGLTFMannequin();

        // 5. 이벤트 리스너
        this.addEventListeners();
        this.animate();

        // 전역 인스턴스 등록
        window.mannequinInstance = this;
    }

    // 기본: 스타일리시 캡슐 마네킹 (부위별 태그 포함)
    createBody() {
        this.mannequin = new THREE.Group();

        const skinColor = 0xFFE0BD;
        const bodyMaterial = new THREE.MeshStandardMaterial({
            color: skinColor,
            roughness: 0.45,
            metalness: 0.05
        });

        // 머리 (계란형) - 옷 적용 안 함
        const head = new THREE.Mesh(new THREE.SphereGeometry(1, 32, 32), bodyMaterial.clone());
        head.scale.set(0.12, 0.16, 0.13);
        head.position.y = 1.75;
        head.castShadow = true;
        head.userData.bodyPart = 'head'; // 태그
        this.mannequin.add(head);

        // 목 - 옷 적용 안 함
        const neck = new THREE.Mesh(new THREE.CylinderGeometry(0.05, 0.06, 0.1, 16), bodyMaterial.clone());
        neck.position.y = 1.6;
        neck.userData.bodyPart = 'neck';
        this.mannequin.add(neck);

        // 가슴 - 상의 적용
        const chest = new THREE.Mesh(new THREE.CylinderGeometry(0.18, 0.14, 0.35, 32), bodyMaterial.clone());
        chest.scale.set(1.4, 1, 0.7);
        chest.position.y = 1.4;
        chest.castShadow = true;
        chest.userData.bodyPart = 'chest'; // 상의
        this.mannequin.add(chest);

        // 허리 - 상의 적용
        const waist = new THREE.Mesh(new THREE.CylinderGeometry(0.13, 0.145, 0.25, 32), bodyMaterial.clone());
        waist.scale.set(1.1, 1, 0.65);
        waist.position.y = 1.1;
        waist.castShadow = true;
        waist.userData.bodyPart = 'waist'; // 상의
        this.mannequin.add(waist);

        // 골반 - 하의 적용
        const pelvis = new THREE.Mesh(new THREE.CylinderGeometry(0.145, 0.16, 0.25, 32), bodyMaterial.clone());
        pelvis.scale.set(1.3, 1, 0.75);
        pelvis.position.y = 0.88;
        pelvis.castShadow = true;
        pelvis.userData.bodyPart = 'pelvis'; // 하의
        this.mannequin.add(pelvis);

        // 어깨 - 상의 적용
        const shoulderGeo = new THREE.SphereGeometry(0.09, 32, 32);
        const lShoulder = new THREE.Mesh(shoulderGeo, bodyMaterial.clone());
        lShoulder.position.set(-0.25, 1.48, 0);
        lShoulder.scale.set(1, 0.8, 0.8);
        lShoulder.userData.bodyPart = 'shoulder'; // 상의
        this.mannequin.add(lShoulder);

        const rShoulder = new THREE.Mesh(shoulderGeo, bodyMaterial.clone());
        rShoulder.position.set(0.25, 1.48, 0);
        rShoulder.scale.set(1, 0.8, 0.8);
        rShoulder.userData.bodyPart = 'shoulder'; // 상의
        this.mannequin.add(rShoulder);

        // 팔 - 상의 적용
        const armGeo = new THREE.CylinderGeometry(0.06, 0.045, 0.7, 16);
        armGeo.translate(0, -0.35, 0);

        const leftArm = new THREE.Mesh(armGeo, bodyMaterial.clone());
        leftArm.scale.set(1, 1, 0.85);
        leftArm.position.set(-0.28, 1.45, 0);
        leftArm.rotation.z = Math.PI / 16;
        leftArm.castShadow = true;
        leftArm.userData.bodyPart = 'arm'; // 상의
        this.mannequin.add(leftArm);

        const rightArm = new THREE.Mesh(armGeo, bodyMaterial.clone());
        rightArm.scale.set(1, 1, 0.85);
        rightArm.position.set(0.28, 1.45, 0);
        rightArm.rotation.z = -Math.PI / 16;
        rightArm.castShadow = true;
        rightArm.userData.bodyPart = 'arm'; // 상의
        this.mannequin.add(rightArm);

        // 다리 - 하의 적용
        const legGeo = new THREE.CylinderGeometry(0.08, 0.05, 0.95, 16);
        legGeo.translate(0, -0.475, 0);

        const leftLeg = new THREE.Mesh(legGeo, bodyMaterial.clone());
        leftLeg.scale.set(1, 1, 0.9);
        leftLeg.position.set(-0.11, 0.8, 0);
        leftLeg.castShadow = true;
        leftLeg.userData.bodyPart = 'leg'; // 하의
        this.mannequin.add(leftLeg);

        const rightLeg = new THREE.Mesh(legGeo, bodyMaterial.clone());
        rightLeg.scale.set(1, 1, 0.9);
        rightLeg.position.set(0.11, 0.8, 0);
        rightLeg.castShadow = true;
        rightLeg.userData.bodyPart = 'leg'; // 하의
        this.mannequin.add(rightLeg);

        this.scene.add(this.mannequin);
    }

    // GLTF 마네킹만 로드 (코드 마네킹 없음)
    async loadGLTFMannequin() {
        try {
            const { GLTFLoader } = await import('three/addons/loaders/GLTFLoader.js');
            const loader = new GLTFLoader();
            const modelPath = '/models/mannequin/scene.gltf';

            console.log("GLTF 마네킹 로딩 중...", modelPath);

            loader.load(modelPath, (gltf) => {
                console.log("✅ GLTF 마네킹 로드 성공!");

                this.mannequin = gltf.scene;
                this.mannequin.scale.set(1.0, 1.0, 1.0);
                this.mannequin.position.set(0, 0, 0);

                // 마네킹 색상
                const mannequinColor = 0xFFE0BD;

                // ===== [메쉬 분석] 메쉬 이름으로 상체/하체 구분 =====
                console.log("📊 메쉬 분석 시작...");
                let meshIndex = 0;

                this.mannequin.traverse((node) => {
                    if (node.isMesh) {
                        const meshName = (node.name || '').toLowerCase();

                        // 메쉬 이름으로 상체/하체 구분
                        if (meshName.includes('upper') || meshName.includes('torso') || meshName.includes('chest') || meshName.includes('arm')) {
                            node.userData.bodyRegion = 'upper';
                        } else if (meshName.includes('lower') || meshName.includes('leg') || meshName.includes('pelvis')) {
                            node.userData.bodyRegion = 'lower';
                        } else {
                            // 이름으로 구분 안 되면 기본값
                            node.userData.bodyRegion = 'upper';
                        }

                        console.log(`  [${meshIndex}] 메쉬: "${node.name || '(이름없음)'}" → ${node.userData.bodyRegion}`);
                        meshIndex++;

                        node.castShadow = true;
                        node.receiveShadow = true;
                        if (node.material) {
                            node.material.color = new THREE.Color(mannequinColor);
                            node.material.needsUpdate = true;
                        }
                    }
                });

                console.log(`📊 메쉬 분석 완료! 총 ${meshIndex}개 메쉬`);
                this.scene.add(this.mannequin);
            },
                (progress) => {
                    if (progress.total > 0) {
                        console.log(`로딩 진행: ${Math.round(progress.loaded / progress.total * 100)}%`);
                    }
                },
                (err) => {
                    console.error("❌ GLTF 마네킹 로드 실패:", err);
                });

        } catch (e) {
            console.error("GLTFLoader 가져오기 실패:", e);
        }
    }

    // 마네킹 색상 동적 변경 메서드
    setMannequinColor(hexColor) {
        if (!this.mannequin) return;
        this.mannequin.traverse((node) => {
            if (node.isMesh && node.material) {
                node.material.color = new THREE.Color(hexColor);
                node.material.needsUpdate = true;
            }
        });
    }

    createLights() {
        const ambient = new THREE.AmbientLight(0xffffff, 0.4);
        this.scene.add(ambient);

        // 정면 하이라이트 조명
        const mainLight = new THREE.DirectionalLight(0xffffff, 0.6);
        mainLight.position.set(1, 2, 3);
        mainLight.castShadow = true;
        mainLight.shadow.mapSize.width = 1024;
        mainLight.shadow.mapSize.height = 1024;
        this.scene.add(mainLight);

        // === 스튜디오 스포트라이트 (위에서 내려오는 조명) ===

        // 중앙 스포트라이트 (메인)
        const centerSpot = new THREE.SpotLight(0xffffff, 1.2);
        centerSpot.position.set(0, 4, 1);
        centerSpot.angle = Math.PI / 6;
        centerSpot.penumbra = 0.5;
        centerSpot.decay = 1.5;
        centerSpot.distance = 10;
        centerSpot.castShadow = true;
        centerSpot.target.position.set(0, 0.5, 0);
        this.scene.add(centerSpot);
        this.scene.add(centerSpot.target);

        // 왼쪽 스포트라이트
        const leftSpot = new THREE.SpotLight(0xfff5e6, 0.8);
        leftSpot.position.set(-2, 3.5, 0.5);
        leftSpot.angle = Math.PI / 7;
        leftSpot.penumbra = 0.6;
        leftSpot.decay = 1.5;
        leftSpot.distance = 8;
        leftSpot.target.position.set(0, 0.5, 0);
        this.scene.add(leftSpot);
        this.scene.add(leftSpot.target);

        // 오른쪽 스포트라이트
        const rightSpot = new THREE.SpotLight(0xfff5e6, 0.8);
        rightSpot.position.set(2, 3.5, 0.5);
        rightSpot.angle = Math.PI / 7;
        rightSpot.penumbra = 0.6;
        rightSpot.decay = 1.5;
        rightSpot.distance = 8;
        rightSpot.target.position.set(0, 0.5, 0);
        this.scene.add(rightSpot);
        this.scene.add(rightSpot.target);

        // 후면 림 라이트 (입체감 강화)
        const rimLight = new THREE.SpotLight(0xaaccff, 0.5);
        rimLight.position.set(0, 2.5, -2);
        rimLight.angle = Math.PI / 4;
        rimLight.penumbra = 0.8;
        rimLight.target.position.set(0, 1, 0);
        this.scene.add(rimLight);
        this.scene.add(rimLight.target);
    }

    createFloor() {
        // 스포트라이트 효과 - 밝은 노란색 원
        const geo = new THREE.CircleGeometry(0.55, 64);
        const mat = new THREE.MeshBasicMaterial({
            color: 0xFFE066, // 밝은 노란색 (조명 느낌)
            transparent: true,
            opacity: 0.85
        });
        const floor = new THREE.Mesh(geo, mat);
        floor.rotation.x = -Math.PI / 2;
        floor.position.y = -0.01;
        floor.receiveShadow = true;
        this.scene.add(floor);
    }

    animate() {
        requestAnimationFrame(() => this.animate());
        if (!this.isDragging && this.mannequin) {
            // 자동 회전도 아주 살짝만
            this.mannequin.rotation.y = Math.sin(Date.now() * 0.0005) * 0.05;
        }
        this.renderer.render(this.scene, this.camera);
    }

    addEventListeners() {
        this.container.addEventListener('mousedown', (e) => {
            this.isDragging = true;
            this.previousMouseX = e.clientX;
        });
        window.addEventListener('mouseup', () => this.isDragging = false);
        window.addEventListener('mousemove', (e) => {
            if (this.isDragging && this.mannequin) {
                const delta = (e.clientX - this.previousMouseX) * 0.005; // 감도 조절
                let newAngle = this.mannequin.rotation.y + delta;

                // 회전 각도 제한 (-0.3 ~ 0.3 라디안, 약 17도)
                newAngle = Math.max(-0.3, Math.min(0.3, newAngle));

                this.mannequin.rotation.y = newAngle;
                this.previousMouseX = e.clientX;
            }
        });
    }

    /**
     * [핵심 기능] 2D 옷 이미지를 마네킹에 입히기 (하이브리드 바디페인팅)
     * 정면: 옷 텍스처, 측면/후면: 주요 색상
     * @param {string} category - 'top' | 'bottom' | 'dress' | 'shoes' 또는 한글
     * @param {string} imageUrl - 이미지 경로
     */
    wearImage(category, imageUrl) {
        if (!imageUrl) return;

        // 한글 카테고리 → 영문 변환
        const categoryMap = {
            '아우터': 'outer', '상의': 'top', '하의': 'bottom',
            '바지': 'bottom', '치마': 'bottom', '원피스': 'dress',
            '신발': 'shoes', 'Outer': 'outer', 'Top': 'top', 'Bottom': 'bottom'
        };
        const normalizedCategory = categoryMap[category] || category.toLowerCase();

        console.log(`[피팅] 원본: ${category} → 변환: ${normalizedCategory}, URL: ${imageUrl}`);

        // 백엔드 프록시를 통해 CORS 해결
        const proxyUrl = `/api/proxy/image?url=${encodeURIComponent(imageUrl)}`;

        const img = new Image();
        img.crossOrigin = "Anonymous";
        img.src = proxyUrl;

        img.onload = () => {
            console.log("✅ 이미지 로딩 성공:", img.width, "x", img.height);

            const texture = new THREE.Texture(img);
            texture.colorSpace = THREE.SRGBColorSpace;
            texture.minFilter = THREE.LinearFilter;
            texture.magFilter = THREE.LinearFilter;
            texture.needsUpdate = true;

            // 이미지 비율 계산
            let aspectRatio = img.width / img.height;
            if (isNaN(aspectRatio)) aspectRatio = 1.0;
            aspectRatio = Math.max(0.5, Math.min(2.0, aspectRatio));

            // [커브드 패널] 옷 이미지를 마네킹 앞에 표시
            this.createClothingPanel(normalizedCategory, texture, aspectRatio);
        };

        img.onerror = (err) => {
            console.error('프록시 이미지 로드 실패:', err);
            if (img.src.includes('/api/proxy/image')) {
                console.log('프록시 실패.. 원본으로 재시도');
                img.src = imageUrl;
            } else {
                console.error('이미지 로드 실패:', err);
            }
        };
    }

    /**
     * 이미지에서 주요(dominant) 색상 추출
     */
    extractDominantColor(img) {
        try {
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d');
            const size = 50; // 샘플링 크기
            canvas.width = size;
            canvas.height = size;
            ctx.drawImage(img, 0, 0, size, size);

            const imageData = ctx.getImageData(0, 0, size, size);
            const data = imageData.data;

            let r = 0, g = 0, b = 0, count = 0;

            // 이미지 중앙 부분에서 색상 샘플링
            for (let i = 0; i < data.length; i += 4) {
                const alpha = data[i + 3];
                if (alpha > 128) { // 투명하지 않은 픽셀만
                    r += data[i];
                    g += data[i + 1];
                    b += data[i + 2];
                    count++;
                }
            }

            if (count > 0) {
                r = Math.round(r / count);
                g = Math.round(g / count);
                b = Math.round(b / count);
            }

            return new THREE.Color(r / 255, g / 255, b / 255);
        } catch (e) {
            console.warn("색상 추출 실패:", e);
            return new THREE.Color(0.5, 0.5, 0.5); // 기본 회색
        }
    }

    /**
     * [색상 바디페인팅] 옷의 주요 색상만 마네킹에 적용 (텍스처 없이)
     */
    applyColorToBody(category, dominantColor) {
        if (!this.mannequin) {
            console.error("마네킹이 없습니다!");
            return;
        }

        // 카테고리별 적용할 영역 결정
        const topCategories = ['top', 'outer', 'upper'];
        const bottomCategories = ['bottom', 'pants', 'skirt', 'lower'];
        const dressCategories = ['dress', 'onepiece'];

        let targetRegion;
        if (topCategories.includes(category)) targetRegion = 'upper';
        else if (bottomCategories.includes(category)) targetRegion = 'lower';
        else if (dressCategories.includes(category)) targetRegion = 'all';
        else targetRegion = 'upper';

        console.log(`[색상 바디페인팅] ${category} 적용 시작... (대상: ${targetRegion})`);
        let appliedCount = 0;

        this.mannequin.traverse((node) => {
            if (node.isMesh && node.material) {
                const originalMeshName = node.name || '';

                // 메쉬 이름으로 적용 여부 결정
                let shouldApply = false;
                if (targetRegion === 'all') {
                    shouldApply = originalMeshName.includes('UpperBody') ||
                        originalMeshName.includes('LowerBody');
                } else if (targetRegion === 'upper') {
                    shouldApply = originalMeshName.includes('UpperBody');
                } else if (targetRegion === 'lower') {
                    shouldApply = originalMeshName.includes('LowerBody');
                }

                if (shouldApply) {
                    console.log(`  ✓ 색상 적용: ${originalMeshName}`);

                    // 원본 재질 저장
                    if (!node.userData.originalMaterial) {
                        node.userData.originalMaterial = node.material.clone();
                    }

                    // 단색 재질 적용 (텍스처 없이 색상만)
                    const colorMaterial = new THREE.MeshStandardMaterial({
                        color: dominantColor,
                        roughness: 0.6,
                        metalness: 0.0,
                        side: THREE.DoubleSide
                    });

                    node.userData.clothingCategory = category;
                    node.material = colorMaterial;
                    appliedCount++;
                }
            }
        });

        console.log(`[색상 바디페인팅] ${category} 적용 완료! (${appliedCount}개 메쉬에 적용)`);
    }

    /**
     * [하이브리드 바디페인팅] 정면은 텍스처, 측면/후면은 단색
     */
    applyHybridBodyPainting(category, texture, dominantColor) {
        if (!this.mannequin) {
            console.error("마네킹이 없습니다!");
            return;
        }

        // 카테고리별 적용할 영역 결정
        const topCategories = ['top', 'outer', 'upper'];
        const bottomCategories = ['bottom', 'pants', 'skirt', 'lower'];
        const dressCategories = ['dress', 'onepiece'];

        let targetRegion;
        if (topCategories.includes(category)) targetRegion = 'upper';
        else if (bottomCategories.includes(category)) targetRegion = 'lower';
        else if (dressCategories.includes(category)) targetRegion = 'all';
        else targetRegion = 'upper';

        console.log(`[하이브리드 바디페인팅] ${category} 적용 시작... (대상: ${targetRegion})`);
        let appliedCount = 0;

        this.mannequin.traverse((node) => {
            if (node.isMesh && node.material) {
                const region = node.userData.bodyRegion || 'unknown';
                const meshName = (node.name || '').toLowerCase();

                // 메쉬 이름으로 적용 여부 결정
                let shouldApply = false;
                const originalMeshName = node.name || '';

                if (targetRegion === 'all') {
                    // 원피스: UpperBody + LowerBody 모두
                    shouldApply = originalMeshName.includes('UpperBody') ||
                        originalMeshName.includes('LowerBody');
                } else if (targetRegion === 'upper') {
                    // 상체: UpperBody만
                    shouldApply = originalMeshName.includes('UpperBody');
                } else if (targetRegion === 'lower') {
                    // 하체: LowerBody만
                    shouldApply = originalMeshName.includes('LowerBody');
                }

                console.log(`  체크: ${originalMeshName} → ${shouldApply ? '적용' : '건너뜀'}`);

                if (shouldApply) {
                    console.log(`  ✓ ${region} 메쉬에 옷 적용: ${node.name || '(이름없음)'}`);

                    // 원본 재질 저장
                    if (!node.userData.originalMaterial) {
                        node.userData.originalMaterial = node.material.clone();
                    }

                    // 텍스처 원본 색상 유지 (color: white로 곱셈 효과 제거)
                    const hybridMaterial = new THREE.MeshStandardMaterial({
                        map: texture,
                        color: 0xffffff,  // 흰색 = 텍스처 색상 그대로 표시
                        roughness: 0.5,
                        metalness: 0.0,
                        side: THREE.DoubleSide
                    });

                    node.userData.clothingCategory = category;
                    node.material = hybridMaterial;
                    appliedCount++;
                }
            }
        });

        console.log(`[하이브리드 바디페인팅] ${category} 적용 완료! (${appliedCount}개 메쉬에 적용)`);

        if (appliedCount === 0) {
            console.warn("적용된 메쉬가 없습니다. 메쉬 이름에 'upper' 또는 'lower'가 포함되어 있는지 확인하세요.");
        }
    }

    /**
     * [바디페인팅] 옷 텍스처를 마네킹 몸에 직접 적용
     * Y 위치 기반으로 상체/하체 구분하여 적용
     */
    applyTextureToBody(category, texture) {
        if (!this.mannequin) {
            console.error("마네킹이 없습니다!");
            return;
        }

        // 카테고리별 적용할 bodyRegion 결정
        let targetRegion;
        switch (category) {
            case 'top':
            case 'outer':
                targetRegion = 'upper';
                break;
            case 'bottom':
            case 'pants':
            case 'skirt':
                targetRegion = 'lower';
                break;
            case 'dress':
            case 'onepiece':
                targetRegion = 'all'; // 전체
                break;
            default:
                targetRegion = 'upper';
        }

        console.log(`[바디페인팅] ${category} 적용 시작... (대상: ${targetRegion})`);
        let appliedCount = 0;

        this.mannequin.traverse((node) => {
            if (node.isMesh && node.material) {
                const region = node.userData.bodyRegion || 'unknown';

                // 해당 영역인지 확인
                const shouldApply = targetRegion === 'all' || region === targetRegion;

                if (shouldApply) {
                    console.log(`  ✓ ${region} 메쉬에 옷 적용: ${node.name || '(이름없음)'}`);

                    // 원본 재질 저장
                    if (!node.userData.originalMaterial) {
                        node.userData.originalMaterial = node.material.clone();
                    }

                    // 옷 텍스처 적용
                    const newMaterial = new THREE.MeshStandardMaterial({
                        map: texture,
                        roughness: 0.5,
                        metalness: 0.1,
                        side: THREE.DoubleSide
                    });

                    node.userData.clothingCategory = category;
                    node.material = newMaterial;
                    appliedCount++;
                }
            }
        });

        console.log(`[바디페인팅] ${category} 적용 완료! (${appliedCount}개 메쉬에 적용)`);

        if (appliedCount === 0) {
            console.warn("적용된 메쉬가 없습니다. 콘솔의 메쉬 분석 결과를 확인해주세요.");
        }
    }

    /**
     * 옷 벗기기 (패널 메쉬 제거 + 원래 재질로 복원)
     */
    removeClothing(category) {
        if (!this.mannequin) return;

        // 1. 패널 메쉬 제거 (createClothingPanel로 생성된 옷)
        let targetSlot = category;
        if (!['top', 'bottom', 'dress', 'shoes'].includes(category)) targetSlot = 'top';

        if (this.clothingMeshes[targetSlot]) {
            this.mannequin.remove(this.clothingMeshes[targetSlot]);
            if (this.clothingMeshes[targetSlot].geometry) this.clothingMeshes[targetSlot].geometry.dispose();
            if (this.clothingMeshes[targetSlot].material) this.clothingMeshes[targetSlot].material.dispose();
            this.clothingMeshes[targetSlot] = null;
            console.log(`[옷 벗기기] ${targetSlot} 패널 제거 완료`);
        }

        // 2. 바디페인팅 재질 복원 (applyHybridBodyPainting 등으로 적용된 경우)
        this.mannequin.traverse((node) => {
            if (node.isMesh && node.userData.originalMaterial) {
                if (!category || node.userData.clothingCategory === category) {
                    node.material = node.userData.originalMaterial;
                    delete node.userData.clothingCategory;
                }
            }
        });
    }

    createClothingPanel(category, texture, aspectRatio) {
        // 카테고리 정규화
        let targetSlot = category;
        if (!['top', 'bottom', 'dress', 'shoes'].includes(category)) targetSlot = 'top';

        // 기존 옷 제거
        if (this.clothingMeshes[targetSlot]) {
            this.mannequin.remove(this.clothingMeshes[targetSlot]);
            this.clothingMeshes[targetSlot] = null;
        }

        // 상/하의/원피스 충돌 처리
        if (targetSlot === 'dress') {
            if (this.clothingMeshes['top']) { this.mannequin.remove(this.clothingMeshes['top']); this.clothingMeshes['top'] = null; }
            if (this.clothingMeshes['bottom']) { this.mannequin.remove(this.clothingMeshes['bottom']); this.clothingMeshes['bottom'] = null; }
        }
        if (['top', 'bottom'].includes(targetSlot)) {
            if (this.clothingMeshes['dress']) { this.mannequin.remove(this.clothingMeshes['dress']); this.clothingMeshes['dress'] = null; }
        }

        // ========== [핵심 업그레이드] 커브드 패널 (반원통형) ==========
        // 텍스처 설정: 곡면에 맞게 반복/오프셋 조정
        texture.wrapS = THREE.RepeatWrapping;
        texture.wrapT = THREE.ClampToEdgeWrapping;
        texture.repeat.set(1, 1); // 이미지 전체 사용
        texture.offset.set(0, 0);

        const panelMaterial = new THREE.MeshBasicMaterial({
            map: texture,
            transparent: true,
            side: THREE.DoubleSide,
            alphaTest: 0.01
        });

        let mesh;

        // 이미지 비율에 따른 크기 조정
        const sizeMultiplier = Math.min(1.0, Math.max(0.5, aspectRatio));
        console.log(`[피팅] 이미지 비율: ${aspectRatio.toFixed(2)}, 크기조절: ${sizeMultiplier.toFixed(2)}`);

        switch (targetSlot) {
            case 'top': {
                // 상의: 크기 키움
                const height = 0.80; // 0.70 -> 0.80
                const radius = 0.29; // 0.25 -> 0.29
                const thetaLength = Math.PI * 1.0; // 0.9 -> 1.0

                const geo = new THREE.CylinderGeometry(radius, radius * 0.85, height, 48, 1, true, -thetaLength / 2, thetaLength);
                mesh = new THREE.Mesh(geo, panelMaterial);
                mesh.position.set(0, 1.2, 0.05); // z를 앞으로 (하의보다 앞에 표시)
                mesh.scale.set(1, 1, 0.6);
                break;
            }
            case 'bottom': {
                // 하의: 크기 키움
                const height = 1.05; // 0.95 -> 1.05
                const radius = 0.32; // 0.28 -> 0.32
                const thetaLength = Math.PI * 1.1; // 1.0 -> 1.1

                const geo = new THREE.CylinderGeometry(radius, radius * 0.5, height, 48, 1, true, -thetaLength / 2, thetaLength);
                mesh = new THREE.Mesh(geo, panelMaterial);
                mesh.position.set(0, 0.50, -0.02);
                mesh.scale.set(1, 1, 0.6);
                break;
            }
            case 'dress': {
                // 원피스
                const height = 0.95 + (sizeMultiplier * 0.15);
                const radius = 0.2;
                const thetaLength = Math.PI * 1.0;

                const geo = new THREE.CylinderGeometry(radius, radius * 0.75, height, 48, 1, true, -thetaLength / 2, thetaLength);
                mesh = new THREE.Mesh(geo, panelMaterial);
                mesh.position.set(0, 0.95, 0);
                break;
            }
            case 'shoes': {
                const geo = new THREE.PlaneGeometry(0.35, 0.25);
                mesh = new THREE.Mesh(geo, panelMaterial);
                mesh.position.set(0, 0.12, 0.15);
                mesh.rotation.x = -Math.PI / 6;
                break;
            }
            default: {
                const geo = new THREE.PlaneGeometry(0.6, 0.6);
                mesh = new THREE.Mesh(geo, panelMaterial);
                mesh.position.set(0, 1.2, 0.3);
            }
        }

        // 상의가 하의보다 앞에 표시되도록 renderOrder 설정
        const renderOrderMap = {
            'top': 1001,     // 상의: 가장 앞
            'bottom': 999,   // 하의: 뒤
            'dress': 1000,   // 원피스: 중간
            'shoes': 998     // 신발: 가장 뒤
        };
        mesh.renderOrder = renderOrderMap[targetSlot] || 999;
        this.mannequin.add(mesh);
        this.clothingMeshes[targetSlot] = mesh;

        if (targetSlot === 'dress') {
            this.clothingMeshes['top'] = mesh;
            this.clothingMeshes['bottom'] = mesh;
        }
    }

    // 외부 호환용 메서드
    setTopType(type) { } // 더 이상 쓰지 않음
    setBottomType(type) { }
    setTopColor(hex) { }
    setBottomColor(hex) { }

    resetView() {
        if (!this.mannequin) return;
        this.mannequin.rotation.y = 0;
        this.removeAllClothing();
    }

    removeAllClothing() {
        if (!this.mannequin) return;

        ['top', 'bottom', 'shoes', 'dress'].forEach(slot => {
            if (this.clothingMeshes[slot]) {
                this.mannequin.remove(this.clothingMeshes[slot]);
                // cleanup
                if (this.clothingMeshes[slot].geometry) this.clothingMeshes[slot].geometry.dispose();
                if (this.clothingMeshes[slot].material) this.clothingMeshes[slot].material.dispose();
                this.clothingMeshes[slot] = null;
            }
        });
        this.clothingMeshes = { top: null, bottom: null, shoes: null, dress: null };
    }

    takeScreenshot() {
        this.renderer.render(this.scene, this.camera);
        const link = document.createElement('a');
        link.download = 'ootd-fit.png';
        link.href = this.renderer.domElement.toDataURL('image/png');
        link.click();
    }

    // OOTD 캘린더 저장용 Blob 반환 메서드
    takeScreenshotBlob() {
        this.renderer.render(this.scene, this.camera);
        return new Promise((resolve) => {
            this.renderer.domElement.toBlob((blob) => {
                resolve(blob);
            }, 'image/png');
        });
    }

    // 컨테이너 크기에 맞춰 리사이즈 (탭 전환 시 필수)
    resize() {
        if (!this.container || !this.camera || !this.renderer) return;
        const width = this.container.clientWidth;
        const height = this.container.clientHeight;

        if (width === 0 || height === 0) return;

        this.camera.aspect = width / height;
        this.camera.updateProjectionMatrix();
        this.renderer.setSize(width, height);
    }
}

window.initMannequin3D = function (id) { return new Mannequin3D(id); }
