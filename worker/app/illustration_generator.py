"""
Once Upon a Time - 동화 일러스트 생성 엔진
프로젝트: P-실무 (Gachon University)
버전: v1.0_Production

[기능 설명]
1. Google Imagen 4.0 Fast 모델 사용
2. 동화 내용 기반 자동 프롬프트 생성
3. 주인공 외형/의상/배경 자동 분석
4. 한국 문화 요소 자연스럽게 반영
5. 표지 이미지 자동 생성 (630x800)

[원본 코드 출처]
Colab Notebook: 동화_일러스트_생성_v_4_.ipynb
변환 일시: 2024-12-12
"""

import os
import sys
import json
import base64
import requests
from typing import Dict, List, Optional, Tuple
from PIL import Image
import io
import logging

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


# ==============================================================================
# 동화 내용 분석기
# ==============================================================================

class StoryAnalyzer:
    """동화 내용 기반 이미지 생성 정보 분석"""
    
    # 의상 키워드 매핑 (원본 유지)
    CLOTHING_KEYWORDS = {
        '한복': 'wearing colorful traditional Korean hanbok',
        '교복': 'wearing school uniform',
        '잠옷': 'wearing cozy pajamas',
        '운동복': 'wearing comfortable sportswear',
        '원피스': 'wearing a pretty dress',
        '티셔츠': 'wearing casual t-shirt and pants',
    }
    
    # 장소 키워드 매핑 (원본 유지)
    LOCATION_KEYWORDS = {
        '학교': 'school environment with classroom elements',
        '운동장': 'school playground with play equipment',
        '교실': 'bright classroom setting',
        '놀이터': 'colorful playground',
        '집': 'cozy home interior',
        '방': 'child\'s bedroom',
        '부엌': 'warm kitchen',
        '공원': 'beautiful park with trees',
        '숲': 'enchanted forest',
        '산': 'Korean mountain landscape',
        '강': 'peaceful riverside',
        '바다': 'seaside scenery',
        '마을': 'Korean village',
        '도시': 'modern city',
        '궁': 'traditional Korean palace',
        '한옥': 'traditional Korean house',
    }
    
    # 오브젝트 키워드 (원본 유지)
    OBJECT_KEYWORDS = {
        '책': 'books',
        '시계': 'clock',
        '가방': 'backpack',
        '도시락': 'lunchbox',
        '우산': 'umbrella',
        '꽃': 'flowers',
        '나무': 'trees',
        '별': 'stars',
        '구름': 'clouds',
        '새': 'birds',
        '나비': 'butterflies',
        '공': 'ball',
        '인형': 'doll',
        '악기': 'musical instrument',
    }
    
    # 한국 문화 요소 (원본 유지)
    KOREAN_ITEMS = {
        '한복': 'traditional Korean hanbok',
        '떡': 'Korean rice cakes',
        '한옥': 'traditional Korean architecture',
        '궁': 'palace architecture',
        '산': 'Korean mountains',
        '강': 'Korean river',
        '전통': 'traditional Korean patterns',
        '등': 'traditional Korean lanterns',
        '부채': 'traditional Korean fan',
    }
    
    def __init__(self):
        """초기화"""
        pass
    
    def analyze(self, story_data: Dict) -> Dict:
        """
        동화 내용을 깊이 분석하여 이미지 생성 정보 추출
        
        Args:
            story_data: 동화 JSON 데이터
            
        Returns:
            분석 결과 딕셔너리
        """
        keywords = story_data.get('story_info', {}).get('keywords', [])
        content = story_data.get('content', '')
        summary = story_data.get('story_info', {}).get('summary', '')
        
        # 전체 텍스트
        full_text = f"{summary} {content}"
        
        # 분석 수행
        analysis = {
            'character': self._analyze_character(full_text),
            'age': self._analyze_age(full_text),
            'clothing': self._analyze_clothing(full_text),
            'setting_elements': self._analyze_setting(full_text, keywords),
            'time_of_day': self._analyze_time(full_text),
            'mood': self._analyze_mood(full_text),
            'key_objects': self._analyze_objects(full_text),
            'korean_elements': self._analyze_korean_elements(full_text)
        }
        
        return analysis
    
    def _analyze_character(self, text: str) -> str:
        """주인공 분석"""
        character_desc = "a young Korean child"
        
        # 성별 파악
        if '소녀' in text or '여자' in text or '공주' in text:
            character_desc = "a young Korean girl"
        elif '소년' in text or '남자' in text or '왕자' in text:
            character_desc = "a young Korean boy"
        
        return character_desc
    
    def _analyze_age(self, text: str) -> str:
        """나이대 파악"""
        age_desc = ""
        
        if '유치원' in text or '어린이집' in text:
            age_desc = "preschool age (4-6 years old)"
        elif '초등학생' in text or '학교' in text:
            age_desc = "elementary school age (7-9 years old)"
        
        return age_desc
    
    def _analyze_clothing(self, text: str) -> str:
        """의상/외형 분석"""
        clothing_desc = ""
        
        # 명시적 의상 언급 확인
        for keyword, desc in self.CLOTHING_KEYWORDS.items():
            if keyword in text:
                clothing_desc = desc
                break
        
        # 의상 언급 없으면 상황에 맞게 자동 결정
        if not clothing_desc:
            if '학교' in text or '수업' in text:
                clothing_desc = 'wearing casual school clothes'
            elif '잠' in text or '꿈' in text or '밤' in text:
                clothing_desc = 'wearing comfortable sleepwear'
            elif '놀이터' in text or '운동장' in text:
                clothing_desc = 'wearing playful casual clothes'
            elif '조선' in text or '옛날' in text or '전통' in text:
                clothing_desc = 'wearing traditional Korean hanbok'
            else:
                clothing_desc = 'wearing comfortable everyday clothes'
        
        return clothing_desc
    
    def _analyze_setting(self, text: str, keywords: List[str]) -> List[str]:
        """배경/장소 분석"""
        setting_elements = []
        
        for keyword, desc in self.LOCATION_KEYWORDS.items():
            if keyword in text or keyword in keywords:
                setting_elements.append(desc)
        
        # 기본 배경 (아무것도 없으면)
        if not setting_elements:
            setting_elements = ['soft dreamy background']
        
        return setting_elements[:3]  # 최대 3개
    
    def _analyze_time(self, text: str) -> str:
        """시간대/분위기 분석"""
        time_of_day = ""
        
        if '아침' in text:
            time_of_day = "bright morning atmosphere with warm sunlight"
        elif '낮' in text or '점심' in text:
            time_of_day = "cheerful daytime with clear bright light"
        elif '저녁' in text or '해질녘' in text:
            time_of_day = "warm evening with golden hour lighting"
        elif '밤' in text or '달' in text:
            time_of_day = "gentle nighttime with soft moonlight"
        
        return time_of_day
    
    def _analyze_mood(self, text: str) -> str:
        """감정/분위기 분석"""
        mood = "heartwarming and magical"
        
        if '모험' in text or '탐험' in text:
            mood = "adventurous and exciting"
        elif '신비' in text or '마법' in text:
            mood = "mystical and enchanting"
        elif '따뜻' in text or '사랑' in text or '우정' in text:
            mood = "warm and touching"
        elif '재미' in text or '웃음' in text:
            mood = "fun and joyful"
        elif '용기' in text or '도전' in text:
            mood = "brave and inspiring"
        elif '슬픔' in text or '외로' in text:
            mood = "gentle and comforting"
        
        return mood
    
    def _analyze_objects(self, text: str) -> List[str]:
        """주요 소품/오브젝트 분석"""
        key_objects = []
        
        for keyword, obj in self.OBJECT_KEYWORDS.items():
            if keyword in text:
                key_objects.append(obj)
        
        return key_objects[:3]  # 최대 3개
    
    def _analyze_korean_elements(self, text: str) -> List[str]:
        """한국 문화 요소 분석"""
        korean_elements = []
        
        for keyword, element in self.KOREAN_ITEMS.items():
            if keyword in text:
                korean_elements.append(element)
        
        return korean_elements[:2]  # 최대 2개


