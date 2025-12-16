package pproject.once_upon_a_time.domain.script.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pproject.once_upon_a_time.domain.script.domain.Script;

import java.util.List;

public interface ScriptRepository extends JpaRepository<Script, Long> {
    List<Script> findByStoryIdOrderBySeqAsc(Long storyId);
}
