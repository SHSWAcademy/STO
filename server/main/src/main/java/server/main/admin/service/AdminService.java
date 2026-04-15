package server.main.admin.service;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import server.main.admin.dto.*;

import java.util.List;

public interface AdminService {
    void registerAsset(AssetRegisterRequestDTO dto, MultipartFile imageFile, MultipartFile pdfFile);       // 자산등록
    AssetDetailResponseDTO getAssetDetail(Long assetId);   // 자산 상세조회
    List<AssetListResponseDTO> getAssetList();             // 자산 리스트 조회
    void updateAsset(Long assetId, AssetUpdateRequestDTO dto, MultipartFile imageFile, MultipartFile pdfFile);   // 자산 수정
    List<AllocationListResponseDTO> getAllocationList();   // 배당 리스트 조회
    void registerAllocation(AllocationRegisterRequestDTO dto, MultipartFile file); // 배당 등록
    List<AllocationDetailResponseDTO> getAllocationDetailList(Long assetId);        // 배당 스케줄내역 상세조회 리스트
    void updateAllocation(Long allocationEventId, AllocationUpdateRequestDTO dto, MultipartFile file);    // 배당 스케줄 수정
    PlatformProfitAccountResponseDTO getPlatformProfitAccount();        // 플랫폼 수익/보유 현황 조회
    void registerCommon(CommonDTO dto);  // 플랫폼 기초설정
    CommonDTO getCommon();  // 플랫폼 기초설정 조회
    Page<MemberListResponseDTO> getMemberList(int page, int size);  // 멤버 리스트 조회
    void updateMember(Long memberId, boolean isActive);   // 멤버 활성/비활성화 처리
    DashBoardResponseDTO getDashBoard();    // 대시보드 데이터 조회
    Page<DashBoardTradeListDTO> getDashBoardTradeList(int page, int size);  // 대시보드 거래내역 데이터 조회
    Page<SystemLogResponseDTO> getSystemLong(String category, int page, int size);  // 로그관리 데이터 조회
}
