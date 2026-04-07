package server.main.asset.service;

import server.main.asset.entity.Asset;
import server.main.token.entity.Token;

public interface AssetService {
    Asset AssetRegister(Asset asset);        // 자산등록 (admin)
    Asset findById(Long assetId);           // 기존 자산조회 (admin)
    String findAssetName(Long assetId);     // 자산 이름조회 (admin)
    void AssetAccountRegister(Token token); // 자산 계좌 생성 (admin)
    void AllocationAccountDeposit(Long amount, Long assetId);  // 배당 월수익 입금 처리 (admin)
}
