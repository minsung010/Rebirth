# 🌱 Re:birth (리버스) - 지속 가능한 패션 플랫폼

> "옷 한 벌에 담긴 2,700리터의 물과 2.6kg의 탄소를 지킵니다."

**Re:birth**는 입지 않는 옷을 공유하고, AI 기술을 통해 새로운 가치를 발견하며, 지속 가능한 패션 라이프스타일을 제안하는 플랫폼입니다.

---

## 🚀 프로젝트 소개 (Overview)

패션 산업은 전 세계 온실가스 배출량의 약 10%를 차지합니다. 우리는 '옷장 속에 잠든 옷'을 깨워 자원 순환을 돕고, 환경 보호를 실천하고자 합니다.

*   **Period**: 2024.12 ~ 2025.01 (약 2개월)
*   **Team**: 1인 개발 (Full Stack)
*   **Keywords**: `Spring Boot`, `AI Agent`, `RAG`, `Vector Search`, `3D WebGL`, `Sustainability`

---

## 🛠 사용 기술 (Tech Stack)

### Backend
*   **Java 17** / **Spring Boot 3.x**
*   **MyBatis** / **Oracle Database** (Main Memory)
*   **Milvus 2.3** (Vector Database)
*   **Spring Security & OAuth2** (Kakao, Naver, Google Login)

### AI & Data
*   **LLM Engine**: `Groq LPU` + `Llama 3.3 70B` (Real-time Chat)
*   **Embedding**: `Google Gemini text-embedding-004`
*   **External APIs**:
    *   `Kakao Maps API` (Geocoding & Maps)
    *   `기상청 단기예보 API` (Weather)

### Frontend
*   **HTML/CSS/JS** (Thymeleaf)
*   **Three.js** (3D Rendering)
*   **FullCalendar** (Scheduling)
*   **TailwindCSS**

---

## 💡 주요 기능 (Key Features)

### 1. 💬 AI 퍼스널 에이전트 (Actionable AI)
*   **초고속 대화**: Groq LPU를 도입하여 지연 없는 실시간 대화 경험 제공
*   **하이브리드 RAG**: `Milvus(Vector)`와 `Oracle(Keyword)` 검색을 결합하여 내 옷장 기반의 정교한 스타일링 제안
*   **실시간 날씨 코디**: 사용자 위치의 실시간 날씨(기온, 강수)를 파악하여 TPO에 맞는 옷차림 추천
*   **앱 제어(Agentic)**: 대화만으로 페이지 이동, 포인트 조회, 옷 등록 화면 연결 등 앱 제어 가능

### 2. 🛒 하이퍼로컬 마켓 (Re:Store)
*   **동네 기반 거래**: 내 위치 반경 Nkm 이내의 의류만 직거래 가능
*   **실시간 거리 정렬**: 하버사인 공식(Haversine Formula) 자체 구현으로 대량의 매물을 거리순 실시간 정렬
*   **간편 등록**: 사진 업로드 시 Gemini Vision AI가 옷의 특징(색상, 카테고리)을 자동 분석

### 3. 👕 3D 가상 피팅룸 (Virtual Fitting)
*   **웹 3D 시각화**: Three.js 기반의 3D 공간에서 내 옷을 마네킹에 입혀보는 경험 제공
*   **커브드 패널(Curved Panel)**: 2D 옷 이미지를 입체적인 마네킹에 왜곡 없이 입히기 위한 독자적인 렌더링 기법 적용
*   **OOTD 캘린더**: 완성된 코디를 캡처하여 캘린더에 기록하고 나만의 스타일 로그 관리

---

## 🔧 기술적 문제 해결 (Troubleshooting)

### 1️⃣ AI Chatbot
> **Problem**: Llama 모델 사용 시 다국어/외계어(`thật`, `ayrıca` 등)가 섞여 나오는 Hallucination 발생
> **Solution**: **유니코드 필터링 & 프롬프트 강화**
> 정규식을 적용하여 비한국어 유니코드 범위를 제거하고, 시스템 프롬프트에 한국어 출력 규칙 강제화

> **Problem**: 서버 재시작 시마다 전체 옷 데이터를 벡터 DB로 동기화하여 부팅 지연
> **Solution**: **On-Demand Sync Strategy**
> 서버 시작 시 동기화를 스킵하고, 신규 옷 등록 시점에만 실시간으로 임베딩/저장하도록 로직 변경

### 2️⃣ Location Service
> **Problem**: Oracle DB 버전에 Spatial Index 부재로 위치 기반 거리 정렬 쿼리 작성 난항
> **Solution**: **Haversine Formula Application**
> Java 레벨에서 두 좌표 간의 구면 거리를 계산하는 공식을 직접 구현하여 DB 의존성 제거 및 0.01초 내 정렬 속도 달성

### 3️⃣ 3D Rendering
> **Problem**: 2D 옷 이미지를 3D 마네킹에 텍스처링 시 로고와 주름이 심하게 왜곡됨
> **Solution**: **Curved Panel Overlay**
> 마네킹 몸체가 아닌, 마네킹 앞에 살짝 뜬 반원통형 투명 패널에 텍스처를 맵핑하여 입체감은 살리고 왜곡은 최소화

---

## ⚙️ 설치 및 실행 (Installation)

**1. Clone the repository**
```bash
git clone https://github.com/minsung010/Rebirth.git
cd Rebirth
```

**2. Configure API Keys**
`src/main/resources/application.properties` 파일에서 아래 키 값을 본인의 키로 변경해야 합니다.
```properties
# Google Gemini
google.gemini.api-key=YOUR_GEMINI_KEY

# Groq (Llama 3)
groq.api-key=YOUR_GROQ_KEY

# Kakao Maps
kakao.maps.appkey=YOUR_APP_KEY
kakao.rest-api-key=YOUR_REST_KEY

# Data Portal (Weather)
kma.api-key=YOUR_KMA_KEY

# DB & Mail
spring.datasource.password=YOUR_DB_PASSWORD
spring.mail.password=YOUR_MAIL_PASSWORD
```

**3. Run Application**
```bash
./mvnw spring-boot:run
```
