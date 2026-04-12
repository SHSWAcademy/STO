package server.main.member.entity;

import jakarta.persistence.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.main.global.util.BaseEntity;

@Entity
@Getter
@Table(name = "MEMBERS")
@NoArgsConstructor
public class Member extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "email")
    private String email;

    @Column(name = "member_password")
    private String memberPassword;

    @Column(name = "member_name")
    private String memberName;

    @Column(name = "is_active")
    private Boolean isActive;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "member")
    private Account account;

    public static Member create(String email, String encodedPassword, String name) {
        Member member = new Member();
        member.email = email;
        member.memberPassword = encodedPassword;
        member.memberName = name;
        member.isActive = true;
        return member;
    }

    // 멤버 활성/비활성화 처리 (admin)
    public void updateIsActive(boolean isActive) {
        this.isActive = isActive;
    }
}
