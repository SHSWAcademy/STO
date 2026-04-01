package server.main.asset.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.asset.entity.Asset;
import server.main.asset.mapper.AssetMapper;
import server.main.asset.repository.AssetRepository;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
@Slf4j
public class AssetService {
    private final AssetRepository assetRepository;
    private final AssetMapper assetMapper;

}
