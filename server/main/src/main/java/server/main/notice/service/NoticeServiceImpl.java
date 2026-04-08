package server.main.notice.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import server.main.admin.dto.AssetRegisterRequestDTO;
import server.main.asset.entity.Asset;
import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.notice.dto.NoticeDetailResponseDTO;
import server.main.notice.dto.NoticeListResponseDTO;
import server.main.notice.dto.NoticeRegisterAssetDTO;
import server.main.notice.dto.NoticeRegisterDTO;
import server.main.notice.entity.Notice;
import server.main.notice.mapper.NoticeMapper;
import server.main.notice.repository.NoticeRepository;

import java.util.List;
import java.util.stream.Collectors;

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

    // 공지사항 조회 (admin)
    @Override
    public List<NoticeListResponseDTO> getNoticeList() {
        List<Notice> notices = noticeRepository.findAll(Sort.by(Sort.Order.desc("createdAt")));
        return notices.stream()
                .map(notice -> noticeMapper.toNoticeAdmin(notice))
                .collect(Collectors.toList());

    }

    // 공지사항 등록 (admin)
    @Override
    public void registerNotice(NoticeRegisterDTO dto) {
        Notice notice = Notice.builder()
                .noticeTitle(dto.getNoticeTitle())
                .noticeContent(dto.getNoticeContent())
                .noticeType(dto.getNoticeType())
                .build();

        log.info("공지사항 저장 : {}", notice);
        // 공지사항 저장
        noticeRepository.save(notice);
    }

    // 공지사항 상세조회 (admin)
    @Override
    public NoticeDetailResponseDTO getNoticeDetail(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUNT_ERROR));
        log.info("공지사항 상세조회 : {}", notice);
        return noticeMapper.noticeDetailResponseDTO(notice);
    }

    // 공지사항 수정
    @Transactional
    @Override
    public void updateNotice(Long noticeId, NoticeRegisterDTO dto) {
        // 수정 대상 조회
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUNT_ERROR));
        // 수정메서드
        notice.updateNotice(dto.getNoticeContent(), dto.getNoticeTitle(), dto.getNoticeType());
        log.info("공지사항 수정내역 : {}", notice);
    }

    // 공지사항 삭제
    @Transactional
    @Override
    public void deleteNotice(Long noticeId) {
        // 삭제 대상 조회
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUNT_ERROR));
        // 삭제 메서드
        notice.softDelete();
        log.info("공지사항 삭제내역 : {}", notice);
    }
}
