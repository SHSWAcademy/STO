package server.main.disclosure.mapper;

import org.springframework.stereotype.Component;
import server.main.disclosure.entity.Disclosure;
import server.main.disclosure.entity.DisclosureCategory;

@Component
public class DisclosureMapper {

    // 자산 등록 시 공시 자동 등록(BUILDING) dto -> entity
    public Disclosure toDisclosure(String assetName, Long assetId) {
        return Disclosure.builder()
                .assetId(assetId)
                .disclosureCategory(DisclosureCategory.BUILDING)
                .disclosureTitle(assetName + " 에 관한 자산 안내입니다.")
                .disclosureContent("자세한 내용은 첨부파일 참고 바랍니다.")
                .build();
    }

    // 자산 등록 시 공시 자동 등록(DIVIDEND) dto -> entity
    public Disclosure toDisclosureAllocation(int year, int month, String assetName, Long assetId) {
        return Disclosure.builder()
                .assetId(assetId)
                .disclosureCategory(DisclosureCategory.DIVIDEND)
                .disclosureTitle(year + "년" + month + "월" + " " + assetName + " " + "배당 보고서")
                .disclosureContent(assetName + "의" + " " + " " + year + "년" + month + "월" + " " + "운용 실적 및 배당금 산정 내역 보고서 입니다.")
                .build();
    }
}
