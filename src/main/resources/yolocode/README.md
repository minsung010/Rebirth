# YOLO + OCR + AI 등급 판정 통합 서버

Flask 서버로 YOLO 모델 기반 의류 상태 분석 + OCR + AI 등급 판정을 제공합니다.

## 기능
- 의류 이미지 결함 탐지 (YOLO)
- 케어라벨 OCR
- AI 등급 판정 (S/A/B/C)

## 실행
```bash
conda activate epro2
python app.py
```

## API 엔드포인트
- `POST /analyze` - 통합 분석
- `GET /` - 테스트 페이지
