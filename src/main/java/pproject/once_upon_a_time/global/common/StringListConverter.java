package pproject.once_upon_a_time.global.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.SneakyThrows;

import java.util.List;

/**
 * List<String> 타입을 DB의 JSON 문자열로 변환하기 위한 JPA AttributeConverter
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private final ObjectMapper mapper = new ObjectMapper();

    @SneakyThrows
    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return mapper.writeValueAsString(attribute);
    }

    @SneakyThrows
    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        return mapper.readValue(dbData, new TypeReference<>() {});
    }
}
