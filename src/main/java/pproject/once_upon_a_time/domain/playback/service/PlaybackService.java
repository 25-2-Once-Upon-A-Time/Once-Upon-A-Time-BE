package pproject.once_upon_a_time.domain.playback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.audiobook.domain.AudioBook;
import pproject.once_upon_a_time.domain.audiobook.dto.PlaybackFinishRequest;
import pproject.once_upon_a_time.domain.audiobook.dto.PlaybackInfoResponse;
import pproject.once_upon_a_time.domain.audiobook.dto.PlaybackProgressRequest;
import pproject.once_upon_a_time.domain.audiobook.dto.PlaybackStartResponse;
import pproject.once_upon_a_time.domain.audiobook.repository.AudioBookRepository;
import pproject.once_upon_a_time.domain.character.domain.Character;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.playback.domain.AudioBookPlayback;
import pproject.once_upon_a_time.domain.playback.domain.PlaybackStatus;
import pproject.once_upon_a_time.domain.playback.infrastructure.PlaybackRedisRepository;
import pproject.once_upon_a_time.domain.playback.repository.AudioBookPlaybackRepository;
import pproject.once_upon_a_time.domain.story.domain.Story;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaybackService {

    private final AudioBookPlaybackRepository playbackRepository;
    private final AudioBookRepository audioBookRepository;
    private final PlaybackRedisRepository playbackRedisRepository;

    // 재생 정보 조회: Redis 값이 있으면 우선 적용
    @Transactional(readOnly = true)
    public PlaybackInfoResponse getInfo(Long audiobookId, Member member) {
        Long memberId = validateMember(member);

        AudioBook audioBook = audioBookRepository.findById(audiobookId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUDIOBOOK_NOT_FOUND));

        AudioBookPlayback playback = playbackRepository.findByAudioBook_IdAndMemberId_Id(audiobookId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAYBACK_NOT_FOUND));

        Story story = audioBook.getStory();
        if (story == null) {
            throw new CustomException(ErrorCode.AUDIOBOOK_STORY_NOT_FOUND);
        }

        Character character = audioBook.getCharacter();
        if (character == null) {
            throw new CustomException(ErrorCode.AUDIOBOOK_CHARACTER_NOT_FOUND);
        }

        Integer lastPosition = playback.getLastPosition();
        PlaybackStatus status = playback.getStatus();

        try {
            Optional<PlaybackRedisRepository.PlaybackRedisValue> redisValOpt = playbackRedisRepository.readPlayback(memberId, audiobookId);
            if (redisValOpt.isPresent()) {
                PlaybackRedisRepository.PlaybackRedisValue redisVal = redisValOpt.get();
                if (redisVal.getLastPosition() != null) {
                    lastPosition = redisVal.getLastPosition();
                }
                if (redisVal.getStatus() != null) {
                    status = redisVal.getStatus();
                }
            }
        } catch (DataAccessException ex) {
            log.warn("Redis 조회 불가, DB 값으로 반환합니다. audiobookId={}, memberId={}", audiobookId, memberId, ex);
        }

        float progressRate = calculateProgressRate(audioBook.getDuration(), lastPosition);

        return PlaybackInfoResponse.builder()
                .playbackId(playback.getId())
                .audiobookId(audiobookId)
                .duration(audioBook.getDuration())
                .lastPosition(lastPosition)
                .progressRate(progressRate)
                .status(status)
                .storyTitle(story.getTitle())
                .theme(story.getTheme())
                .vibe(story.getVibe())
                .thumbnailUrl(story.getThumbnailUrl())
                .characterName(character.getCharacterName())
                .build();
    }

    // 재생 시작: DB 조회/생성 후 Redis 초기화 및 응답 반환
    @Transactional
    public PlaybackStartResponse start(Long audiobookId, Member member) {
        Long memberId = validateMember(member);

        AudioBook audioBook = audioBookRepository.findById(audiobookId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUDIOBOOK_NOT_FOUND));

        Optional<AudioBookPlayback> existingPlayback = playbackRepository.findByAudioBook_IdAndMemberId_Id(audiobookId, memberId);
        int lastPosition = existingPlayback.map(p -> Optional.ofNullable(p.getLastPosition()).orElse(0)).orElse(0);
        float progressRate = calculateProgressRate(audioBook.getDuration(), lastPosition);

        AudioBookPlayback playback;
        if (existingPlayback.isEmpty()) {
            playback = AudioBookPlayback.builder()
                    .audioBook(audioBook)
                    .memberId(member)
                    .lastPosition(lastPosition)
                    .listenedDuration(lastPosition)
                    .progressRate(progressRate)
                    .startedAt(LocalDateTime.now())
                    .status(PlaybackStatus.PLAYING)
                    .build();
            playbackRepository.save(playback);
        } else {
            playback = existingPlayback.get();
            playbackRepository.updateProgress(audiobookId, memberId, lastPosition, progressRate, lastPosition, PlaybackStatus.PLAYING);
        }

        savePlaybackToRedis(memberId, audiobookId, lastPosition, PlaybackStatus.PLAYING, LocalDateTime.now());

        return PlaybackStartResponse.builder()
                .playbackId(playback.getId())
                .audiobookId(audiobookId)
                .lastPosition(lastPosition)
                .progressRate(progressRate)
                .status(PlaybackStatus.PLAYING)
                .audioUrl(audioBook.getAudioUrl())
                .build();
    }

    // 진행도 업데이트: 요청 위치/상태를 Redis에만 반영
    @Transactional(readOnly = true)
    public void updateProgress(Long audiobookId, Member member, PlaybackProgressRequest request) {
        Long memberId = validateMember(member);
        if (request == null || request.getCurrentTime() == null || request.getCurrentTime() < 0) {
            throw new CustomException(ErrorCode.INVALID_PROGRESS);
        }
        validateStatus(request.getStatus());
        PlaybackStatus status = Optional.ofNullable(request.getStatus()).orElse(PlaybackStatus.PLAYING);
        savePlaybackToRedis(memberId, audiobookId, request.getCurrentTime(), status, LocalDateTime.now());
    }

    // 재생 완료: Redis/요청값을 기반으로 DB 최종 반영 후 Redis 정리
    @Transactional
    public void finish(Long audiobookId, Member member, PlaybackFinishRequest request) {
        Long memberId = validateMember(member);

        AudioBook audioBook = audioBookRepository.findById(audiobookId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUDIOBOOK_NOT_FOUND));

        AudioBookPlayback playback = playbackRepository.findByAudioBook_IdAndMemberId_Id(audiobookId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAYBACK_NOT_FOUND));

        if (PlaybackStatus.COMPLETED.equals(playback.getStatus())) {
            log.info("이미 완료된 플레이백에 대한 finish 호출입니다. audiobookId={}, memberId={}", audiobookId, memberId);
            safeDeleteRedisKey(memberId, audiobookId);
            return;
        }

        PlaybackRedisRepository.PlaybackRedisValue redisValue = null;
        try {
            redisValue = playbackRedisRepository.readPlayback(memberId, audiobookId).orElse(null);
        } catch (DataAccessException e) {
            log.warn("finish 처리 중 Redis 사용 불가, DB 값으로 진행합니다. audiobookId={}, memberId={}", audiobookId, memberId, e);
        }

        Integer finalPosition = chooseFinalPosition(
                redisValue == null ? null : redisValue.getLastPosition(),
                request == null ? null : request.getFinalPosition());
        PlaybackStatus status = chooseStatus(redisValue, request);

        float progressRate = calculateProgressRate(audioBook.getDuration(), finalPosition);
        int listenedDuration = finalPosition == null ? 0 : finalPosition;
        int updated = playbackRepository.finishPlayback(audiobookId, memberId, finalPosition, progressRate, listenedDuration, status, LocalDateTime.now());
        if (updated == 0) {
            throw new CustomException(ErrorCode.PLAYBACK_NOT_FOUND);
        }

        safeDeleteRedisKey(memberId, audiobookId);
    }

    private void savePlaybackToRedis(Long memberId, Long audiobookId, Integer lastPosition, PlaybackStatus status, LocalDateTime updatedAt) {
        try {
            playbackRedisRepository.savePlayback(memberId, audiobookId, lastPosition, status, updatedAt);
        } catch (DataAccessException e) {
            log.warn("Redis 저장 실패(무시). audiobookId={}, memberId={}", audiobookId, memberId, e);
        }
    }

    // 회원 유효성 및 ID 추출
    private Long validateMember(Member member) {
        if (member == null) {
            throw new CustomException(ErrorCode.INVALID_LOGIN);
        }
        Long memberId = member.getId();
        if (memberId == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return memberId;
    }

    // 진행률 계산 (duration 기반)
    private float calculateProgressRate(Double duration, Integer position) {
        if (duration == null || duration <= 0) {
            return 0f;
        }
        int safePosition = Optional.ofNullable(position).orElse(0);
        float rate = (float) (safePosition / duration);
        return Math.min(1.0f, Math.max(0f, rate));
    }

    // int 입력용 진행률 계산
    private float calculateProgressRate(Double duration, int position) {
        return calculateProgressRate(duration, Integer.valueOf(position));
    }

    // Redis/요청값 중 최종 재생 위치 결정 (요청값을 우선, 없으면 Redis 값 사용)
    private Integer chooseFinalPosition(Integer redisPosition, Integer requestPosition) {
        Integer finalPosition = requestPosition != null ? requestPosition : redisPosition;
        if (redisPosition == null && requestPosition == null) {
            throw new CustomException(ErrorCode.REDIS_PLAYBACK_NOT_FOUND);
        }
        if (finalPosition == null || finalPosition < 0) {
            throw new CustomException(ErrorCode.INVALID_PROGRESS);
        }
        return finalPosition;
    }

    // Redis/요청값 중 최종 상태 결정 (기본 COMPLETED)
    private PlaybackStatus chooseStatus(PlaybackRedisRepository.PlaybackRedisValue redisValue, PlaybackFinishRequest request) {
        PlaybackStatus fromRequest = request != null ? request.getStatus() : null;
        PlaybackStatus fromRedis = redisValue != null ? redisValue.getStatus() : null;
        PlaybackStatus status = fromRequest != null ? fromRequest : fromRedis;
        return status != null ? status : PlaybackStatus.COMPLETED;
    }

    // 요청 상태 값 검증 (Enum 범위 밖이면 400)
    private void validateStatus(PlaybackStatus status) {
        if (status == null) {
            return; // null은 기본값 사용
        }
    }

    // Redis 삭제 실패 시 예외 없이 경고만 남김
    private void safeDeleteRedisKey(Long memberId, Long audiobookId) {
        try {
            playbackRedisRepository.deletePlaybackRedisKey(memberId, audiobookId);
        } catch (DataAccessException e) {
            log.warn("Redis 삭제 실패(무시). audiobookId={}, memberId={}", audiobookId, memberId, e);
        }
    }
}
