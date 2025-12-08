package pproject.once_upon_a_time.global.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.global.auth.config.JwtProvider;
import pproject.once_upon_a_time.global.auth.dto.request.KakaoSignupRequestDto;
import pproject.once_upon_a_time.global.auth.dto.response.KakaoLoginResponseDto;
import pproject.once_upon_a_time.global.auth.dto.response.KakaoTokenResponseDto;
import pproject.once_upon_a_time.global.auth.dto.response.KakaoUserInfoResponseDto;
import pproject.once_upon_a_time.global.auth.dto.response.TokenResponseDto;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.member.repository.MemberRepository;
import pproject.once_upon_a_time.domain.token.domain.RefreshToken;
import pproject.once_upon_a_time.domain.token.repository.RefreshTokenRepository;
import pproject.once_upon_a_time.global.common.MemberRole;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final KakaoAuthService kakaoAuthService;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    public KakaoLoginResponseDto kakaoLogin(String code) {
        KakaoTokenResponseDto kakaoTokens = kakaoAuthService.getKakaoTokens(code);
        KakaoUserInfoResponseDto userInfo = kakaoAuthService.getKakaoUserInfo(kakaoTokens.getAccessToken());
        String kakaoUserId = userInfo.getId().toString();

        Optional<Member> optionalMember = memberRepository.findByKakaoUserId(kakaoUserId);

        if (optionalMember.isPresent()) {
            // 기존 회원일 경우
            Member member = optionalMember.get();
            TokenResponseDto tokenResponseDto = createAndSaveToken(member);

            return KakaoLoginResponseDto.builder()
                    .isNewUser(false)
                    .accessToken(tokenResponseDto.getAccessToken())
                    .refreshToken(tokenResponseDto.getRefreshToken())
                    .build();
        } else {
            // 신규 회원일 경우
            return KakaoLoginResponseDto.builder()
                    .isNewUser(true)
                    .kakaoUserId(kakaoUserId)
                    .build();
        }
    }

    public TokenResponseDto signup(KakaoSignupRequestDto requestDto) {
        if (memberRepository.findByKakaoUserId(requestDto.getKakaoUserId()).isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL, "이미 가입된 카카오 계정입니다.");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        LocalDate birthDate = LocalDate.parse(requestDto.getBirth(), formatter);

        Member member = Member.builder()
                .kakaoUserId(requestDto.getKakaoUserId())
                .name(requestDto.getName())
                .nickname(requestDto.getNickname())
                .gender(requestDto.getGender())
                .birth(birthDate)
                .personalPhone(requestDto.getPersonalPhone())
                .role(MemberRole.USER)
                .build();

        Member savedMember = memberRepository.save(member);
        return createAndSaveToken(savedMember);
    }
    
    @Transactional
    public void logout(String accessToken) {
        String resolvedToken = accessToken.substring(7);
        Long memberId = jwtProvider.getMemberIdFromToken(resolvedToken);
        refreshTokenRepository.deleteByMemberId(memberId);
    }

    private TokenResponseDto createAndSaveToken(Member member) {
        TokenResponseDto tokenResponseDto = jwtProvider.createToken(member);
        Optional<RefreshToken> optionalRefreshToken = refreshTokenRepository.findByMemberId(member.getId());

        if (optionalRefreshToken.isPresent()) {
            RefreshToken refreshToken = optionalRefreshToken.get();
            refreshToken.updateToken(tokenResponseDto.getRefreshToken());
        } else {
            RefreshToken refreshToken = RefreshToken.builder()
                    .memberId(member.getId())
                    .token(tokenResponseDto.getRefreshToken())
                    .build();
            refreshTokenRepository.save(refreshToken);
        }
        return tokenResponseDto;
    }
}