# ==============================================================================
# 프롬프트 생성기
# ==============================================================================

class PromptGenerator:
    """동화 분석 결과 기반 Imagen 프롬프트 생성"""
    
    # 분위기별 색상 (원본 유지)
    MOOD_COLORS = {
        'adventurous and exciting': 'vibrant warm colors with dynamic energy',
        'mystical and enchanting': 'soft purple, blue, and golden magical tones',
        'warm and touching': 'warm pastel colors with gentle pink and orange',
        'heartwarming and magical': 'soft warm pastel colors with gentle glow',
        'fun and joyful': 'bright cheerful colors with playful energy',
        'brave and inspiring': 'strong confident colors with warm highlights',
        'gentle and comforting': 'soft soothing colors with gentle warmth',
    }
    
    def __init__(self):
        """초기화"""
        pass
    
    def generate(self, analysis: Dict) -> str:
        """
        동화 분석 결과로부터 완전 동적 프롬프트 생성
        
        Args:
            analysis: StoryAnalyzer.analyze() 결과
            
        Returns:
            Imagen 4.0용 프롬프트 문자열
        """
        # 캐릭터 설명
        character_full = f"{analysis['character']}"
        if analysis['age']:
            character_full += f", {analysis['age']}"
        if analysis['clothing']:
            character_full += f", {analysis['clothing']}"
        
        # 배경 요소 조합
        setting_text = " and ".join(analysis['setting_elements'])
        
        # 시간대 추가
        if analysis['time_of_day']:
            setting_text += f", {analysis['time_of_day']}"
        
        # 주요 오브젝트
        objects_text = ""
        if analysis['key_objects']:
            objects_text = f"Include {', '.join(analysis['key_objects'])} as natural elements in the scene."
        
        # 한국 문화 요소 (있으면 자연스럽게 추가)
        korean_text = ""
        if analysis['korean_elements']:
            korean_text = f"Subtle Korean cultural elements: {', '.join(analysis['korean_elements'])}."
        
        # 분위기별 색상
        color_palette = self.MOOD_COLORS.get(
            analysis['mood'], 
            'warm soft colors'
        )
        
        # 최종 프롬프트 (원본 유지)
        prompt = f"""
NO TEXT. NO Korean hangul (한글). NO English (ABC). Pure illustration only.

A beautiful watercolor illustration painting for a children's storybook.

ABSOLUTELY CRITICAL REQUIREMENTS - READ CAREFULLY:
This is a pure illustration painting WITHOUT any text elements.
- NO text of any kind whatsoever
- NO Korean hangul characters (한글, ㄱㄴㄷ, 가나다, etc.)
- NO English letters or alphabet (ABC, abc)
- NO numbers or symbols (123, !, @, #)
- NO signs, banners, labels, or any surface with writing
- NO book titles, no speech bubbles, no captions
- All surfaces must be completely blank without any readable text
- This is a clean visual artwork only - pure illustration without any typography
- Do not add title, do not add words, keep all areas completely text-free
- Even decorative text-like patterns are not allowed

Character Description:
{character_full}. Natural joyful expression with bright eyes and gentle smile. The character is the main focus with clear, recognizable features suitable for young children aged 3-7.

Setting and Background:
{setting_text}. Soft atmospheric background that creates depth without overwhelming the character. {objects_text}

Background Style:
Dreamy soft-focus bokeh effect with {color_palette}. Subtle depth with layered atmosphere, gentle magical glow around the character creating a warm inviting feeling.

Mood and Atmosphere:
{analysis['mood']}, whimsical enchanting atmosphere that appeals to children aged 3-7. Safe, comforting, and engaging visual style.

Korean Cultural Context:
{korean_text if korean_text else "Subtle Korean aesthetic in a way that feels natural and accessible to young children."}

Art Technique:
Professional watercolor illustration painting with soft gentle brushstrokes, storybook art quality for young children, high-quality children's book illustration style with rich colors and clear details.

Composition:
Portrait-style focusing on the character (character takes up 60-70% of the frame), centered composition with atmospheric dreamy background that complements but doesn't distract from the main character.

Image Quality:
High resolution professional artwork, child-friendly, age-appropriate for 3-7 years old, safe and comforting visual style. Professional children's book illustration quality.


"""
        
        return prompt


