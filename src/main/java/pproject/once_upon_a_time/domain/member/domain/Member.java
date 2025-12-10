package pproject.once_upon_a_time.domain.member.domain;

import jakarta.persistence.*;
import lombok.*;
import pproject.once_upon_a_time.global.common.MemberRole;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String kakaoUserId;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, unique = true, length = 30)
    private String nickname;

    @Column(length = 20)
    private String personalPhone;

    @Column(length = 10)
    private String gender;

    private LocalDate birth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;
}