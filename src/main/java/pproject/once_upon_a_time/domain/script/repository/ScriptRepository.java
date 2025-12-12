package pproject.once_upon_a_time.domain.script.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pproject.once_upon_a_time.domain.script.domain.Script;

public interface ScriptRepository extends JpaRepository<Script, Long> {
}
