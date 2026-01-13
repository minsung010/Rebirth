"""
YOLO + OCR + AI 등급 판정 통합 서버
기존 ocr/app.py를 건들지 않고 독립 구현
"""

from flask import Flask, request, jsonify, render_template
from flask_cors import CORS
import requests
import json
from ultralytics import YOLO
import cv2
import numpy as np
import base64
import os
from PIL import Image, ImageDraw, ImageFont
from rembg import remove
import google.generativeai as genai
from dotenv import load_dotenv

# 환경 변수 로드 (현재 폴더 + ocr 폴더)
load_dotenv()  # 현재 폴더
load_dotenv(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "yolo_analysis", ".env"))  # ocr 폴더

app = Flask(__name__)
CORS(app)

# ==============================
# 1. 설정
# ==============================
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
UPLOAD_FOLDER = os.path.join(BASE_DIR, "uploads")
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

# Google API 설정
GOOGLE_API_KEY = os.getenv("GOOGLE_API_KEY")
if GOOGLE_API_KEY:
    genai.configure(api_key=GOOGLE_API_KEY)

# YOLO 모델 로드
MODEL_PATH = os.path.join(BASE_DIR, "..", "model", "waste_clothing_extended", "weights", "best.pt")
if os.path.exists(MODEL_PATH):
    yolo_model = YOLO(MODEL_PATH)
    print(f"✅ YOLO 모델 로드 완료: {MODEL_PATH}")
    print(f"📋 클래스: {yolo_model.names}")
else:
    yolo_model = None
    print(f"⚠️ YOLO 모델을 찾을 수 없습니다: {MODEL_PATH}")

# ==============================
# 2. 가중치 및 등급 설정
# ==============================
DEFECT_WEIGHTS = {
    'pollution(외부오염)': 10,
    'wear_off(해짐)': 8,
    'torn(찢어짐)': 20,
    'ripped(뜯어짐)': 25,
}

# 결함 클래스 (바운딩 박스 표시)
DEFECT_CLASSES = ['pollution(외부오염)', 'torn(찢어짐)', 'ripped(뜯어짐)', 'wear_off(해짐)']
# 판정 클래스 (바운딩 박스 숨김)
JUDGMENT_CLASSES = ['dispose(폐기)', 'recycle(재활용)', 'reusable(재사용)']

# 클래스별 색상
CLASS_COLORS = {
    'pollution(외부오염)': (0, 165, 255),   # 주황
    'torn(찢어짐)': (0, 0, 255),            # 빨강
    'ripped(뜯어짐)': (0, 0, 200),          # 진한 빨강
    'wear_off(해짐)': (0, 255, 255),        # 노랑
}


# ==============================
# 3. 등급 계산
# ==============================
def calculate_grade(defect_counts):
    """
    결함 개수와 가중치를 기반으로 점수 및 등급 계산
    등급: S (90+), A (60+), B (60 미만 - 폐기/업사이클링)
    """
    score = 100
    
    for defect_type, count in defect_counts.items():
        weight = DEFECT_WEIGHTS.get(defect_type, 5)
        score -= weight * count
    
    score = max(0, score)
    
    if score >= 90:
        grade = "S등급"
        recommendation = "거의 새것 - 바로 판매 가능"
    elif score >= 60:
        grade = "A등급"
        recommendation = "양호 - 손질 후 판매/기부 권장"
    else:
        grade = "B등급"
        recommendation = "폐기 또는 업사이클링 권장"
    
    return grade, score, recommendation


