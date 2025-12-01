package pproject.once_upon_a_time.domain.member.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "member")
public class Member {

    @Id
    @Column(name = "member_id", nullable = false, length = 255)
    private String memberId;        // 아이디

    @Column(name = "password", nullable = false, length = 255)
    private String password;        // 비밀번호

    @Column(name = "name", length = 200)
    private String name;            // 실명

    @Column(name = "gender", length = 200)
    private String gender;          // 성별

    @Column(name = "is_delete")
    private Boolean isDelete;       // 계정 탈퇴 여부 (Y/N)

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // 생성일

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 수정일

    @Column(name = "profile_img", length = 255)
    private String profileImg;      // 프로필 이미지 URL
}