# ==============================================================================
# 이미지 생성기
# ==============================================================================

class IllustrationGenerator:
    """Google Imagen 4.0 Fast 기반 동화 일러스트 생성기"""
    
    def __init__(self, api_key: str):
        """
        초기화
        
        Args:
            api_key: Google Gemini API 키
        """
        self.api_key = api_key
        self.analyzer = StoryAnalyzer()
        self.prompt_generator = PromptGenerator()
        
        logger.info("✅ IllustrationGenerator 초기화 완료")
    
    def generate_cover_image(
        self,
        story_data: Dict,
        output_path: str = "cover_image.png",
        target_size: Tuple[int, int] = (630, 800)
    ) -> Tuple[str, Image.Image]:
        """
        동화 표지 이미지 생성
        
        Args:
            story_data: 동화 JSON 데이터
            output_path: 저장 경로
            target_size: 최종 이미지 크기 (width, height)
            
        Returns:
            (저장 경로, PIL Image) 튜플
        """
        logger.info("🎨 [표지 이미지 생성 시작 - 동화 내용 기반]")
        logger.info("=" * 70)
        
        # 동화 깊이 분석
        title = story_data.get('story_info', {}).get('title', '제목 없음')
        analysis = self.analyzer.analyze(story_data)
        
        logger.info(f"📖 제목: {title}")
        logger.info(f"👤 주인공: {analysis['character']}")
        logger.info(f"👔 의상: {analysis['clothing']}")
        logger.info(f"📍 배경: {', '.join(analysis['setting_elements'])}")
        logger.info(f"💫 분위기: {analysis['mood']}")
        
        if analysis['key_objects']:
            logger.info(f"🎯 주요 소품: {', '.join(analysis['key_objects'])}")
        if analysis['korean_elements']:
            logger.info(f"🏷️ 한국 요소: {', '.join(analysis['korean_elements'])}")
        
        # 동적 프롬프트 생성
        image_prompt = self.prompt_generator.generate(analysis)
        
        logger.info("\n📝 동적 프롬프트 생성 완료")
        logger.info("-" * 70)
        
        try:
            logger.info("⏳ Imagen 4.0 Fast로 이미지 생성 중... (30-60초 소요)\n")
            
            # REST API 호출
            url = f"https://generativelanguage.googleapis.com/v1beta/models/imagen-4.0-fast-generate-001:predict?key={self.api_key}"
            
            payload = {
                "instances": [{
                    "prompt": image_prompt
                }],
                "parameters": {
                    "sampleCount": 1,
                    "aspectRatio": "3:4"
                }
            }
            
            headers = {"Content-Type": "application/json"}
            response = requests.post(url, json=payload, headers=headers, timeout=90)
            
            logger.info(f"📬 HTTP 상태 코드: {response.status_code}\n")
            
            if response.status_code == 200:
                result = response.json()
                
                # 이미지 추출
                image_b64 = self._extract_image_from_response(result)
                
                if image_b64:
                    # Base64 디코딩 및 PIL Image 변환
                    image_bytes = base64.b64decode(image_b64)
                    pil_image = Image.open(io.BytesIO(image_bytes))
                    
                    logger.info(f"\n✅ 원본 이미지 생성 완료 (크기: {pil_image.size})")
                    
                    # 리사이즈
                    resized_image = pil_image.resize(target_size, Image.Resampling.LANCZOS)
                    resized_image.save(output_path, format='PNG', quality=95)
                    
                    logger.info(f"✅ 리사이즈 완료: {resized_image.size} (W:{target_size[0]}, H:{target_size[1]})")
                    logger.info(f"💾 저장 완료: {output_path}")
                    logger.info("=" * 70)
                    
                    return output_path, resized_image
                else:
                    raise Exception("API 응답에서 이미지 데이터를 찾을 수 없습니다")
            else:
                error_msg = response.text
                logger.error(f"❌ API 오류 {response.status_code}:")
                logger.error(error_msg[:500])
                raise Exception(f"API 호출 실패: {response.status_code}")
        
        except Exception as e:
            logger.error(f"\n❌ 이미지 생성 실패: {e}")
            
            import traceback
            logger.error("\n🔍 상세 오류:")
            logger.error(traceback.format_exc())
            
            # 대체 이미지 생성
            logger.warning("\n⚠️ 임시 대체 이미지 생성...")
            blank_image = Image.new('RGB', target_size, color=(255, 240, 245))
            blank_image.save(output_path, format='PNG')
            
            return output_path, blank_image
    
    def _extract_image_from_response(self, response_data: Dict) -> Optional[str]:
        """
        Imagen API 응답에서 Base64 이미지 추출
        
        Args:
            response_data: API 응답 JSON
            
        Returns:
            Base64 인코딩된 이미지 문자열 또는 None
        """
        try:
            # predictions 배열에서 첫 번째 항목의 bytesBase64Encoded 추출
            if 'predictions' in response_data:
                predictions = response_data['predictions']
                if predictions and len(predictions) > 0:
                    first_prediction = predictions[0]
                    
                    # bytesBase64Encoded 필드 찾기
                    if 'bytesBase64Encoded' in first_prediction:
                        return first_prediction['bytesBase64Encoded']
                    
                    # mimeType과 함께 있는 경우
                    if 'image' in first_prediction:
                        image_data = first_prediction['image']
                        if 'bytesBase64Encoded' in image_data:
                            return image_data['bytesBase64Encoded']
            
            logger.error("응답 구조에서 이미지 데이터를 찾을 수 없습니다")
            return None
            
        except Exception as e:
            logger.error(f"이미지 추출 중 오류: {e}")
            return None


