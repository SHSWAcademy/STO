package server.main.member.service;

import server.main.member.dto.MemberMeResponse;

public interface MemberService {
    MemberMeResponse getMyInfo(Long memberId, String role);
}
