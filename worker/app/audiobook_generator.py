"""
Once Upon a Time - TTS 오디오북 생성 엔진
프로젝트: P-실무 (Gachon University)
버전: v1.0_Production

[기능 설명]
1. GPT-SoVITS v3 한국어 TTS 모델 초기화
2. 동화 JSON 스크립트 기반 오디오 생성
3. 캐릭터별 음성 합성
4. 메타데이터 생성 및 관리
5. 한국 시간대(KST) 지원

[원본 코드 출처]
Colab Notebook: audiobook_generator_v3_cleaned.ipynb
변환 일시: 2024-12-12
"""

import os
import sys
import json
import shutil
import numpy as np
import soundfile as sf
from pathlib import Path
from datetime import datetime, timedelta, timezone
from typing import Dict, List, Optional, Tuple
import logging

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


# ==============================================================================
# 환경 및 유틸리티 함수
# ==============================================================================

def get_kst_now():
    """한국 표준시(KST, UTC+9) 현재 시간 반환"""
    return datetime.now(timezone(timedelta(hours=9)))


def setup_python_paths(gpt_sovits_root: str = "/content/GPT-SoVITS"):
    """
    GPT-SoVITS 모듈 경로를 Python path에 추가
    
    Args:
        gpt_sovits_root: GPT-SoVITS 저장소 루트 경로
    """
    paths_to_add = [
        gpt_sovits_root,
        os.path.join(gpt_sovits_root, "GPT_SoVITS"),
        os.path.join(gpt_sovits_root, "GPT_SoVITS", "eres2net")
    ]
    
    for path in paths_to_add:
        if path not in sys.path:
            sys.path.insert(0, path)
    
    logger.info(f"✅ Python 경로 설정 완료: {len(paths_to_add)}개")


def clear_tts_module_cache():
    """
    TTS 관련 모듈 캐시 초기화
    모델 재로드 시 필요
    """
    modules_to_clear = []
    for module in list(sys.modules.keys()):
        if 'TTS_infer_pack' in module or 'BigVGAN' in module:
            modules_to_clear.append(module)
            del sys.modules[module]
    
    if modules_to_clear:
        logger.info(f"🧹 모듈 캐시 초기화: {len(modules_to_clear)}개")


# ==============================================================================
# BigVGAN 경로 수정 유틸리티
# ==============================================================================

