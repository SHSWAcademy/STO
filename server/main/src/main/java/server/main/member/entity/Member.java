package server.main.member.entity;

import jakarta.persistence.*;
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

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY) // cascade 설정 : member 삭제 시 account도 삭제되어야 한다면 cascade = CascadeType.REMOVE 추가 설정 필요
    private Account account;
}
