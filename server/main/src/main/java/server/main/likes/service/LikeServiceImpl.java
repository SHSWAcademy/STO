package server.main.likes.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.asset.entity.Asset;
import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.likes.dto.LikeResponseDto;
import server.main.likes.entity.Like;
import server.main.likes.repository.LikeRepository;
import server.main.member.entity.Member;
import server.main.member.repository.MemberRepository;
import server.main.token.entity.Token;
import server.main.token.repository.TokenRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final MemberRepository memberRepository;
    private final TokenRepository tokenRepository;

    @Override
    public List<LikeResponseDto> getMyLikes(Long memberId) {
        return likeRepository.findAllByMemberIdWithAsset(memberId)
                .stream()
                .map(like -> {
                    Token token = tokenRepository.findByAssetIdWithAsset(like.getAsset().getAssetId())
                            .orElse(null);

                    return LikeResponseDto.builder()
                            .tokenId(token != null ? token.getTokenId() : null)
                            .assetId(like.getAsset().getAssetId())
                            .assetName(like.getAsset().getAssetName())
                            .tokenSymbol(token != null ? token.getTokenSymbol() : null)
                            .imgUrl(token.getAsset().getImgUrl())
                            .currentPrice(token != null && token.getCurrentPrice() != null
                                    ? Math.round(token.getCurrentPrice())
                                    : 0L)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public void addLike(Long memberId, Long tokenId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다. memberId=" + memberId));

        Token token = tokenRepository.findByIdWithAsset(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("토큰이 존재하지 않습니다. tokenId=" + tokenId));

        Asset asset = token.getAsset();

        if (likeRepository.existsByMember_MemberIdAndAsset_AssetId(memberId, asset.getAssetId())) {
            throw new BusinessException(ErrorCode.WATCHLIST_ALREADY_EXISTS);
        }

        likeRepository.save(Like.create(member, asset));
    }

    @Override
    @Transactional
    public void removeLike(Long memberId, Long tokenId) {
        Token token = tokenRepository.findByIdWithAsset(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("토큰이 존재하지 않습니다. tokenId=" + tokenId));

        Long assetId = token.getAsset().getAssetId();

        Like like = likeRepository.findByMember_MemberIdAndAsset_AssetId(memberId, assetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WATCHLIST_NOT_FOUND));

        likeRepository.delete(like);
    }
}
