package pproject.once_upon_a_time.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pproject.once_upon_a_time.global.storage.S3StorageService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    private static final String IMAGE_PREFIX = "uploads/images/";
    private static final String AUDIO_PREFIX = "uploads/audio/";
    private static final String CHARACTER_PREFIX = "uploads/character/";

    private final S3StorageService s3StorageService;

    private final List<String> imageTypes = List.of("image/png", "image/jpeg", "image/jpg", "image/webp");
    private final List<String> audioTypes = List.of("audio/mpeg", "audio/wav", "audio/mp4", "audio/ogg");

    public String uploadFile(MultipartFile file, FileType type) throws IOException {
        validateMimeType(file, type);

        String prefix = switch (type) {
            case IMAGE -> IMAGE_PREFIX;
            case AUDIO -> AUDIO_PREFIX;
            case CHARACTER -> CHARACTER_PREFIX;
        };
        String originalName = file.getOriginalFilename();
        String fileName = UUID.randomUUID() + "-" + (originalName == null ? "file" : originalName);
        String key = prefix + fileName;

        s3StorageService.uploadFile(key, file);
        return s3StorageService.createPresignedGetUrl(key);
    }

    private void validateMimeType(MultipartFile file, FileType type) {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("파일의 Content-Type을 확인할 수 없습니다.");
        }

        boolean allowed = switch (type) {
            case IMAGE -> imageTypes.contains(contentType);
            case AUDIO, CHARACTER -> audioTypes.contains(contentType);
        };

        if (!allowed) {
            throw new IllegalArgumentException("허용되지 않은 " + type.name().toLowerCase() + " MIME 타입입니다: " + contentType);
        }
    }

    public enum FileType {
        IMAGE, AUDIO, CHARACTER
    }
}