def fix_bigvgan_path(gpt_sovits_root: str = "/content/GPT-SoVITS") -> bool:
    """
    TTS.py의 BigVGAN 경로를 절대 경로로 수정
    
    Args:
        gpt_sovits_root: GPT-SoVITS 저장소 루트 경로
        
    Returns:
        True: 수정 성공 또는 이미 수정됨
        False: 수정 실패
    """
    import re
    
    logger.info("🔧 BigVGAN 경로 자동 수정 시작...")
    
    tts_file = os.path.join(
        gpt_sovits_root, 
        "GPT_SoVITS", 
        "TTS_infer_pack", 
        "TTS.py"
    )
    
    if not os.path.exists(tts_file):
        logger.error(f"❌ TTS.py 파일을 찾을 수 없습니다: {tts_file}")
        return False
    
    # 파일 읽기
    with open(tts_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    modified = False
    
    # BigVGAN 절대 경로
    bigvgan_abs_path = os.path.join(
        gpt_sovits_root,
        "GPT_SoVITS",
        "pretrained_models",
        "models--nvidia--bigvgan_v2_24khz_100band_256x"
    )
    
    # 수정 1: now_dir 기반 경로를 절대 경로로
    pattern1 = r'"%s/GPT_SoVITS/pretrained_models/models--nvidia--bigvgan_v2_24khz_100band_256x" % \(now_dir,?\)'
    replacement1 = f'"{bigvgan_abs_path}"'
    
    if re.search(pattern1, content):
        content = re.sub(pattern1, replacement1, content)
        logger.info("  ✅ now_dir 기반 경로 → 절대 경로로 수정")
        modified = True
    
    # 수정 2: 상대 경로를 절대 경로로
    pattern2 = r'"pretrained_models/models--nvidia--bigvgan_v2_24khz_100band_256x"'
    replacement2 = f'"{bigvgan_abs_path}"'
    
    if re.search(pattern2, content) and replacement2 not in content:
        content = re.sub(pattern2, replacement2, content)
        logger.info("  ✅ 상대 경로 → 절대 경로로 수정")
        modified = True
    
    # 수정 3: state_dict 로드 라인 주석 처리
    if 'print("loading vocoder", self.vocoder.load_state_dict(state_dict_g))' in content:
        content = content.replace(
            'print("loading vocoder", self.vocoder.load_state_dict(state_dict_g))',
            '# print("loading vocoder", self.vocoder.load_state_dict(state_dict_g))'
        )
        logger.info("  ✅ state_dict 로드 라인 주석 처리")
        modified = True
    
    if modified:
        # 백업 생성
        backup_file = tts_file + ".backup"
        if not os.path.exists(backup_file):
            shutil.copy2(tts_file, backup_file)
        
        # 파일 저장
        with open(tts_file, 'w', encoding='utf-8') as f:
            f.write(content)
        
        logger.info("✅ TTS.py 수정 완료!")
        return True
    else:
        logger.info("ℹ️ 수정할 내용이 없습니다 (이미 수정됨)")
        return True


# ==============================================================================
# 캐릭터 음성 데이터 관리
# ==============================================================================

class CharacterVoiceManager:
    """캐릭터별 음성 참조 데이터 관리"""
    
    # 12개 캐릭터 매핑 (원본 유지)
    CHARACTER_MAPPING = {
        # 남성 캐릭터
        "boy": "M0101_명량한 소년",
        "scholar": "M0202_선비",
        "nolbu": "M0303_놀부",
        "king": "M0404_왕",
        "commander": "M0505_지휘관",
        "bachelor": "M0606_총각",
        
        # 여성 캐릭터
        "teacher": "W0107_선생님",
        "librarian": "W0208_사서",
        "queen": "W0309_여왕",
        "girl": "W0410_소녀",
        "grandma": "W0511_할머니",
        "princess": "W0612_공주"
    }
    
    # 각 캐릭터의 참조 텍스트 (원본 유지)
    REFERENCE_TEXTS = {
        "boy": "나는 씩씩한 소년이야!",
        "scholar": "학문의 길은 멀고도 험하도다.",
        "nolbu": "이놈, 감히 형님한테 대들어!",
        "king": "짐이 명하노니, 모두 물러가거라.",
        "commander": "전군, 집결하라!",
        "bachelor": "저는 총각입니다.",
        "teacher": "자, 이제 공부를 시작해볼까요?",
        "librarian": "조용히 하세요. 여기는 도서관입니다.",
        "queen": "나는 이 나라의 여왕이니라.",
        "girl": "엄마, 나 배고파!",
        "grandma": "옛날 옛날에 말이야.",
        "princess": "저는 아름다운 공주랍니다."
    }
    
    def __init__(self, dataset_path: str = "/content/GPT-SoVITS/sliced"):
        """
        초기화
        
        Args:
            dataset_path: 슬라이싱된 음성 데이터 경로
        """
        self.dataset_path = dataset_path
        self.reference_info = {}
        self._load_references()
    
    def _load_references(self):
        """각 캐릭터의 참조 오디오 정보 로드"""
        for character in self.CHARACTER_MAPPING.keys():
            char_dir = os.path.join(self.dataset_path, character)
            
            if not os.path.exists(char_dir):
                logger.warning(f"⚠️ {character} 폴더 없음: {char_dir}")
                continue
            
            wav_files = sorted(list(Path(char_dir).glob("*.wav")))
            
            if wav_files:
                self.reference_info[character] = {
                    "ref_audio_path": str(wav_files[0]),
                    "ref_text": self.REFERENCE_TEXTS.get(character, "안녕하세요"),
                    "voice_type": self.CHARACTER_MAPPING[character],
                    "total_files": len(wav_files)
                }
                logger.debug(f"  ✓ {character}: {wav_files[0].name}")
    
    def get_reference(self, character: str) -> Optional[Dict]:
        """
        캐릭터의 참조 오디오 정보 반환
        
        Args:
            character: 캐릭터 이름
            
        Returns:
            참조 정보 딕셔너리 또는 None
        """
        return self.reference_info.get(character)
    
    def get_default_reference(self) -> Dict:
        """
        기본 참조 오디오 정보 반환 (narrator용)
        
        Returns:
            기본 참조 정보 딕셔너리
        """
        # teacher를 기본 narrator로 사용
        if "teacher" in self.reference_info:
            return self.reference_info["teacher"]
        
        # teacher가 없으면 첫 번째 참조 사용
        if self.reference_info:
            return list(self.reference_info.values())[0]
        
        # 아무것도 없으면 에러
        raise ValueError("참조 오디오가 하나도 없습니다. 데이터를 준비하세요.")


# ==============================================================================
# TTS 오디오북 생성기
# ==============================================================================

class AudiobookGenerator:
    """GPT-SoVITS v3 기반 오디오북 생성기"""
    
    def __init__(
        self,
        gpt_sovits_root: str = "/content/GPT-SoVITS",
        dataset_path: str = "/content/GPT-SoVITS/sliced",
        output_base: str = "/content/audiobook_outputs"
    ):
        """
        초기화
        
        Args:
            gpt_sovits_root: GPT-SoVITS 저장소 루트 경로
            dataset_path: 캐릭터 음성 데이터 경로
            output_base: 오디오북 출력 기본 경로
        """
        self.gpt_sovits_root = gpt_sovits_root
        self.dataset_path = dataset_path
        self.output_base = output_base
        
        # 캐릭터 음성 관리자
        self.voice_manager = CharacterVoiceManager(dataset_path)
        
        # TTS 파이프라인 (나중에 초기화)
        self.tts_pipeline = None
        
        logger.info("✅ AudiobookGenerator 초기화 완료")
    
    def initialize_tts(self, auto_fix_bigvgan: bool = True):
        """
        TTS 파이프라인 초기화
        
        Args:
            auto_fix_bigvgan: BigVGAN 경로 자동 수정 여부
        """
        logger.info("🎙️ TTS 파이프라인 초기화 중...")
        
        # Python 경로 설정
        setup_python_paths(self.gpt_sovits_root)
        
        # BigVGAN 경로 수정
        if auto_fix_bigvgan:
            fix_bigvgan_path(self.gpt_sovits_root)
        
        # 작업 디렉토리 변경
        os.chdir(os.path.join(self.gpt_sovits_root, "GPT_SoVITS"))
        
        # 모듈 캐시 초기화
        clear_tts_module_cache()
        
        # TTS 모듈 임포트
        from TTS_infer_pack.TTS import TTS, TTS_Config
        
        # v3 모델 설정 (원본 경로 유지)
        tts_config = TTS_Config()
        tts_config.t2s_weights_path = os.path.join(
            self.gpt_sovits_root,
            "GPT_SoVITS/pretrained_models/s1v3.ckpt"
        )
        tts_config.vits_weights_path = os.path.join(
            self.gpt_sovits_root,
            "GPT_SoVITS/pretrained_models/s2Gv3.pth"
        )
        tts_config.bert_base_path = os.path.join(
            self.gpt_sovits_root,
            "GPT_SoVITS/pretrained_models/chinese-roberta-wwm-ext-large"
        )
        tts_config.cnhuhbert_base_path = os.path.join(
            self.gpt_sovits_root,
            "GPT_SoVITS/pretrained_models/chinese-hubert-base"
        )
        tts_config.device = "cuda"
        
        # TTS 파이프라인 생성
        self.tts_pipeline = TTS(tts_config)
        
        logger.info("✅ TTS 파이프라인 초기화 완료!")
    
    def generate_audiobook(
        self,
        story_json: Dict,
        output_dir: Optional[str] = None,
        use_kst_time: bool = True
    ) -> Dict:
        """
        동화 JSON으로부터 오디오북 생성
        
        Args:
            story_json: 동화 스크립트 JSON 데이터
            output_dir: 출력 디렉토리 (None이면 자동 생성)
            use_kst_time: 한국 시간 사용 여부
            
        Returns:
            생성 결과 메타데이터
        """
        if self.tts_pipeline is None:
            raise RuntimeError("TTS 파이프라인이 초기화되지 않았습니다. initialize_tts()를 먼저 호출하세요.")
        
        logger.info(f"📖 동화 제목: {story_json.get('title', '제목 없음')}")
        
        # 스크립트 추출
        script = story_json.get('script', [])
        if not script:
            # story_info에서 script 찾기 (호환성)
            script = story_json.get('story_info', {}).get('script', [])
        
        logger.info(f"📝 총 {len(script)}개 세그먼트")
        
        # 출력 디렉토리 생성
        if output_dir is None:
            output_dir = self._create_output_directory(story_json, use_kst_time)
        else:
            Path(output_dir).mkdir(parents=True, exist_ok=True)
        
        logger.info(f"📁 출력 디렉토리: {output_dir}")
        
        # 오디오 생성
        results = self._generate_audio_files(script, output_dir)
        
        # 메타데이터 저장
        metadata = self._create_metadata(
            story_json,
            output_dir,
            results,
            use_kst_time
        )
        
        metadata_file = os.path.join(output_dir, "metadata.json")
        with open(metadata_file, 'w', encoding='utf-8') as f:
            json.dump(metadata, f, ensure_ascii=False, indent=2)
        
        logger.info(f"💾 메타데이터 저장: {metadata_file}")
        
        # 결과 출력
        self._print_summary(metadata)
        
        return metadata
    
    def _create_output_directory(
        self,
        story_json: Dict,
        use_kst_time: bool
    ) -> str:
        """출력 디렉토리 경로 생성"""
        # 제목 추출
        title = story_json.get('title', story_json.get('story_info', {}).get('title', '제목없음'))
        safe_title = title.replace(" ", "_").replace("/", "_")
        
        # 타임스탬프
        if use_kst_time:
            now = get_kst_now()
            timestamp = now.strftime("%Y%m%d_%H%M")
        else:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M")
        
        # 디렉토리 이름
        dir_name = f"{safe_title}_{timestamp}"
        output_dir = os.path.join(self.output_base, dir_name)
        
        Path(output_dir).mkdir(parents=True, exist_ok=True)
        
        return output_dir
    
    def _generate_audio_files(
        self,
        script: List[Dict],
        output_dir: str
    ) -> Dict:
        """
        스크립트의 각 세그먼트에 대해 오디오 생성
        
        Args:
            script: 스크립트 리스트
            output_dir: 출력 디렉토리
            
        Returns:
            생성 결과 딕셔너리
        """
        logger.info("🎬 오디오북 생성 시작...")
        logger.info("=" * 60)
        
        generated_files = []
        failed_segments = []
        total_duration = 0
        
        # 기본 참조 오디오 (narrator용)
        default_ref = self.voice_manager.get_default_reference()
        default_ref_audio = default_ref["ref_audio_path"]
        default_ref_text = default_ref["ref_text"]
        
        for segment in script:
            seq = segment.get('seq', 0)
            text = segment.get('text', '')
            audio_filename = segment.get('audio_file_name', f'{seq:03d}.wav')
            character = segment.get('character', segment.get('role', 'narrator'))
            
            # 진행 상황 표시
            logger.info(f"\n[{seq}/{len(script)}] {character}")
            logger.info(f"📝 {text[:60]}{'...' if len(text) > 60 else ''}")
            
            try:
                # 캐릭터별 참조 오디오 선택
                char_ref = self.voice_manager.get_reference(character)
                if char_ref:
                    ref_audio = char_ref["ref_audio_path"]
                    ref_text = char_ref["ref_text"]
                else:
                    ref_audio = default_ref_audio
                    ref_text = default_ref_text
                
                # TTS 입력 구성
                inputs = {
                    "text": text,
                    "text_lang": "ko",
                    "ref_audio_path": ref_audio,
                    "prompt_text": ref_text,
                    "prompt_lang": "ko",
                    "streaming_mode": False,
                    "return_fragment": False
                }
                
                # TTS 실행
                result_gen = self.tts_pipeline.run(inputs)
                
                # 결과 처리
                for result in result_gen:
                    if isinstance(result, tuple) and len(result) == 2:
                        sr, audio_data = result
                        
                        # 저장
                        output_path = os.path.join(output_dir, audio_filename)
                        sf.write(output_path, audio_data, sr)
                        
                        duration = len(audio_data) / sr
                        total_duration += duration
                        
                        generated_files.append({
                            'seq': seq,
                            'filename': audio_filename,
                            'path': output_path,
                            'duration': duration,
                            'character': character,
                            'text': text
                        })
                        
                        logger.info(f"✅ 생성 완료 ({duration:.1f}초)")
                        break
                
            except Exception as e:
                error_msg = str(e)
                logger.error(f"❌ 오류: {error_msg[:100]}")
                failed_segments.append({
                    'seq': seq,
                    'filename': audio_filename,
                    'character': character,
                    'text': text,
                    'error': error_msg
                })
        
        return {
            'generated_files': generated_files,
            'failed_segments': failed_segments,
            'total_duration': total_duration
        }
    
    def _create_metadata(
        self,
        story_json: Dict,
        output_dir: str,
        results: Dict,
        use_kst_time: bool
    ) -> Dict:
        """메타데이터 생성"""
        if use_kst_time:
            now = get_kst_now()
            timezone_str = "Asia/Seoul (UTC+9)"
            time_str = now.strftime('%Y-%m-%d %H:%M:%S KST')
        else:
            now = datetime.now()
            timezone_str = "Local"
            time_str = now.strftime('%Y-%m-%d %H:%M:%S')
        
        # 사용된 캐릭터 추출
        used_characters = set()
        for item in results['generated_files']:
            used_characters.add(item['character'])
        
        metadata = {
            "title": story_json.get('title', story_json.get('story_info', {}).get('title', '제목 없음')),
            "generated_at": now.isoformat(),
            "generated_at_display": time_str,
            "timezone": timezone_str,
            "total_segments": len(story_json.get('script', [])),
            "successful_segments": len(results['generated_files']),
            "failed_segments": len(results['failed_segments']),
            "total_duration_seconds": results['total_duration'],
            "total_duration_minutes": results['total_duration'] / 60,
            "used_characters": sorted(list(used_characters)),
            "output_directory": output_dir,
            "files": results['generated_files'],
            "failures": results['failed_segments']
        }
        
        return metadata
    
    def _print_summary(self, metadata: Dict):
        """생성 결과 요약 출력"""
        logger.info("\n" + "=" * 60)
        logger.info("📊 생성 결과 요약")
        logger.info("=" * 60)
        
        logger.info(f"\n✅ 성공: {metadata['successful_segments']}개 / {metadata['total_segments']}개")
        logger.info(f"❌ 실패: {metadata['failed_segments']}개")
        logger.info(f"⏱️  총 오디오 길이: {metadata['total_duration_seconds']:.1f}초 ({metadata['total_duration_minutes']:.1f}분)")
        logger.info(f"📁 저장 위치: {metadata['output_directory']}")
        logger.info(f"🎭 사용된 캐릭터: {', '.join(metadata['used_characters'])}")


# ==============================================================================
# 스프링부트 연동용 메인 함수
# ==============================================================================

def generate_audiobook_from_json_file(
    json_file_path: str,
    output_dir: Optional[str] = None,
    gpt_sovits_root: str = "/content/GPT-SoVITS",
    dataset_path: str = "/content/GPT-SoVITS/sliced"
) -> Dict:
    """
    JSON 파일로부터 오디오북 생성 (스프링부트 연동용)
    
    Args:
        json_file_path: 동화 JSON 파일 경로
        output_dir: 출력 디렉토리 (None이면 자동 생성)
        gpt_sovits_root: GPT-SoVITS 루트 경로
        dataset_path: 캐릭터 음성 데이터 경로
        
    Returns:
        생성 결과 메타데이터
    """
    # JSON 로드
    with open(json_file_path, 'r', encoding='utf-8') as f:
        story_json = json.load(f)
    
    # 생성기 초기화
    generator = AudiobookGenerator(
        gpt_sovits_root=gpt_sovits_root,
        dataset_path=dataset_path
    )
    
    # TTS 초기화
    generator.initialize_tts(auto_fix_bigvgan=True)
    
    # 오디오북 생성
    metadata = generator.generate_audiobook(
        story_json=story_json,
        output_dir=output_dir
    )
    
    return metadata


# ==============================================================================
# 테스트 코드
# ==============================================================================

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description="TTS 오디오북 생성기")
    parser.add_argument("json_file", help="동화 JSON 파일 경로")
    parser.add_argument("--output", help="출력 디렉토리", default=None)
    parser.add_argument("--gpt-sovits-root", help="GPT-SoVITS 루트 경로", 
                       default="/content/GPT-SoVITS")
    parser.add_argument("--dataset-path", help="캐릭터 음성 데이터 경로",
                       default="/content/GPT-SoVITS/sliced")
    
    args = parser.parse_args()
    
    logger.info("=" * 60)
    logger.info("Once Upon a Time - TTS 오디오북 생성")
    logger.info("=" * 60)
    
    try:
        metadata = generate_audiobook_from_json_file(
            json_file_path=args.json_file,
            output_dir=args.output,
            gpt_sovits_root=args.gpt_sovits_root,
            dataset_path=args.dataset_path
        )
        
        logger.info("\n✅ 오디오북 생성 완료!")
        logger.info(f"📁 출력 위치: {metadata['output_directory']}")
        
    except Exception as e:
        logger.error(f"\n❌ 오류 발생: {str(e)}")
        import traceback
        traceback.print_exc()
        exit(1)
