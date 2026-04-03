package server.main.notice.dto;

import lombok.*;
import server.main.admin.dto.AssetRegisterRequestDTO;
import server.main.asset.entity.Asset;
import server.main.notice.entity.NoticeType;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class NoticeRegisterAssetDTO {
    private NoticeType noticeType;   // 공지 타입
    private String noticeTitle;      // 공지 제목
    private String noticeContent;    // 공지 본문

    // 신규 자산 등록 시 공지 자동 등록
    // 문구 하드코딩
    public void changeNotice(AssetRegisterRequestDTO dto) {
        this.noticeType = NoticeType.GENERAL;
        this.noticeTitle = "[신규 상장] " + dto.getAssetName() + " STO 자산 상장 안내";
        this.noticeContent =
                "새로운 " + dto.getAssetName() + " STO 자산이 상장되었습니다.\n\n" +
                        "안녕하세요, STONE입니다.\n\n" +
                        "항상 저희 서비스를 이용해 주시는 고객님께 깊은 감사의 말씀을 드립니다. " +
                        "본 공지사항을 통해 안내드리는 내용을 확인하시어 서비스 이용에 참고하시기 바랍니다.\n\n" +
                        "[주요 내용]\n" +
                        "• 자산명: " + dto.getAssetName() + "\n" +
                        "• 위치: " + dto.getAssetAddress() + "\n" +
                        "• 총 자산가치: " + dto.getTotalValue() + "원\n" +
                        "• 발행 토큰 수: " + dto.getTotalSupply() + "개\n\n" +
                        "자세한 내용은 자산 상세 페이지에서 확인하세요.\n\n" +
                        "더욱 안정적이고 편리한 서비스를 제공하기 위해 최선을 다하겠습니다. " +
                        "감사합니다.";
    }
}
