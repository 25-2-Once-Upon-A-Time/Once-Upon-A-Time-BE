package pproject.once_upon_a_time.domain.story.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import pproject.once_upon_a_time.domain.story.dto.ScriptItem;

import java.io.IOException;
import java.util.List;

@Converter
@Slf4j
public class ScriptConverter implements AttributeConverter<List<ScriptItem>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<ScriptItem> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Error converting List<ScriptItem> to JSON string", e);
            throw new IllegalArgumentException("Could not convert list to JSON", e);
        }
    }

    @Override
    public List<ScriptItem> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<ScriptItem>>() {});
        } catch (IOException e) {
            log.error("Error converting JSON string to List<ScriptItem>", e);
            throw new IllegalArgumentException("Could not convert JSON to list", e);
        }
    }
}