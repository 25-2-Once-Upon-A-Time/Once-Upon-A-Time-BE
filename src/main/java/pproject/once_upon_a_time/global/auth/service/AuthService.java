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
import pproject.once_upon_a_time.global.auth.dto.request.TokenReissueRequestDto; // [추가]
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

    public KakaoLoginResponseDto kakaoLogin(String code) {
        // 1. 인가 코드로 카카오 토큰 받기
        KakaoTokenResponseDto kakaoTokens = kakaoAuthService.getKakaoTokens(code);
        // 2. 카카오 토큰으로 사용자 정보 받기
        KakaoUserInfoResponseDto userInfo = kakaoAuthService.getKakaoUserInfo(kakaoTokens.getAccessToken());
        String kakaoUserId = userInfo.getId().toString();

        Optional<Member> optionalMember = memberRepository.findByKakaoUserId(kakaoUserId);

        if (optionalMember.isPresent()) {
            // 3-1. 기존 회원일 경우, 우리 서비스의 토큰 발급
            Member member = optionalMember.get();
            TokenResponseDto tokenResponseDto = createAndSaveToken(member);

            return KakaoLoginResponseDto.builder()
                .isNewUser(false)
                .accessToken(tokenResponseDto.getAccessToken())
                .refreshToken(tokenResponseDto.getRefreshToken())
                .build();
        } else {
            // 3-2. 신규 회원일 경우, 회원가입에 사용할 임시 'signupToken' 발급
            String signupToken = jwtProvider.createSignupToken(kakaoUserId);
            log.info("신규 사용자 확인. 회원가입용 임시 토큰 발급.");

            return KakaoLoginResponseDto.builder()
                .isNewUser(true)
                .signupToken(signupToken)
                .build();
        }
    }

    public TokenResponseDto signup(String signupToken, SignupRequestDto requestDto) {
        // 1. signupToken 검증
        String kakaoUserId = jwtProvider.getKakaoUserIdFromSignupToken(signupToken);
        if (kakaoUserId == null) {
            throw new CustomException(ErrorCode.TOKEN_INVALID); // 에러코드 수정 필요시 변경
        }

        // 2. 중복 가입 체크
        if (memberRepository.findByKakaoUserId(kakaoUserId).isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL); // 적절한 에러코드로 변경
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        LocalDate birthDate = LocalDate.parse(requestDto.getBirth(), formatter);

        // 3. 사용자 생성
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

        // 4. 최종 토큰 발급
        return createAndSaveToken(savedMember);
    }

    // [최종] 토큰 재발급 (리프레시 토큰 고정)
    @Transactional
    public TokenResponseDto reissue(TokenReissueRequestDto request) {
        // 1. 유효성 검사
        if (!jwtProvider.validateToken(request.getRefreshToken())) {
            throw new CustomException(ErrorCode.TOKEN_INVALID);
        }

        // 2. DB 조회
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
            .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        // 3. 멤버 조회
        Member member = memberRepository.findById(refreshToken.getMember().getId())
            .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 4. Access Token만 새로 생성 (이제 이 메서드가 존재함!)
        String newAccessToken = jwtProvider.createAccessToken(member.getId(), member.getRole());

        // 5. 결과 반환 (기존 Refresh Token 유지)
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
        // 1. DB에서 memberId로 회원 조회
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 2. 토큰 생성 및 반환 (기존 로그인 로직 재활용)
        // createAndSaveToken 메서드는 이미 구현되어 있다고 가정합니다.
        return createAndSaveToken(member);
    }
}
