package pproject.once_upon_a_time.domain.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pproject.once_upon_a_time.domain.admin.dto.AdminAudioBookResponseDto;
import pproject.once_upon_a_time.domain.admin.dto.AdminMemberResponseDto;
import pproject.once_upon_a_time.domain.admin.dto.AdminStoryResponseDto;
import pproject.once_upon_a_time.domain.admin.service.AdminService;
import pproject.once_upon_a_time.global.response.ApiResult;

import java.util.List;

@Tag(name = "Admin API", description = "관리자 전용 기능 (ADMIN 권한 필수)")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "전체 회원 조회")
    @GetMapping("/members")
    public ResponseEntity<ApiResult<List<AdminMemberResponseDto>>> getAllMembers() {
        return ResponseEntity.ok(ApiResult.ok(adminService.getAllMembers()));
    }

    @Operation(summary = "회원 강제 탈퇴")
    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<ApiResult<Void>> deleteMember(@PathVariable Long memberId) {
        adminService.deleteMember(memberId);
        return ResponseEntity.ok(ApiResult.ok(null));
    }

    @Operation(summary = "전체 동화 조회")
    @GetMapping("/stories")
    public ResponseEntity<ApiResult<List<AdminStoryResponseDto>>> getAllStories() {
        return ResponseEntity.ok(ApiResult.ok(adminService.getAllStories()));
    }

    @Operation(summary = "전체 오디오북 조회")
    @GetMapping("/audiobooks")
    public ResponseEntity<ApiResult<List<AdminAudioBookResponseDto>>> getAllAudioBooks() {
        return ResponseEntity.ok(ApiResult.ok(adminService.getAllAudioBooks()));
    }
}
