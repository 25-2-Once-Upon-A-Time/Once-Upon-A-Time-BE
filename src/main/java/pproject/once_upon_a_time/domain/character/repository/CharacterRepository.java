package pproject.once_upon_a_time.domain.character.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pproject.once_upon_a_time.domain.character.domain.Character;

import java.util.List;

public interface CharacterRepository extends JpaRepository<Character, Long> {
    List<Character> findByCharacterNameContainingIgnoreCase(String characterName);
}
