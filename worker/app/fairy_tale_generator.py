"""
Once Upon a Time - 한국형 동화 생성 AI 엔진
프로젝트: P-실무 (Gachon University)
버전: v1.0_Production

[기능 설명]
1. 사용자 입력 검증 (Safety & Requirement Check)
2. 통합 프롬프트 재구성 (Prompt Re-engineering)
3. LLM 기반 동화 텍스트 생성
4. 후처리 검증 (Editor AI with Rubric)
5. 피드백 반영 재작성 (Refinement Loop)
6. JSON 포맷팅 및 구조화

[원본 코드 출처]
Colab Notebook: P_실무_동화생성AI__V_9_.ipynb
변환 일시: 2024
"""

import os
import json
import re
import pytz
from datetime import datetime
from openai import OpenAI
from typing import Dict, List, Optional, Tuple


# ==============================================================================
# 설정 및 초기화
# ==============================================================================

class FairyTaleGenerator:
    """한국형 동화 생성 AI 엔진 메인 클래스"""
    
    def __init__(self, api_key: Optional[str] = None):
        """
        초기화
        
        Args:
            api_key: OpenAI API 키. None이면 환경 변수에서 읽음.
        """
        if api_key:
            os.environ["OPENAI_API_KEY"] = api_key
        
        self.client = OpenAI()
        
        # 시대별 오프닝 문구 (원본 로직 유지)
        self.OPENING_PHRASES = {
            "myth": "하늘이 처음 열리고 곰과 호랑이가 소원을 빌던 아주 먼 옛날,",
            "traditional": "호랑이가 곰방대를 물고 뻐끔뻐끔 연기를 내뿜던 시절,",
            "modern_history": "전차 종소리가 땡땡 울리고 신기한 물건이 가득하던 시절,",
            "contemporary": "높은 빌딩이 숲을 이루고 우리가 살고 있는 바로 지금,"
        }
        
        print("✅ 동화 생성 엔진 초기화 완료")
    
    # ==========================================================================
    # Step 1: 입력값 검증 및 분석
    # ==========================================================================
    
    def validate_input(self, user_input: str) -> bool:
        """
        사용자 입력의 안전성과 적합성 판단
        
        Args:
            user_input: 사용자가 입력한 동화 요청 텍스트
            
        Returns:
            True: 검증 통과, False: 부적절한 입력
        """
        print(f"\n🚀 [프로세스 시작] 사용자 입력: \"{user_input}\"")
        print("1️⃣ [검증 단계] 입력값의 안전성과 적합성을 판단 중...")
        
        check_prompt = f"""
        당신은 동화 서비스의 '안전 관리자'입니다.
        사용자의 입력이 동화를 만들기에 적절한지 판단하세요.
        폭력적, 선정적, 혐오 표현이 있거나 동화와 전혀 관련 없는 요청이라면 'FALSE'를,
        적절하다면 'TRUE'를 출력하세요.
        설명 없이 오직 TRUE 또는 FALSE만 대답하세요.

        사용자 입력: {user_input}
        """
        
        check_response = self.client.chat.completions.create(
            model="gpt-4o",
            messages=[
                {"role": "system", "content": "You are a safety moderator."},
                {"role": "user", "content": check_prompt}
            ],
            temperature=0.0
        )
        
        is_safe = check_response.choices[0].message.content.strip()
        
        if "FALSE" in is_safe:
            print("⚠️ [차단됨] 부적절하거나 동화 생성과 관련 없는 요청입니다. 다시 입력해주세요.")
            return False
        
        print("   -> ✅ 검증 통과.")
        return True
    
    # ==========================================================================
    # Step 2: 통합 프롬프트 재구성 (Prompt Re-engineering)
    # ==========================================================================
    
    def create_story_plan(self, user_input: str) -> Optional[Dict]:
        """
        사용자 입력을 분석하여 상세한 '동화 기획서' 생성
        
        Args:
            user_input: 사용자가 입력한 동화 요청 텍스트
            
        Returns:
            동화 기획안 JSON 딕셔너리 또는 None (실패 시)
        """
        print("2️⃣ [기획 단계] 사용자 의도를 분석하여 '통합 프롬프트' 생성 중...")
        
        architect_prompt = f"""
        당신은 '한국형 동화 기획자'입니다.
        사용자 입력을 분석하여 시대 배경을 정하고, 그 시대에 맞지 않는 설정은 **반드시** 해당 시대에 어울리는 개념으로 바꿔야 합니다.

        [필수 수행 절차]
        1. **시대 결정(Era)**: [단군시대, 조선시대, 근대, 현대] 중 택 1
           - 입력에 시대 언급이 없으면, 내용에 가장 잘 어울리는 시대를 고르되 기본은 '현대'입니다.

        2. **시대별 개념 치환 (Context Mapping)**:
           **만약 '현대(Modern)'가 선택되었다면:**
           - 왕, 왕비 -> '시장님', '교장 선생님', '회장님', '부모님'
           - 공주, 왕자 -> '인기 아이돌', '부잣집 친구', '반장', '전학생'
           - 무도회 -> '학예회', '학교 축제', '생일 파티', '장기자랑'
           - 왕국, 성 -> '학교', '우리 동네', '아파트', '놀이공원'
           - 마법 -> '과학 기술', '신기한 도구', '스마트폰', '꿈속 상상' (혹은 현대 판타지로 처리)

           **만약 '조선시대'가 선택되었다면:**
           - 파티 -> '잔치', '연회'
           - 빵/케이크 -> '떡', '다과'
           - 드레스 -> '비단 한복', '당의'

        [사용자 입력]
        {user_input}

        [출력 포맷 - JSON 형식만 출력할 것]
        {{
          "title": "동화 제목",
          "era": "선택한 시대 (단군시대/조선시대/근대/현대)",
          "characters": "주인공과 등장인물 상세 설정 (한국식 이름, 직업, 성격 서술)",
          "setting": "배경 설정 (구체적인 한국 지명 포함)",
          "korean_items": ["이야기에 사용할 한국적 소재1", "소재2", "소재3"],
          "magic_trigger": "변신이나 사건이 발생하는 구체적인 계기 (예: 경복궁에서 주운 신비한 노리개, 남산타워의 소원 자물쇠가 빛나며 등)",
          "tone": "분위기",
          "moral": "교훈",
          "plot_outline": "줄거리 요약"
        }}
        """
        
        plan_response = self.client.chat.completions.create(
            model="gpt-4o",
            messages=[
                {"role": "system", "content": "You are a creative story architect. Output JSON only."},
                {"role": "user", "content": architect_prompt}
            ],
            response_format={"type": "json_object"},
            temperature=0.7
        )
        
        story_plan = json.loads(plan_response.choices[0].message.content)
        
        return story_plan
    
    # ==========================================================================
    # Step 3: 동화 텍스트 생성 (LLM Generation)
    # ==========================================================================
    
    def generate_story_text(self, story_plan: Dict) -> str:
        """
        동화 설계도를 바탕으로 실제 동화 텍스트 생성
        
        Args:
            story_plan: create_story_plan에서 생성된 기획안
            
        Returns:
            생성된 동화 텍스트 (전문)
        """
        # 시대별 오프닝 문구 매핑
        era_from_plan = story_plan.get("era", "")
        
        if "단군" in era_from_plan:
            selected_era_key = "myth"
        elif "조선" in era_from_plan:
            selected_era_key = "traditional"
        elif "근대" in era_from_plan:
            selected_era_key = "modern_history"
        elif "현대" in era_from_plan:
            selected_era_key = "contemporary"
        else:
            selected_era_key = "traditional"  # Default
        
        opening_sentence = self.OPENING_PHRASES.get(
            selected_era_key, 
            self.OPENING_PHRASES["traditional"]
        )
        
        print("\n   📄 [생성된 동화 설계도]")
        print(f"   - 제목: {story_plan.get('title', '제목 미정')}")
        print(f"   - 시대: {selected_era_key} -> 첫 문장: \"{opening_sentence}\"")
        print(f"   - 한국적 소재: {story_plan.get('korean_items')}")
        
        print("\n3️⃣ [생성 단계] 설계도를 바탕으로 GPT-4o가 동화를 집필 중입니다...")
        
        writer_prompt = f"""
        당신은 한국의 정서를 담아 따뜻하고 교훈적인 이야기를 만드는 베스트셀러 동화 작가입니다.
        아래의 [동화 설계도]를 바탕으로 아이들이 읽기 좋은 동화를 작성해 주세요.

        [작성 가이드]
        1. 필수 지침(매우 중요): 이야기는 무조건 다음 문장으로 시작해야 합니다. 다른 서두를 붙이지 마세요.
           -> "{opening_sentence}"

        2. 한국적 요소 활용: 기획된 한국적 소재({story_plan.get('korean_items')})를 이야기에 자연스럽게 녹여내세요.
           (예: 파티 대신 잔치, 빵 대신 떡, 왕자님 대신 도령님 사용)

        3. 문체 및 어조:
           - '~했어요', '~였답니다'와 같은 부드러운 구어체(경어)를 사용하세요.
           - 아이들에게 읽어주는 듯한 다정한 어조를 유지하세요.

        5. 캐릭터 소개 및 문장 구성 주의사항:

        6. 논리적 연결 (인과관계 강화):
           - 문장과 문장 사이가 뚝 끊기지 않게 연결하세요.
           - A사건이 있었기에 B사건이 일어났음을 독자가 알 수 있어야 합니다.
           - 추천 접속 표현: '그러자', '그 때문에', '덕분에', '마침내', '놀랍게도'

        7. 구성과 분량:
           - 기승전결(도입-전개-위기-절정-결말)이 뚜렷해야 합니다.
           - 분량: 낭독 시 약 3~5분 분량 (공백 포함 약 1,200자 ~ 1,500자).

        [동화 설계도]
        {json.dumps(story_plan, ensure_ascii=False)}
        """
        
        story_response = self.client.chat.completions.create(
            model="gpt-4o",
            messages=[
                {"role": "system", "content": "You are a warm and creative fairy tale writer specialized in Korean folklore."},
                {"role": "user", "content": writer_prompt}
            ],
            temperature=0.7
        )
        
        final_story = story_response.choices[0].message.content
        
        return final_story
    
    # ==========================================================================
    # Step 4: 후처리 검증 (Editor AI)
    # ==========================================================================
    
    def verify_story_quality(self, story_text: str, story_plan: Dict) -> Dict:
        """
        생성된 동화의 품질을 루브릭 기반으로 검증
        
        Args:
            story_text: 생성된 동화 텍스트
            story_plan: 동화 기획안
            
        Returns:
            검증 결과 딕셔너리 (scores, reasons, final_decision, rewrite_suggestion)
        """
        print("\n🧐 [검증 단계] '수석 편집장 AI'가 루브릭에 맞춰 원고를 정밀 심사 중입니다...")
        
        target_moral = story_plan.get('moral', '교훈 없음')
        
        verification_prompt = f"""
        당신은 5~8세 아동용 동화 서비스 'Once Upon a Time'의 [수석 품질 검수자]입니다.
        작가(AI)가 작성한 동화 원고를 아래 [검증 루브릭]에 따라 엄격하게 1~5점 척도로 채점하고, 최종 판정을 내리세요.

        [검증 루브릭 (Scoring Rubric)]


        1. 유해성 (Safety) - Critical
        - 기준: 아동에게 정서적 충격, 공포, 혐오감을 주는가?
        - 5점: 부정적 요소 전무, 심리적 안정감을 주는 따뜻한 내용.
        - 4점: 갈등이 있으나 권선징악으로 건전하게 해소됨.
        - 3점: 약간의 거친 표현(예: "멍청이", 가벼운 다툼)이 있으나 맥락상 허용 가능.
        - 2점: [Fail] 따라 할 수 있는 나쁜 행동, 구체적인 공포 묘사 포함.
        - 1점: [Fail] 잔혹성, 선정성, 혐오 표현, 트라우마 유발.

        2. 저작권 (Copyright) - Critical
        - 기준: 기존 유명 IP(디즈니, 해리포터 등)의 고유명사나 설정을 도용했는가?
        - 5점: 고유명사, 플롯, 캐릭터가 완전히 오리지널함.
        - 4점: 전형적 클리셰(왕자, 공주)는 있으나 특정 작품이 연상되지 않음.
        - 3점: 구조가 유명 동화와 흡사하지만 고유명사는 다름.
        - 2점: [Fail] 특정 IP(예: 엘사, 호그와트, 피카츄)를 연상시키는 명칭이나 설정 등장.
        - 1점: [Fail] 기존 작품의 이름, 대사, 스토리를 그대로 베낌.

        3.  품질: 교훈 적합성 (Alignment)
        - 기준: 사용자(부모)가 입력한 [요청 교훈]이 이야기에 잘 녹아있는가?
        - 5점: 교훈이 결말과 완벽히 연결되며 아이가 스스로 깨달을 수 있음.
        - 4점: 교훈이 잘 드러나나, 다소 직접적인 설명 방식임.
        - 3점: 교훈이 포함되어 있으나 이야기 흐름과 약간 겉돔.
        - 2점: [Warning] 교훈이 억지스럽거나 미미함.
        - 1점: [Warning] 요청된 교훈과 무관한 내용.

        4.  품질: 정서 및 표현 (Aesthetics)
        - 기준: 한국적 정서(존댓말)와 아동 눈높이 표현(의성어/의태어)이 적절한가?
        - 5점: 부드러운 '해요체', 풍부한 의성어/의태어, 따뜻한 어조.
        - 4점: 문장이 매끄럽고 이해하기 쉬운 단어 사용.
        - 3점: 문법은 맞으나 딱딱하거나 교과서적인 문체.
        - 2점: [Warning] 번역투 문장, 어려운 한자어 사용.
        - 1점: [Warning] 비문이 많거나 문맥이 끊김.

        [원고 내용]
        {story_text}

        [출력 포맷 - 반드시 JSON 형식만 출력]
        {{
            "scores": {{
                "safety": Integer (1-5),
                "copyright": Integer (1-5),
                "alignment": Integer (1-5),
                "aesthetics": Integer (1-5)
            }},
            "reasons": {{
                "safety": "점수 부여 사유 (한 문장)",
                "copyright": "점수 부여 사유",
                "alignment": "점수 부여 사유",
                "aesthetics": "점수 부여 사유"
            }},
            "final_decision": "PASS" 또는 "FAIL",
            "rewrite_suggestion": "FAIL인 경우, 작가에게 전달할 구체적인 수정 지시사항 (PASS면 null)"
        }}
        ** 주의: 유해성이나 저작권 점수가 3점 미만이면 무조건 'final_decision'은 "FAIL"입니다. **
        """
        
        try:
            response = self.client.chat.completions.create(
                model="gpt-4o",
                messages=[
                    {"role": "system", "content": "You are a strict editor evaluator. Output JSON only."},
                    {"role": "user", "content": verification_prompt}
                ],
                response_format={"type": "json_object"},
                temperature=0.0
            )
            
            result = json.loads(response.choices[0].message.content)
            
            # 로그 출력
            print(f"   -> 판정 결과: {result['final_decision']}")
            print(f"   -> 점수: Safety({result['scores']['safety']}), Copyright({result['scores']['copyright']}), Quality({result['scores']['alignment']}/{result['scores']['aesthetics']})")
            
            return result
            
        except Exception as e:
            print(f"검증 중 오류 발생: {e}")
            return {
                "final_decision": "FAIL", 
                "rewrite_suggestion": "시스템 오류로 검증 실패"
            }
    
    # ==========================================================================
    # Step 5: 피드백 반영 재작성 (Refinement)
    # ==========================================================================
    
    def refine_story(self, previous_story: str, feedback: str, story_plan: Dict) -> str:
        """
        편집장 피드백을 반영하여 동화 재작성
        
        Args:
            previous_story: 이전 버전의 동화 텍스트
            feedback: 편집장의 수정 요청 피드백
            story_plan: 동화 기획안
            
        Returns:
            재작성된 동화 텍스트
        """
        print(f"\n✍️ [수정 단계] 작가 AI가 피드백을 반영하여 원고를 다시 쓰고 있습니다...")
        
        refine_prompt = f"""
        당신은 동화 작가입니다. 초안이 편집 심사에서 반려되었습니다.
        아래 피드백을 반영하여 동화를 다시 작성해주세요.

        [기획 정보]
        제목: {story_plan['title']}
        교훈: {story_plan['moral']}

        [편집장 피드백 (수정 요청)]
        {feedback}

        [기존 원고]
        {previous_story}

        위 내용을 바탕으로 피드백을 해결한 완성된 동화를 다시 써주세요.
        (문체와 분량은 기존 가이드를 따르세요.)
        """
        
        response = self.client.chat.completions.create(
            model="gpt-4o",
            messages=[
                {"role": "system", "content": "You are a professional fairy tale writer."},
                {"role": "user", "content": refine_prompt}
            ],
            temperature=0.7
        )
        
        return response.choices[0].message.content
    
    # ==========================================================================
    # Helper: 텍스트 -> Script Structure 파싱
    # ==========================================================================
    
    def parse_text_to_script(self, full_text: str) -> List[Dict]:
        """
        LLM이 생성한 줄글 텍스트를 (화자, 대사, 감정) 구조로 변환
        
        Args:
            full_text: 생성된 동화 텍스트 전문
            
        Returns:
            스크립트 구조 리스트 (GPT-SoVITS 추론용)
        """
        script_data = []
        lines = full_text.split('\n')
        seq_counter = 1
        
        for line in lines:
            line = line.strip()
            if not line:
                continue
            
            # 화자와 대사 분리 (콜론 기준)
            if ":" in line:
                parts = line.split(":", 1)
                role = parts[0].strip()
                text_content = parts[1].strip()
            else:
                role = "narrator"
                text_content = line
            
            # 감정 추출 (괄호 안의 내용)
            emotion = "neutral"
            emotion_match = re.search(r'\((.*?)\)', text_content)
            if emotion_match:
                emotion = emotion_match.group(1)
                text_content = re.sub(r'\(.*?\)', '', text_content).strip()
            
            script_item = {
                "seq": seq_counter,
                "role": role,
                "text": text_content,
                "emotion": emotion,
                "audio_file_name": f"{seq_counter:03d}_{role}.wav"
            }
            
            script_data.append(script_item)
            seq_counter += 1
        
        return script_data
    
    # ==========================================================================
    # 최종 결과 포맷팅
    # ==========================================================================
    
    def format_final_output(
        self, 
        story_text: str, 
        story_plan: Dict, 
        original_input: Dict
    ) -> Dict:
        """
        최종 JSON 구조 생성
        
        Args:
            story_text: LLM이 생성한 동화 텍스트
            story_plan: LLM이 생성한 기획안
            original_input: 프론트엔드에서 받은 원본 JSON
            
        Returns:
            완성된 JSON 구조 (DB 저장 및 TTS 연동용)
        """
        # 텍스트 파싱
        structured_script = self.parse_text_to_script(story_text)
        
        # 한국 시간
        kst = pytz.timezone('Asia/Seoul')
        now = datetime.now(kst)
        
        # 키워드 추출
        korean_items = story_plan.get('korean_items', [])
        keywords_list = korean_items[:3] if isinstance(korean_items, list) else []
        
        # 태그 구성
        general_tags = [original_input.get('vibe', '일반'), "교훈", "어린이"]
        for k in keywords_list:
            if k not in general_tags:
                general_tags.append(k)
        
        # 최종 JSON 조립
        final_json = {
            "metadata": {
                "project_name": "Once Upon a Time (P-실무)",
                "version": "v1.0_GPT-SoVITS",
                "created_at": now.strftime("%Y-%m-%d %H:%M:%S"),
                "model_type": "gpt_sovits_v2",
                "total_segments": len(structured_script)
            },
            "story_info": {
                # 프론트엔드 입력값 우선 적용
                "title": original_input.get("title", story_plan.get("title", "제목 없음")),
                "theme": original_input.get("theme", "자유 주제"),
                "vibe": original_input.get("vibe", "일반"),
                "original_prompt": original_input.get("prompt", ""),
                
                # AI 생성값 적용
                "target_age": story_plan.get("target_age", "All"),
                "summary": story_plan.get("plot_outline", ""),
                "keywords": keywords_list
            },
            "script": structured_script,
            
            # 하위 호환성 필드
            "content": story_text,
            "verification_status": "Verified",
            "tags": general_tags,
            "category": "창작동화",
            "moral": story_plan.get("moral", ""),
            "characters": story_plan.get("characters", {})
        }
        
        return final_json
    
    # ==========================================================================
    # Helper: 점수표 출력
    # ==========================================================================
    
    def print_score_card(self, attempt: int, result: Dict):
        """검증 결과를 시각적으로 출력"""
        scores = result.get('scores', {})
        reasons = result.get('reasons', {})
        decision = result.get('final_decision', 'UNKNOWN')
        
        print(f"\n📊 [검증 리포트 - 시도 {attempt}회차]")
        print("-" * 50)
        print(f"1. 유해성 (Safety)    : {'★' * scores.get('safety', 0)} ({scores.get('safety', 0)}/5)")
        print(f"2. 저작권 (Copyright) : {'★' * scores.get('copyright', 0)} ({scores.get('copyright', 0)}/5)")
        print(f"3. 교훈성 (Alignment) : {'★' * scores.get('alignment', 0)} ({scores.get('alignment', 0)}/5)")
        print(f"4. 심미성 (Aesthetics): {'★' * scores.get('aesthetics', 0)} ({scores.get('aesthetics', 0)}/5)")
        print("-" * 50)
        print(f"🧐 종합 판정: [{'✅ PASS' if decision == 'PASS' else '❌ FAIL'}]")
        
        if decision == 'FAIL':
            fail_reasons = []
            for key, value in scores.items():
                if value < 3:
                    if key in ['safety', 'copyright', 'alignment', 'aesthetics']:
                        fail_reasons.append(reasons.get(key, ''))
            
            if not fail_reasons:
                fail_reasons.append('기준 미달')
            
            print(f"⚠️ 사유: {', '.join([r for r in fail_reasons if r])}")
        print("=" * 60)
    
    # ==========================================================================
    # 메인 실행기: 전체 프로세스 통합
    # ==========================================================================
    
    def generate_fairy_tale(self, json_data: Dict) -> Optional[Dict]:
        """
        프론트엔드 JSON을 받아 전체 프로세스 실행
        
        Args:
            json_data: {
                "theme": "모험",
                "vibe": "신비로운",
                "prompt": "동대문에서 공주 옷을 찾는 유민이",
                "title": "동대문에 간 공주"
            }
            
        Returns:
            완성된 동화 JSON 또는 None (실패 시)
        """
        # JSON 데이터 파싱
        theme = json_data.get("theme", "자유 주제")
        vibe = json_data.get("vibe", "일반적인")
        user_prompt = json_data.get("prompt", "")
        title = json_data.get("title", "제목 없음")
        
        print(f"📥 [System] 요청 수신: {title} ({theme}, {vibe})")
        
        # 프롬프트 구성
        enriched_prompt = f"""
        [동화 생성 요청]
        1. 제목: {title}
        2. 핵심 주제: {theme}
        3. 이야기 분위기(Tone & Manner): {vibe}
        4. 사용자 입력 줄거리: {user_prompt}

        위 4가지 요소를 모두 반영하여, 아이들이 읽기 좋은 동화를 작성해줘.
        특히 '{vibe}' 분위기를 살려서 서술해줘.
        """
        
        # 입력값 검증
        if not self.validate_input(enriched_prompt):
            return None
        
        # 동화 기획안 생성
        story_plan = self.create_story_plan(enriched_prompt)
        if not story_plan:
            print("❌ [System] 기획안 생성 실패.")
            return None
        
        # 생성 및 검증 루프
        max_retries = 3
        current_story = None
        feedback = None
        
        for attempt in range(1, max_retries + 1):
            # 동화 생성 (초안 또는 재작성)
            if attempt == 1:
                print(f"\n📝 [Step 1] 초안 작성 중... (Attempt {attempt})")
                current_story = self.generate_story_text(story_plan)
                
                print("\n" + "┌" + "─"*58 + "┐")
                print("│  📜 [초안]                                           │")
                print("└" + "─"*58 + "┘")
                print(current_story[:100] + "...")
                print("-" * 60)
            else:
                print(f"\n🔄 [Step 1-Retry] 재작성 중... (Attempt {attempt})")
                current_story = self.refine_story(current_story, feedback, story_plan)
            
            # 검증
            print("\n🔍 [Step 2] 품질 검증 수행 중...")
            verification_result = self.verify_story_quality(current_story, story_plan)
            self.print_score_card(attempt, verification_result)
            
            decision = verification_result.get('final_decision', 'FAIL')
            
            if decision == "PASS":
                print("\n✅ [Step 3] 최종안 확정.")
                
                # 최종 결과 포맷팅
                print("\n" + "="*60)
                print("🎉 [Final Result] 최종 완성")
                print("="*60)
                
                final_json_data = self.format_final_output(
                    current_story, 
                    story_plan, 
                    json_data
                )
                
                return final_json_data
            else:
                feedback = verification_result.get('rewrite_suggestion', '내용 수정 필요')
                if attempt < max_retries:
                    print(f"🚫 [System] 기준 미달 -> 재작성")
                else:
                    print("❌ [System] 수정 한도 초과.")
        
        print("\n⚠️ 최종 생성 실패.")
        return None


