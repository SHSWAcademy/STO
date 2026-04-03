package server.main.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import server.main.global.security.CustomUserPrincipal;
import server.main.member.dto.MemberMeResponse;
import server.main.member.entity.Member;
import server.main.member.service.MemberService;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    
    @GetMapping("/me")
    public ResponseEntity<MemberMeResponse> getMyInfo(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        Member member = memberService.getMyInfo(principal.getId());
        MemberMeResponse response = new MemberMeResponse(
                member.getMemberId(),
                member.getEmail(),
                member.getMemberName(),
                principal.getRole()
        );
        return ResponseEntity.ok(response);
    }
    

}



