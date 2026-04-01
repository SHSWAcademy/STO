package server.main.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.main.global.util.BaseEntity;

@Entity
@Getter
@Table(name = "MEMBERS")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    private String email;

    private String memberPassword;

    private String memberName;

    private Boolean isActive;

}