# ==============================================================================
# JSON 파일 저장 함수 (Standalone - Colab 호환성 제거)
# ==============================================================================

def save_story_to_json(json_data: Dict, output_dir: str = ".") -> str:
    """
    동화 JSON을 파일로 저장
    
    Args:
        json_data: 저장할 JSON 데이터
        output_dir: 저장 디렉토리 경로
        
    Returns:
        저장된 파일의 경로
    """
    try:
        title = json_data.get("story_info", {}).get("title", "제목 없음")
        if title == "제목 없음":
            title = json_data.get("title", "제목 없음")
        
        # 파일명 안전하게 변환
        safe_title = "".join([c if c.isalnum() else "_" for c in title])
        
        # 한국 시간 타임스탬프
        kst = pytz.timezone('Asia/Seoul')
        timestamp = datetime.now(kst).strftime("%Y%m%d_%H%M")
        
        file_name = f"{safe_title}_{timestamp}.json"
        file_path = os.path.join(output_dir, file_name)
        
        # 파일 쓰기
        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump(json_data, f, ensure_ascii=False, indent=4)
        
        print(f"\n💾 [System] JSON 파일이 저장되었습니다: {file_path}")
        
        return file_path
        
    except Exception as e:
        print(f"❌ 파일 저장 중 오류 발생: {e}")
        return ""


