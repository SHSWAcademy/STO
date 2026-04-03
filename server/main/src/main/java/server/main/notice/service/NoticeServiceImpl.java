package server.main.notice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import server.main.admin.dto.AssetRegisterRequestDTO;
import server.main.asset.entity.Asset;
import server.main.notice.dto.NoticeRegisterAssetDTO;
import server.main.notice.mapper.NoticeMapper;
import server.main.notice.repository.NoticeRepository;

@Service
@RequiredArgsConstructor
@Log4j2
public class NoticeServiceImpl implements NoticeService{

    private final NoticeRepository noticeRepository;
    private final NoticeMapper noticeMapper;

    // 자산 등록 시 자동 공지 등록
    @Override
    public void registerAssetNotice(AssetRegisterRequestDTO dto) {
        NoticeRegisterAssetDTO noticeRegisterAssetDTO = new NoticeRegisterAssetDTO();
        noticeRegisterAssetDTO.changeNotice(dto);
        log.info("공지등록 내역 확인 : {} ", noticeRegisterAssetDTO);
        noticeRepository.save(noticeMapper.toNotice(noticeRegisterAssetDTO));
    }
}
