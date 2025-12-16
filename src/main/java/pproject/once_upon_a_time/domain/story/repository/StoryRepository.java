package pproject.once_upon_a_time.domain.story.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pproject.once_upon_a_time.domain.story.domain.Story;

import java.util.List;

public interface StoryRepository extends JpaRepository<Story, Long> {
    List<Story> findByTitleContainingIgnoreCase(String title);
    List<Story> findByMemberId(Long memberId);
    List<Story> findByMemberIdAndTitleContainingIgnoreCase(Long memberId, String title);
}