# ==============================================================================
# 스프링부트 연동용 메인 함수
# ==============================================================================

def main_api_handler(input_json_str: str, api_key: str) -> str:
    """
    스프링부트에서 호출할 메인 함수
    
    Args:
        input_json_str: 프론트엔드 요청 JSON 문자열
        api_key: OpenAI API 키
        
    Returns:
        생성된 동화 JSON 문자열
    """
    try:
        # JSON 파싱
        input_data = json.loads(input_json_str)
        
        # 생성기 초기화
        generator = FairyTaleGenerator(api_key=api_key)
        
        # 동화 생성
        result = generator.generate_fairy_tale(input_data)
        
        if result:
            wrapped_result = {
                "success": True,
                "data": result
            }
            return json.dumps(wrapped_result, ensure_ascii=False, indent=2)

        error_result = {
            "success": False,
            "error": "동화 생성 실패",
            "message": "최대 재시도 횟수 초과 또는 입력값 검증 실패"
        }
        return json.dumps(error_result, ensure_ascii=False, indent=2)
            
    except Exception as e:
        error_result = {
            "success": False,
            "error": "처리 중 오류 발생",
            "message": str(e)
        }
        return json.dumps(error_result, ensure_ascii=False, indent=2)


# ==============================================================================
# 테스트 코드
# ==============================================================================

