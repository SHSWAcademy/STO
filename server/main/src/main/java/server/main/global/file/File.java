package server.main.global.file;

import jakarta.persistence.*;
import lombok.*;
import server.main.global.util.BaseEntity;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Table(name = "files")
public class File extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileId;        // 파일ID
    private Long disclosureId;  // 공시ID (FK)
    private String origin_name; // 파일 원본명
    private String stored_name; // 파일 저장명
    private Long size;          // 파일 용량
    private String path;        // 파일 저장명

}