# ==============================
# 4. YOLO 분석
# ==============================
def analyze_with_yolo(image_path, show_defects_only=True):
    """
    YOLO로 의류 결함 탐지
    
    Args:
        image_path: 이미지 경로
        show_defects_only: True면 결함만 표시, False면 모든 클래스 표시
    
    Returns:
        분석 결과 딕셔너리
    """
    if yolo_model is None:
        return {"error": "YOLO 모델이 로드되지 않았습니다."}
    
    img = cv2.imread(image_path)
    if img is None:
        return {"error": "이미지를 읽을 수 없습니다."}
    
    # YOLO 추론
    results = yolo_model(img, conf=0.15)
    
    defects = []
    defect_counts = {}
    judgments = []
    
    for box in results[0].boxes:
        cls_id = int(box.cls[0].item())
        cls_name = yolo_model.names[cls_id]
        conf = float(box.conf[0].item())
        x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
        
        detection = {
            "type": cls_name,
            "confidence": round(conf, 2),
            "bbox": [x1, y1, x2, y2]
        }
        
        # 결함 vs 판정 분류
        if cls_name in DEFECT_CLASSES:
            defects.append(detection)
            defect_counts[cls_name] = defect_counts.get(cls_name, 0) + 1
        else:
            judgments.append(detection)
    
    # 등급 계산
    grade, score, recommendation = calculate_grade(defect_counts)
    
    # 결함만 표시한 이미지 생성 (한글 폰트 지원)
    annotated_img = img.copy()
    pil_img = Image.fromarray(cv2.cvtColor(annotated_img, cv2.COLOR_BGR2RGB))
    draw = ImageDraw.Draw(pil_img)
    
    # 한글 폰트 로드
    try:
        font = ImageFont.truetype("malgun.ttf", 20)  # Windows 맑은고딕
    except:
        try:
            font = ImageFont.truetype("C:/Windows/Fonts/malgun.ttf", 20)
        except:
            font = ImageFont.load_default()
    
    for box in results[0].boxes:
        cls_id = int(box.cls[0].item())
        cls_name = yolo_model.names[cls_id]
        
        # 결함 클래스만 표시 (show_defects_only=True일 때)
        if show_defects_only and cls_name not in DEFECT_CLASSES:
            continue
        
        conf = float(box.conf[0].item())
        x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
        
        color = CLASS_COLORS.get(cls_name, (255, 255, 255))
        rgb_color = (color[2], color[1], color[0])  # BGR -> RGB
        
        # 박스 그리기
        draw.rectangle([x1, y1, x2, y2], outline=rgb_color, width=3)
        
        # 라벨 (한글만, 퍼센트 제거)
        if '(' in cls_name:
            label = cls_name.split('(')[1].replace(')', '')
        else:
            label = cls_name
        
        # 라벨 배경
        bbox = draw.textbbox((x1, y1 - 25), label, font=font)
        draw.rectangle([bbox[0]-2, bbox[1]-2, bbox[2]+2, bbox[3]+2], fill=rgb_color)
        draw.text((x1, y1 - 25), label, fill=(255, 255, 255), font=font)
    
    # PIL -> OpenCV 변환 후 Base64 인코딩
    annotated_img = cv2.cvtColor(np.array(pil_img), cv2.COLOR_RGB2BGR)
    _, buffer = cv2.imencode('.jpg', annotated_img)
    annotated_base64 = base64.b64encode(buffer).decode('utf-8')
    
    return {
        "defects": defects,
        "defect_summary": defect_counts,
        "judgments": judgments,
        "score": score,
        "grade": grade,
        "recommendation": recommendation,
        "annotated_image_base64": annotated_base64
    }


# ==============================
# 5. 누끼 제거
# ==============================
def remove_background(image_path):
    """배경 제거"""
    with Image.open(image_path) as img:
        img = img.convert("RGBA")
        result = remove(img)
        
        output_path = image_path.replace('.', '_nobg.')
        result.save(output_path, format="PNG")
        
        return output_path


# ==============================
# 6. OCR (기존 ocr/app.py와 완전 동일 - REST API 방식)
# ==============================
def perform_ocr(image_path):
    """Google Vision OCR - REST API 방식 (기존 ocr/app.py와 동일)"""
    VISION_API_KEY = os.getenv("VISION_API_KEY")
    
    if not VISION_API_KEY:
        print("❌ VISION_API_KEY가 설정되지 않았습니다.")
        return ""
    
    try:
        # 이미지 Base64 인코딩
        with open(image_path, 'rb') as f:
            img_base64 = base64.b64encode(f.read()).decode('utf-8')
        
        # Vision API 호출
        api_url = f"https://vision.googleapis.com/v1/images:annotate?key={VISION_API_KEY}"
        request_body = {
            "requests": [
                {
                    "image": {"content": img_base64},
                    "features": [{"type": "TEXT_DETECTION"}]
                }
            ]
        }
        
        response = requests.post(api_url, json=request_body)
        
        if response.status_code != 200:
            print(f"OCR API 오류: {response.status_code}")
            return ""
        
        data = response.json()
        
        try:
            text = data["responses"][0]["fullTextAnnotation"]["text"]
            return text
        except KeyError:
            return ""
            
    except Exception as e:
        print(f"OCR 오류: {e}")
        return ""


