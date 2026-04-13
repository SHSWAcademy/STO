package server.main.likes.service;

import server.main.likes.dto.LikeResponseDto;

import java.util.List;

public interface LikeService {
    List<LikeResponseDto> getMyLikes(Long memberId);
    void addLike(Long memberId, Long tokenId);
    void removeLike(Long memberId, Long tokenId);
}
