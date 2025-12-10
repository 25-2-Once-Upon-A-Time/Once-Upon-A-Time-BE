package pproject.once_upon_a_time.domain.playback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pproject.once_upon_a_time.domain.playback.domain.AudioBookPlayback;
import pproject.once_upon_a_time.domain.playback.domain.PlaybackStatus;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AudioBookPlaybackRepository extends JpaRepository<AudioBookPlayback, Long> {

    Optional<AudioBookPlayback> findByAudioBook_IdAndMemberId_Id(Long audioBookId, Long memberId);

    @Modifying
    @Query("""
            update AudioBookPlayback p
               set p.lastPosition = :lastPosition,
                   p.progressRate = :progressRate,
                   p.listenedDuration = :listenedDuration,
                   p.status = :status
             where p.audioBook.id = :audioBookId
               and p.memberId.id = :memberId
            """)
    int updateProgress(@Param("audioBookId") Long audioBookId,
                       @Param("memberId") Long memberId,
                       @Param("lastPosition") Integer lastPosition,
                       @Param("progressRate") Float progressRate,
                       @Param("listenedDuration") Integer listenedDuration,
                       @Param("status") PlaybackStatus status);

    @Modifying
    @Query("""
            update AudioBookPlayback p
               set p.lastPosition = :lastPosition,
                   p.progressRate = :progressRate,
                   p.listenedDuration = :listenedDuration,
                   p.status = :status,
                   p.endedAt = :endedAt
             where p.audioBook.id = :audioBookId
               and p.memberId.id = :memberId
            """)
    int finishPlayback(@Param("audioBookId") Long audioBookId,
                       @Param("memberId") Long memberId,
                       @Param("lastPosition") Integer lastPosition,
                       @Param("progressRate") Float progressRate,
                       @Param("listenedDuration") Integer listenedDuration,
                       @Param("status") PlaybackStatus status,
                       @Param("endedAt") LocalDateTime endedAt);
}