# ==============================
# 7. LLM 분석 (Gemini) - K-Fashion 상세 분류
# ==============================
def analyze_with_gemini(image_path=None, ocr_text=None):
    """Gemini로 의류/태그 분석 (K-Fashion 상세 분류) - 기존 ocr/app.py와 동일 모델 사용"""
    if not GOOGLE_API_KEY:
        return {"error": "Gemini API 키가 설정되지 않았습니다."}
    
    try:
        import time
        
        # 시도할 모델 목록 (기존 ocr/app.py와 동일)
        models_to_try = ['models/gemini-2.0-flash', 'models/gemini-flash-latest']
        
        if image_path:
            # K-Fashion 상세 분류 프롬프트
            with Image.open(image_path) as img:
                prompt = """이 의류 이미지를 K-Fashion 분류 기준에 따라 상세 분석해주세요.

[스타일 분류 옵션]
- 대분류: 트래디셔널, 매니시, 페미닌, 에스닉, 컨템포러리, 내추럴, 젠더플루이드, 스포티, 서브컬쳐, 캐주얼
- 세부스타일: 클래식, 프레피, 톰보이, 로맨틱, 섹시, 히피, 웨스턴, 오리엔탈, 모던, 아방가르드, 컨트리, 리조트, 스포티, 레트로, 스트리트, 힙합, 펑크, 키치, 밀리터리

[카테고리 옵션]
- 상의: 탑, 블라우스, 티셔츠, 니트웨어, 셔츠, 브라탑, 후드티
- 하의: 청바지, 팬츠, 스커트, 레깅스, 조거팬츠
- 아우터: 코트, 재킷, 점퍼, 패딩, 베스트, 가디건, 짚업
- 원피스: 드레스, 점프수트

[세부 속성 옵션]
- 컬러: 블랙, 화이트, 그레이, 레드, 핑크, 오렌지, 베이지, 브라운, 옐로우, 그린, 카키, 블루, 네이비, 퍼플, 실버, 골드, 멀티
- 디테일: 비즈, 단추, 니트꽈배기, 체인, 컷오프, 더블브레스티드, 드롭숄더, 자수, 프릴, 프린지, 레이스, 셔링, 퍼프, 페플럼, 포켓, 리본, 지퍼, 스터드
- 프린트: 체크, 스트라이프, 지그재그, 호피, 지브라, 도트, 카무플라쥬, 페이즐리, 아가일, 플로럴, 그래픽, 레터링, 타이다이, 믹스, 무지
- 소재: 퍼, 무스탕, 스웨이드, 헤어니트, 코듀로이, 시퀸, 데님, 저지, 니트, 트위드, 벨벳, 레더, 실크, 린넨, 면, 폴리에스터, 울, 플리스
- 기장(상의/아우터): 크롭, 노멀, 롱, 하프, 맥시
- 기장(하의/원피스): 미니, 니렝스, 미디, 발목, 맥시
- 소매기장: 민소매, 반팔, 캡, 7부소매, 긴팔
- 넥라인: 라운드넥, 유넥, 브이넥, 홀터넥, 오프숄더, 원숄더, 스퀘어넥, 노카라, 후드, 터틀넥, 보트넥, 스위트하트
- 칼라: 셔츠칼라, 보우칼라, 세일러칼라, 숄칼라, 폴로칼라, 피터팬칼라, 너치드칼라, 차이나칼라, 밴드칼라
- 핏: 타이트, 노멀, 루즈, 오버사이즈, 스키니, 와이드, 벨보텀

반드시 아래 JSON 형식으로만 답변하세요:
{
    "style": "대분류 스타일",
    "sub_style": "세부 스타일",
    "category": "상의/하의/아우터/원피스",
    "sub_category": "세부 카테고리",
    "color": "메인 컬러",
    "sub_color": "서브 컬러 (없으면 null)",
    "detail": ["디테일1", "디테일2"],
    "print": "프린트 패턴",
    "material": "추정 소재",
    "length": "기장",
    "sleeve_length": "소매기장",
    "neckline": "넥라인",
    "collar": "칼라 (없으면 null)",
    "fit": "핏",
    "season": "봄/여름/가을/겨울/사계절",
    "short_name": "짧은 의류명 (3-5단어, 예: 그레이 후드집업, 블랙 와이드팬츠)"
}"""
                # 모델 fallback 시도
                last_error = None
                for model_name in models_to_try:
                    try:
                        print(f"[K-Fashion] Trying model: {model_name}")
                        model = genai.GenerativeModel(
                            model_name,
                            generation_config={"response_mime_type": "application/json"}
                        )
                        response = model.generate_content([prompt, img])
                        print(f"[K-Fashion] Success with model: {model_name}")
                        return json.loads(response.text)
                    except Exception as model_error:
                        print(f"[K-Fashion] Model {model_name} failed: {model_error}")
                        last_error = model_error
                        time.sleep(1)
                        continue
                
                raise last_error
                
        elif ocr_text:
            # OCR 텍스트 분석
            prompt = f"""다음 케어라벨 OCR 텍스트를 분석해주세요:
            {ocr_text}
            
            JSON 형식으로 답변:
            {{
                "material": "소재 (예: 면 100%, 폴리에스터 80% + 면 20%)",
                "size": "사이즈",
                "brand": "브랜드",
                "origin": "제조국",
                "washing_instruction": "세탁 방법"
            }}"""
            
            # 모델 fallback 시도
            last_error = None
            for model_name in models_to_try:
                try:
                    print(f"[OCR-LLM] Trying model: {model_name}")
                    model = genai.GenerativeModel(
                        model_name,
                        generation_config={"response_mime_type": "application/json"}
                    )
                    response = model.generate_content(prompt)
                    print(f"[OCR-LLM] Success with model: {model_name}")
                    return json.loads(response.text)
                except Exception as model_error:
                    print(f"[OCR-LLM] Model {model_name} failed: {model_error}")
                    last_error = model_error
                    time.sleep(1)
                    continue
            
            raise last_error
        else:
            return {"error": "이미지 또는 OCR 텍스트가 필요합니다."}
        
    except Exception as e:
        print(f"Gemini 오류: {e}")
        return {"error": str(e)}


