package server.main.likes.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.main.global.security.CustomUserPrincipal;
import server.main.likes.dto.LikeResponseDto;
import server.main.likes.service.LikeService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/likes")
public class LikeController {

    private final LikeService likeService;

    @GetMapping
    public ResponseEntity<List<LikeResponseDto>> getMyLikes(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(likeService.getMyLikes(principal.getId()));
    }

    @PostMapping("/{tokenId}")
    public ResponseEntity<Void> addLike(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long tokenId) {
        likeService.addLike(principal.getId(), tokenId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{tokenId}")
    public ResponseEntity<Void> removeLike(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long tokenId) {
        likeService.removeLike(principal.getId(), tokenId);
        return ResponseEntity.noContent().build();
    }
}
