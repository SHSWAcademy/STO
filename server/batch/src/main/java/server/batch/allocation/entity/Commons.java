package server.batch.allocation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "commons")
public class Commons {

    @Id
    private Long baseId;

    private Double taxRate;
    private Double chargeRate;
    private Integer allocateDate;
    private Integer allocateSetDate;
}