# ==============================
# 7-2. 케어라벨 이미지 분석 (Gemini Vision) - 색칠된 사이즈 추출
# ==============================
def analyze_label_with_gemini(image_path, ocr_text=""):
    """
    케어라벨 이미지를 직접 분석하여 색칠/강조된 사이즈 정확히 추출
    OCR 텍스트가 여러 사이즈를 읽어도 이미지에서 강조된 것만 추출
    """
    if not GOOGLE_API_KEY:
        return {"error": "Gemini API 키가 설정되지 않았습니다."}
    
    try:
        import time
        
        models_to_try = ['models/gemini-2.0-flash', 'models/gemini-flash-latest']
        
        with Image.open(image_path) as img:
            prompt = f"""이 의류 케어라벨/태그 이미지를 분석해주세요.

[중요 규칙]
1. **사이즈**: 사이즈가 여러 개 나열되어 있고 그 중 하나가 검은색 박스/색칠/강조 표시가 되어 있다면, 
   강조된 사이즈 하나만 추출하세요. (예: "24 25 26 [27] 28 29" 에서 27만 추출)
2. 태그 하단의 바코드 아래에 적힌 사이즈 정보도 참고하세요 (예: "67(27)" 에서 27)
3. 소재 정보가 백분율로 표시되어 있으면 정확히 읽어주세요

[OCR 참고 텍스트]
{ocr_text}

반드시 아래 JSON 형식으로만 답변하세요:
{{
    "brand": "브랜드명",
    "material": "소재 (예: 면 100%, 폴리에스터 80% + 면 20%)",
    "size": "실제 선택된 사이즈 (강조/색칠된 사이즈만)",
    "origin": "제조국",
    "price": "가격 (있는 경우)",
    "product_code": "품번 (있는 경우)",
    "color": "색상 (있는 경우)",
    "washing_instruction": "세탁 방법"
}}"""
            
            # 모델 fallback 시도
            last_error = None
            for model_name in models_to_try:
                try:
                    print(f"[Label-Vision] Trying model: {model_name}")
                    model = genai.GenerativeModel(
                        model_name,
                        generation_config={"response_mime_type": "application/json"}
                    )
                    response = model.generate_content([prompt, img])
                    print(f"[Label-Vision] Success with model: {model_name}")
                    result = json.loads(response.text)
                    print(f"[Label-Vision] Extracted size: {result.get('size', 'N/A')}")
                    return result
                except Exception as model_error:
                    print(f"[Label-Vision] Model {model_name} failed: {model_error}")
                    last_error = model_error
                    time.sleep(1)
                    continue
            
            raise last_error
            
    except Exception as e:
        print(f"Label Vision 오류: {e}")
        return {"error": str(e)}


