package pproject.once_upon_a_time.domain.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pproject.once_upon_a_time.global.common.MemberRole;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String kakaoUserId;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, length = 30, unique = true)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Column(length = 10)
    private String gender; // "male", "female"

    private LocalDate birth; // YYYY-MM-DD

    @Column(length = 20)
    private String personalPhone;

    @Builder
    public Member(String kakaoUserId, String name, String nickname, MemberRole role, String gender, LocalDate birth, String personalPhone) {
        this.kakaoUserId = kakaoUserId;
        this.name = name;
        this.nickname = nickname;
        this.role = role;
        this.gender = gender;
        this.birth = birth;
        this.personalPhone = personalPhone;
    }
}
