package pproject.once_upon_a_time.domain.story.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// This class is a DTO used for JSON serialization within the Story entity.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptItem {
    private Integer seq;
    private String role;
    private String text;
    private String emotion;
    private String audio_file_name;
}