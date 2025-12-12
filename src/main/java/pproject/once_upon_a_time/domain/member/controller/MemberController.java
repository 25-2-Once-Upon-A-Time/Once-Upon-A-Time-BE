package pproject.once_upon_a_time.domain.member.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pproject.once_upon_a_time.domain.member.dto.MemberResponse;
import pproject.once_upon_a_time.domain.member.dto.MemberUpdateRequest;
import pproject.once_upon_a_time.domain.member.service.MemberService;
import pproject.once_upon_a_time.global.response.ApiResult;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 내 프로필 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResult<MemberResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        MemberResponse response = memberService.getMemberProfile(memberId);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    /**
     * 내 프로필 수정
     */
    @PatchMapping("/me")
    public ResponseEntity<ApiResult<Void>> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody MemberUpdateRequest request
    ) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        memberService.updateMemberProfile(memberId, request);
        return ResponseEntity.ok(ApiResult.ok(null));
    }
}
