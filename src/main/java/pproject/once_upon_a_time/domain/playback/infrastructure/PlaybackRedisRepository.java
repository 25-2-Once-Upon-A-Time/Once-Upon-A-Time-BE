package pproject.once_upon_a_time.domain.playback.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import pproject.once_upon_a_time.domain.playback.domain.PlaybackStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Redis에 플레이백 진행 정보를 저장/조회/삭제하는 레포지토리.
 */
@Repository
@RequiredArgsConstructor
public class PlaybackRedisRepository {

    private static final String KEY_FORMAT = "playback:%d:%d";
    private static final String FIELD_LAST_POSITION = "last_position";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_UPDATED_AT = "updated_at";
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final Pattern KEY_PATTERN = Pattern.compile("playback:(\\d+):(\\d+)");

    private final StringRedisTemplate stringRedisTemplate;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    public void savePlayback(Long memberId, Long audiobookId, Integer lastPosition, PlaybackStatus status, LocalDateTime updatedAt) {
        String key = buildKey(memberId, audiobookId);
        HashOperations<String, String, String> ops = stringRedisTemplate.opsForHash();

        Map<String, String> values = new HashMap<>();
        values.put(FIELD_LAST_POSITION, lastPosition == null ? "0" : lastPosition.toString());
        values.put(FIELD_STATUS, status == null ? PlaybackStatus.PLAYING.name() : status.name());
        values.put(FIELD_UPDATED_AT, updatedAt == null ? LocalDateTime.now().format(formatter) : updatedAt.format(formatter));

        ops.putAll(key, values);
        stringRedisTemplate.expire(key, TTL);
    }

    public Optional<PlaybackRedisValue> readPlayback(Long memberId, Long audiobookId) {
        String key = buildKey(memberId, audiobookId);
        HashOperations<String, String, String> ops = stringRedisTemplate.opsForHash();

        String lastPositionValue = ops.get(key, FIELD_LAST_POSITION);
        String statusValue = ops.get(key, FIELD_STATUS);
        String updatedAtValue = ops.get(key, FIELD_UPDATED_AT);

        if (lastPositionValue == null && statusValue == null && updatedAtValue == null) {
            return Optional.empty();
        }

        PlaybackRedisValue value = PlaybackRedisValue.builder()
                .lastPosition(parseInteger(lastPositionValue))
                .status(parseStatus(statusValue))
                .updatedAt(parseDateTime(updatedAtValue))
                .build();

        return Optional.of(value);
    }

    public void deletePlaybackRedisKey(Long memberId, Long audiobookId) {
        stringRedisTemplate.delete(buildKey(memberId, audiobookId));
    }

    public String buildKey(Long memberId, Long audiobookId) {
        return String.format(KEY_FORMAT, memberId, audiobookId);
    }

    public Set<String> findAllPlaybackKeys() {
        return stringRedisTemplate.keys("playback:*");
    }

    public Optional<KeyInfo> parseKey(String key) {
        Matcher matcher = KEY_PATTERN.matcher(key);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        Long memberId = parseLong(matcher.group(1));
        Long audiobookId = parseLong(matcher.group(2));
        if (memberId == null || audiobookId == null) {
            return Optional.empty();
        }
        return Optional.of(new KeyInfo(memberId, audiobookId));
    }

    private Integer parseInteger(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private PlaybackStatus parseStatus(String value) {
        if (value == null) {
            return null;
        }
        try {
            return PlaybackStatus.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, formatter);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @lombok.Builder
    @lombok.Getter
    public static class PlaybackRedisValue {
        private final Integer lastPosition;
        private final PlaybackStatus status;
        private final LocalDateTime updatedAt;
    }

    public record KeyInfo(Long memberId, Long audiobookId) {
    }
}
