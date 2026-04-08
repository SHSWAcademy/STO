package server.batch.token.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "TOKENS")
public class Token {
    @Id @Column(name = "token_id")
    private Long tokenId;

    @Enumerated(EnumType.STRING)
    private TokenStatus tokenStatus;
}