# ==============================================================================
# 스프링부트 연동용 메인 함수
# ==============================================================================

def generate_cover_from_json_file(
    json_file_path: str,
    output_path: Optional[str] = None,
    api_key: Optional[str] = None,
    target_size: Tuple[int, int] = (630, 800)
) -> Dict:
    """
    JSON 파일로부터 표지 이미지 생성 (스프링부트 연동용)
    
    Args:
        json_file_path: 동화 JSON 파일 경로
        output_path: 이미지 저장 경로 (None이면 자동 생성)
        api_key: Google Gemini API 키 (None이면 환경 변수에서 읽음)
        target_size: 최종 이미지 크기
        
    Returns:
        결과 메타데이터
    """
    # API 키 확인
    if api_key is None:
        api_key = os.getenv("GEMINI_API_KEY")
    
    if not api_key:
        raise ValueError("GEMINI_API_KEY가 설정되지 않았습니다.")
    
    # JSON 로드
    with open(json_file_path, 'r', encoding='utf-8') as f:
        story_data = json.load(f)
    
    # 출력 경로 자동 생성
    if output_path is None:
        title = story_data.get('story_info', {}).get('title', '제목없음')
        safe_title = "".join([
            c if c.isalnum() or c in ['_', '-'] else "_" 
            for c in title
        ])
        output_path = f"cover_{safe_title}.png"
    
    # 생성기 초기화
    generator = IllustrationGenerator(api_key=api_key)
    
    # 이미지 생성
    saved_path, image = generator.generate_cover_image(
        story_data=story_data,
        output_path=output_path,
        target_size=target_size
    )
    
    # 메타데이터 반환
    metadata = {
        "success": True,
        "cover_image_path": saved_path,
        "image_size": image.size,
        "story_title": story_data.get('story_info', {}).get('title', '제목 없음')
    }
    
    return metadata


# ==============================================================================
# 테스트 코드
# ==============================================================================

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description="동화 일러스트 생성기")
    parser.add_argument("json_file", help="동화 JSON 파일 경로")
    parser.add_argument("--output", help="출력 이미지 경로", default=None)
    parser.add_argument("--api-key", help="Gemini API 키", default=None)
    parser.add_argument("--width", type=int, help="이미지 너비", default=630)
    parser.add_argument("--height", type=int, help="이미지 높이", default=800)
    
    args = parser.parse_args()
    
    logger.info("=" * 60)
    logger.info("Once Upon a Time - 동화 일러스트 생성")
    logger.info("=" * 60)
    
    try:
        metadata = generate_cover_from_json_file(
            json_file_path=args.json_file,
            output_path=args.output,
            api_key=args.api_key,
            target_size=(args.width, args.height)
        )
        
        logger.info("\n✅ 이미지 생성 완료!")
        logger.info(f"📁 저장 위치: {metadata['cover_image_path']}")
        logger.info(f"📐 이미지 크기: {metadata['image_size']}")
        
    except Exception as e:
        logger.error(f"\n❌ 오류 발생: {str(e)}")
        import traceback
        traceback.print_exc()
        exit(1)
