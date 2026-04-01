package server.main.member.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.main.global.util.BaseEntity;

@Entity
@Getter
@Table(name = "ACCOUNTS")
@NoArgsConstructor
public class Account extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;

    private String accountNumber;

    private String accountPassword; //bcrypt 변경 메서드 - global 패키지 util에 생성 필요

    private Long availableBalance;

    private Long lockedBalance;
}
