package pproject.once_upon_a_time.domain.member.domain;

import jakarta.persistence.*;
import lombok.*;
import pproject.once_upon_a_time.domain.audiobook.domain.AudioBook;
import pproject.once_upon_a_time.global.common.BaseTimeEntity;
import pproject.once_upon_a_time.global.common.MemberRole;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "member")
public class Member extends BaseTimeEntity {

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

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<AudioBook> audioBooks;

    public void updateProfile(String newNickname, String newPersonalPhone) {
        if (newNickname != null && !newNickname.isBlank()) {
            this.nickname = newNickname;
        }
        if (newPersonalPhone != null && !newPersonalPhone.isBlank()) {
            this.personalPhone = newPersonalPhone;
        }
    }
}