package server.main.domain.member;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import server.main.domain.BaseEntity;

@Entity
@Getter
@Table(name = "ACCOUNTS")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long id;

    private String accountNumber;

    private String accountPassword; //bcrypt 변경 메서드 - global 패키지 util에 생성 필요

    private Long availableBalance;

    private Long lockedBalance;
}
