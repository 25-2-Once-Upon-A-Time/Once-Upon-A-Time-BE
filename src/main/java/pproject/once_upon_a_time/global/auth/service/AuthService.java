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
            log.info("신규 사용자 확인. 회원가입용 임시 토큰 발급. (테스트용 로그)");

            return KakaoLoginResponseDto.builder()
                    .isNewUser(true)
                    .signupToken(signupToken) // kakaoUserId 대신 signupToken 반환
                    .build();
        }
    }

    // signupToken을 받아 회원가입 처리
    public TokenResponseDto signup(String signupToken, SignupRequestDto requestDto) {
        // 1. signupToken 검증 및 kakaoUserId 추출
        String kakaoUserId = jwtProvider.getKakaoUserIdFromSignupToken(signupToken);
        if (kakaoUserId == null) {
            throw new CustomException(ErrorCode.TOKEN_INVALID, "회원가입용 토큰이 유효하지 않습니다.");
        }

        // 2. 이미 가입된 사용자인지 다시 한번 확인
        if (memberRepository.findByKakaoUserId(kakaoUserId).isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL, "이미 가입된 카카오 계정입니다.");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        LocalDate birthDate = LocalDate.parse(requestDto.getBirth(), formatter);

        // 3. 토큰에서 얻은 kakaoUserId로 사용자 생성
        Member member = Member.builder()
                .kakaoUserId(kakaoUserId) // 프론트에서 받은 값이 아닌, 토큰에서 추출한 안전한 값 사용
                .name(requestDto.getName())
                .nickname(requestDto.getNickname())
                .gender(requestDto.getGender())
                .birth(birthDate)
                .personalPhone(requestDto.getPersonalPhone())
                .role(MemberRole.USER)
                .build();

        Member savedMember = memberRepository.save(member);

        // 4. 우리 서비스의 최종 토큰 발급
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
                    .member(member)
                    .token(tokenResponseDto.getRefreshToken())
                    .build();
            refreshTokenRepository.save(refreshToken);
        }
        return tokenResponseDto;
    }
}
