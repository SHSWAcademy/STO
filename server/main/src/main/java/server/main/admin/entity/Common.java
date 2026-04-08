package server.main.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Table(name = "commons")
public class Common {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long baseId;    // 기본ID
    private Double taxRate; // 세율
    private Double chargeRate; // 수수료
    private int allocateDate; // 배당 지급일
    private int allocateSetDate; // 배당 입력일 (관리자)

    // 수정용
    public void update(Double taxRate, Double chargeRate, int allocateDate, int allocateSetDate) {
        this.taxRate = taxRate;
        this.chargeRate = chargeRate;
        this.allocateDate = allocateDate;
        this.allocateSetDate = allocateSetDate;
    }

}
