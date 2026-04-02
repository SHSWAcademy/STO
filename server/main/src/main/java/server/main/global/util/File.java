package server.main.global.util;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class File extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileId;        // 파일ID
    private String origin_name; // 파일 원본명
    private String stored_name; // 파일 저장명
    private Long size;          // 파일 용량
    private String path;        // 파일 저장명


}
