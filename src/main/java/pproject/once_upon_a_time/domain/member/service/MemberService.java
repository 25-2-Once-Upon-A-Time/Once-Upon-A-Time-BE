package pproject.once_upon_a_time.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.member.dto.MemberResponse;
import pproject.once_upon_a_time.domain.member.dto.MemberUpdateRequest;
import pproject.once_upon_a_time.domain.member.repository.MemberRepository;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    /**
     * 내 프로필 조회
     */
    public MemberResponse getMemberProfile(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        return MemberResponse.from(member);
    }

    /**
     * 내 프로필 수정
     */
    @Transactional
    public void updateMemberProfile(Long memberId, MemberUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 닉네임 중복 검사 (현재 닉네임과 다른 경우에만)
        if (!member.getNickname().equals(request.getNickname())) {
            if (memberRepository.existsByNickname(request.getNickname())) {
                throw new CustomException(ErrorCode.DUPLICATE_EMAIL, "이미 사용 중인 닉네임입니다."); // Note: A more specific ErrorCode like DUPLICATE_NICKNAME would be better.
            }
        }

        // Dirty Checking을 활용한 업데이트
        member.updateProfile(request.getNickname(), request.getPersonalPhone());
    }
}
