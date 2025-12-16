package pproject.once_upon_a_time.domain.playback.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.audiobook.domain.AudioBook;
import pproject.once_upon_a_time.domain.audiobook.repository.AudioBookRepository;
import pproject.once_upon_a_time.domain.playback.domain.PlaybackStatus;
import pproject.once_upon_a_time.domain.playback.infrastructure.PlaybackRedisRepository;
import pproject.once_upon_a_time.domain.playback.repository.AudioBookPlaybackRepository;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlaybackFlushScheduler {

    private static final String PLAYBACK_KEY_PATTERN = "playback:*";
    private static final long FLUSH_INTERVAL_MILLIS = 60_000L;

    private final PlaybackRedisRepository playbackRedisRepository;
    private final AudioBookPlaybackRepository playbackRepository;
    private final AudioBookRepository audioBookRepository;
    private final AtomicBoolean redisDownLogged = new AtomicBoolean(false);

    // Redis에 임시 저장된 진행도를 주기적으로 DB에 반영한다.
    @Scheduled(fixedDelay = FLUSH_INTERVAL_MILLIS)
    @Transactional
    public void flushPlaybackProgress() {
        Set<String> keys;
        try {
            keys = playbackRedisRepository.findAllPlaybackKeys();
            resetRedisDownFlagIfRecovered();
        } catch (DataAccessException e) {
            logRedisDownOnce("플레이백 flush 중단: Redis 키 조회 불가", e);
            return;
        }
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            Optional<PlaybackRedisRepository.KeyInfo> keyInfoOpt = playbackRedisRepository.parseKey(key);
            if (keyInfoOpt.isEmpty()) {
                log.warn("잘못된 Redis 키 형식으로 건너뜁니다: {}", key);
                continue;
            }
            PlaybackRedisRepository.KeyInfo info = keyInfoOpt.get();
            try {
                Optional<PlaybackRedisRepository.PlaybackRedisValue> redisValOpt = playbackRedisRepository.readPlayback(info.memberId(), info.audiobookId());
                if (redisValOpt.isPresent()) {
                    flushToDatabase(new ParsedPlayback(info, redisValOpt.get()));
                }
            } catch (DataAccessException e) {
                logRedisDownOnce("Flush 건너뜀: Redis 오류. key={}, memberId={}, audiobookId={}", e, key, info.memberId(), info.audiobookId());
                continue;
            } catch (Exception e) {
                log.error("Flush 건너뜀: 예상치 못한 오류. key={}, memberId={}, audiobookId={}", key, info.memberId(), info.audiobookId(), e);
                continue;
            }
        }
    }

    private void flushToDatabase(ParsedPlayback parsed) {
        Long audiobookId = parsed.info.audiobookId();
        Long memberId = parsed.info.memberId();
        Integer lastPosition = parsed.value.getLastPosition();

        if (lastPosition == null) {
            return;
        }

        Optional<AudioBook> audioBookOpt = audioBookRepository.findById(audiobookId);
        if (audioBookOpt.isEmpty()) {
            return;
        }

        double duration = Optional.ofNullable(audioBookOpt.get().getDuration()).orElse(0.0d);
        float progressRate = calculateProgressRate(duration, lastPosition);
        int listenedDuration = lastPosition;
        PlaybackStatus status = Optional.ofNullable(parsed.value.getStatus()).orElse(PlaybackStatus.PLAYING);

        playbackRepository.updateProgress(audiobookId, memberId, lastPosition, progressRate, listenedDuration, status);
    }

    private float calculateProgressRate(Double duration, Integer position) {
        if (duration == null || duration <= 0) {
            return 0f;
        }
        int safePosition = Optional.ofNullable(position).orElse(0);
        float rate = (float) (safePosition / duration);
        return Math.min(1.0f, Math.max(0f, rate));
    }

    private record ParsedPlayback(PlaybackRedisRepository.KeyInfo info,
                                  PlaybackRedisRepository.PlaybackRedisValue value) {
    }

    private void logRedisDownOnce(String message, DataAccessException e, Object... args) {
        if (redisDownLogged.compareAndSet(false, true)) {
            log.error(message, args, e);
        }
    }

    private void resetRedisDownFlagIfRecovered() {
        if (redisDownLogged.get()) {
            redisDownLogged.set(false);
        }
    }
}
