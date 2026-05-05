package server.main.notice.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import server.main.admin.dto.AssetRegisterRequestDTO;
import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.notice.dto.NoticeDetailResponseDTO;
import server.main.notice.dto.NoticeListResponseDTO;
import server.main.notice.dto.NoticeRegisterAssetDTO;
import server.main.notice.dto.NoticeRegisterDTO;
import server.main.notice.entity.Notice;
import server.main.notice.mapper.NoticeMapper;
import server.main.notice.repository.NoticeRepository;

@Service
@RequiredArgsConstructor
@Log4j2
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeMapper noticeMapper;

    @Override
    public void registerAssetNotice(AssetRegisterRequestDTO dto) {
        NoticeRegisterAssetDTO noticeRegisterAssetDTO = new NoticeRegisterAssetDTO();
        noticeRegisterAssetDTO.changeNotice(dto);
        log.info("공지 등록 자동 생성: {}", noticeRegisterAssetDTO);
        noticeRepository.save(noticeMapper.toNotice(noticeRegisterAssetDTO));
    }

    @Override
    public Page<NoticeListResponseDTO> getNoticeList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return noticeRepository.findAll(pageable).map(noticeMapper::toNoticeAdmin);
    }

    @Override
    public Page<NoticeListResponseDTO> getPublicNoticeList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return noticeRepository.findByDeletedAtIsNull(pageable).map(noticeMapper::toNoticeAdmin);
    }

    @Override
    public void registerNotice(NoticeRegisterDTO dto) {
        Notice notice = Notice.builder()
                .noticeTitle(dto.getNoticeTitle())
                .noticeContent(dto.getNoticeContent())
                .noticeType(dto.getNoticeType())
                .build();

        log.info("공지사항 저장: {}", notice);
        noticeRepository.save(notice);
    }

    @Override
    public NoticeDetailResponseDTO getNoticeDetail(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUNT_ERROR));
        return noticeMapper.noticeDetailResponseDTO(notice);
    }

    @Override
    public NoticeDetailResponseDTO getPublicNoticeDetail(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUNT_ERROR));
        return noticeMapper.noticeDetailResponseDTO(notice);
    }

    @Transactional
    @Override
    public void updateNotice(Long noticeId, NoticeRegisterDTO dto) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUNT_ERROR));
        notice.updateNotice(dto.getNoticeContent(), dto.getNoticeTitle(), dto.getNoticeType());
        log.info("공지사항 수정: {}", notice);
    }

    @Transactional
    @Override
    public void deleteNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUNT_ERROR));
        notice.softDelete();
        log.info("공지사항 삭제: {}", notice);
    }
}