# ==============================
# 8. API 엔드포인트
# ==============================
@app.route("/")
def index():
    return render_template("index.html")


@app.route("/analyze", methods=["POST"])
def analyze():
    """
    통합 분석 API
    - 의류 이미지: YOLO 결함 탐지 + Gemini 분석
    - 케어라벨: OCR + Gemini 분석
    """
    clothing_file = request.files.get("clothingImage")
    label_file = request.files.get("labelImage")
    
    result = {"success": True}
    
    try:
        # 의류 이미지 처리
        if clothing_file:
            clothing_path = os.path.join(UPLOAD_FOLDER, clothing_file.filename)
            clothing_file.save(clothing_path)
            
            # 누끼 제거
            nobg_path = remove_background(clothing_path)
            with open(nobg_path, 'rb') as f:
                result["clothing_image_no_bg_base64"] = base64.b64encode(f.read()).decode('utf-8')
            
            # YOLO 분석 (결함만 표시)
            result["condition_analysis"] = analyze_with_yolo(clothing_path, show_defects_only=True)
            
            # Gemini 의류 분석
            result["clothing_analysis"] = analyze_with_gemini(image_path=clothing_path)
        
        # 케어라벨 처리
        if label_file:
            label_path = os.path.join(UPLOAD_FOLDER, label_file.filename)
            label_file.save(label_path)
            
            # OCR
            ocr_text = perform_ocr(label_path)
            result["ocr_text"] = ocr_text
            
            # Gemini 태그 분석 (이미지 + OCR 텍스트 함께 전달)
            result["tag_analysis"] = analyze_label_with_gemini(label_path, ocr_text)
        
        return jsonify(result)
        
    except Exception as e:
        import traceback
        print(f"분석 오류: {e}")
        print(traceback.format_exc())
        return jsonify({"success": False, "error": str(e)}), 500


@app.route("/yolo-only", methods=["POST"])
def yolo_only():
    """YOLO 분석만 수행"""
    if "image" not in request.files:
        return jsonify({"error": "이미지가 필요합니다."}), 400
    
    file = request.files["image"]
    path = os.path.join(UPLOAD_FOLDER, file.filename)
    file.save(path)
    
    show_all = request.form.get("show_all", "false").lower() == "true"
    result = analyze_with_yolo(path, show_defects_only=not show_all)
    
    return jsonify(result)


# ==============================
# 9. 서버 실행
# ==============================
if __name__ == "__main__":
    print("=" * 50)
    print("🚀 YOLO + OCR 통합 분석 서버")
    print("=" * 50)
    print(f"📁 업로드 폴더: {UPLOAD_FOLDER}")
    print(f"🤖 YOLO 모델: {'✅ 로드됨' if yolo_model else '❌ 없음'}")
    print(f"🔑 Gemini API: {'✅ 설정됨' if GOOGLE_API_KEY else '❌ 없음'}")
    print("=" * 50)
    
    app.run(host="0.0.0.0", port=5000, debug=True)
