package pproject.once_upon_a_time.global.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * List<String> 타입을 DB의 JSON 문자열로 변환하기 위한 JPA AttributeConverter
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final Logger log = LoggerFactory.getLogger(StringListConverter.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return mapper.writeValueAsString(attribute);
        } catch (Exception e) {
            log.warn("List<String> to JSON 변환 실패, null 반환. attribute={}", attribute, e);
            return null;
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(dbData, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("JSON to List<String> 파싱 실패, 빈 리스트 반환. raw={}", dbData, e);
            return List.of();
        }
    }
}