if __name__ == "__main__":
    import sys
    
    # stdin 모드 확인 (스프링부트 연동용)
    if not sys.stdin.isatty():
        # stdin에서 JSON 읽기
        input_json_str = sys.stdin.read()
        api_key = os.getenv("OPENAI_API_KEY")
        
        if not api_key:
            error_result = {
                "error": "API 키 오류",
                "message": "OPENAI_API_KEY 환경 변수가 설정되지 않았습니다."
            }
            print(json.dumps(error_result, ensure_ascii=False, indent=2))
            sys.exit(1)
        
        # 동화 생성 및 결과 반환
        result_json = main_api_handler(input_json_str, api_key)
        
        # stdout으로 결과 출력 (스프링부트가 읽음)
        print(result_json)
    
    else:
        # 일반 테스트 모드
        test_json_input = {
            "theme": "모험",
            "vibe": "신비로운",
            "prompt": "동대문에서 공주 옷을 찾는 유민이",
            "title": "동대문에 간 공주"
        }
        
        print("=" * 60)
        print("Once Upon a Time - 동화 생성 AI 테스트")
        print("=" * 60)
        
        # API 키 설정 (환경 변수에서 읽음)
        api_key = os.getenv("OPENAI_API_KEY")
        
        if not api_key:
            print("⚠️ OPENAI_API_KEY 환경 변수를 설정해주세요.")
            exit(1)
        
        # 생성기 초기화
        generator = FairyTaleGenerator(api_key=api_key)
        
        # 동화 생성
        result = generator.generate_fairy_tale(test_json_input)
        
        if result:
            # 파일 저장
            output_path = save_story_to_json(result, output_dir=".")
            print(f"\n✅ 테스트 완료! 파일 저장 경로: {output_path}")
        else:
            print("\n❌ 테스트 실패.")
