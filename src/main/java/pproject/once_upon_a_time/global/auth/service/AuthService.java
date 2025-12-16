package pproject.once_upon_a_time.global.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.member.repository.MemberRepository;
import pproject.once_upon_a_time.domain.token.domain.RefreshToken;
import pproject.once_upon_a_time.domain.token.repository.RefreshTokenRepository;
import pproject.once_upon_a_time.global.auth.config.JwtProvider;
import pproject.once_upon_a_time.global.auth.dto.request.SignupRequestDto;
import pproject.once_upon_a_time.global.auth.dto.request.TokenReissueRequestDto;
import pproject.once_upon_a_time.global.auth.dto.response.KakaoLoginResponseDto;
import pproject.once_upon_a_time.global.auth.dto.response.KakaoTokenResponseDto;
import pproject.once_upon_a_time.global.auth.dto.response.KakaoUserInfoResponseDto;
import pproject.once_upon_a_time.global.auth.dto.response.TokenResponseDto;
import pproject.once_upon_a_time.global.common.MemberRole;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final KakaoAuthService kakaoAuthService;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    // [수정] redirectUri 파라미터 추가
    public KakaoLoginResponseDto kakaoLogin(String code, String redirectUri) {

        // 1. 인가 코드로 카카오 토큰 받기 (여기에 redirectUri를 꼭 같이 넘겨야 함!)
        KakaoTokenResponseDto kakaoTokens = kakaoAuthService.getKakaoTokens(code, redirectUri);

        // 2. 카카오 토큰으로 사용자 정보 받기
        KakaoUserInfoResponseDto userInfo = kakaoAuthService.getKakaoUserInfo(kakaoTokens.getAccessToken());
        String kakaoUserId = userInfo.getId().toString();

        Optional<Member> optionalMember = memberRepository.findByKakaoUserId(kakaoUserId);

        if (optionalMember.isPresent()) {
            Member member = optionalMember.get();
            TokenResponseDto tokenResponseDto = createAndSaveToken(member);

            return KakaoLoginResponseDto.builder()
                .isNewUser(false)
                .accessToken(tokenResponseDto.getAccessToken())
                .refreshToken(tokenResponseDto.getRefreshToken())
                .build();
        } else {
            String signupToken = jwtProvider.createSignupToken(kakaoUserId);
            log.info("신규 사용자 확인. 회원가입용 임시 토큰 발급.");

            return KakaoLoginResponseDto.builder()
                .isNewUser(true)
                .signupToken(signupToken)
                .build();
        }
    }

    public TokenResponseDto signup(String signupToken, SignupRequestDto requestDto) {
        String kakaoUserId = jwtProvider.getKakaoUserIdFromSignupToken(signupToken);
        if (kakaoUserId == null) {
            throw new CustomException(ErrorCode.TOKEN_INVALID);
        }

        if (memberRepository.findByKakaoUserId(kakaoUserId).isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        LocalDate birthDate = LocalDate.parse(requestDto.getBirth(), formatter);

        Member member = Member.builder()
            .kakaoUserId(kakaoUserId)
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
    public TokenResponseDto reissue(TokenReissueRequestDto request) {
        if (!jwtProvider.validateToken(request.getRefreshToken())) {
            throw new CustomException(ErrorCode.TOKEN_INVALID);
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
            .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        Member member = memberRepository.findById(refreshToken.getMember().getId())
            .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        String newAccessToken = jwtProvider.createAccessToken(member.getId(), member.getRole());

        return TokenResponseDto.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshToken.getToken())
            .build();
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
                .member(member)
                .token(tokenResponseDto.getRefreshToken())
                .build();
            refreshTokenRepository.save(refreshToken);
        }
        return tokenResponseDto;
    }

    @Transactional
    public TokenResponseDto devLogin(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        return createAndSaveToken(member);
    }
}
