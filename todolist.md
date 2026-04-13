# STO 프로젝트 작업 내역

## 상세 페이지 (Token Detail Page)

### 백엔드

#### DTO / 구조
- [x] `TokenDetailDto` → `TokenChartDetailResponseDto` 이름 변경 및 사이드이펙트 수정
- [x] `TokenMapper` - `asset.assetName`, `asset.imgUrl` 매핑 추가

#### API 엔드포인트 (`/api/token/{tokenId}`)
- [x] `GET /chart` - 차트, 호가 데이터 (`getTokenDetail`)
- [x] `GET /info` - 종목 정보 (`getTokenAssetInfo`)
- [x] `GET /allocation` - 배당금 내역 (`getAllocationInfo`)
- [x] `GET /disclosure` - 공시 (`getDisclosureInfo`)

#### 서비스 로직 개선
- [x] `getTokenAssetInfo` - `findById` → `findByIdWithAsset` 수정 (N+1 방지)
- [x] `getAllocationInfo` - `findById` → `findByIdWithAsset` 수정
- [x] `getAllocationInfo` - `totalSupply` null/0 방어 코드 추가
- [x] `getAllocationInfo` - 배당 내역 최신순 정렬 (`OrderBySettledAtDesc`)
- [x] 미사용 `AllocationPayoutRepository`, `AssetRepository`, `AssetMapper` 제거

#### Repository
- [x] `AllocationEventRepository` - `findAllByAssetIdOrderBySettledAtDesc` 추가
- [x] `DisclosureRepository` - `findAllByAssetId` 추가

#### 엔티티
- [x] `Account` - `cancelOrder(Long amount)` 메서드 추가 (매수 취소 시 묶인 금액 해제)

#### 테스트
- [x] `TokenServiceImplTest` 작성 (총 13개)
  - `getTokenDetail` - 정상 조회, 토큰 없음 예외
  - `getTokenAssetInfo` - 정상 조회, 토큰 없음 예외, BUILDING 공시 없음 예외
  - `getAllocationInfo` - 정상 조회, 빈 리스트, totalSupply=0, totalSupply=null, 토큰 없음 예외
  - `getDisclosureInfo` - 파일 있는 공시, 파일 없는 공시(OriginName null), 빈 리스트, 토큰 없음 예외

### 프론트엔드
- [x] `/mockup/:tokenId` 라우트 - 비로그인 접근 허용 (Auth 가드 우회)
- [x] `MockupPage` - `/api/token/{tokenId}/chart` 엔드포인트 연동

---

## 진행 예정
- [ ] 상세 페이지 프론트엔드 - 종목 정보 탭 연동 (`/info`)
- [ ] 상세 페이지 프론트엔드 - 배당금 내역 탭 연동 (`/allocation`)
- [ ] 상세 페이지 프론트엔드 - 공시 탭 연동 (`/disclosure`)
