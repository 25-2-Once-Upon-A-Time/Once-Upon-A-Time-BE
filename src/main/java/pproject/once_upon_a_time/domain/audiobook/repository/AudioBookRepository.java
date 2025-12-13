package pproject.once_upon_a_time.domain.audiobook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pproject.once_upon_a_time.domain.audiobook.domain.AudioBook;
import pproject.once_upon_a_time.domain.audiobook.dto.AudioBookResponseDto;

import java.util.List;

public interface AudioBookRepository extends JpaRepository<AudioBook, Long> {

    @Query("""
            select new pproject.once_upon_a_time.domain.audiobook.dto.AudioBookResponseDto$Item(
                a.id,
                s.title,
                s.title,
                s.theme,
                s.vibe,
                s.thumbnailUrl,
                c.characterName,
                a.duration
            )
            from AudioBook a
            join a.story s
            join a.character c
            join a.member m
            where m.id = :memberId
            """)
    List<AudioBookResponseDto.Item> findItemsByMemberId(@Param("memberId") Long memberId);
}
