package server.main.member.service;

import server.main.member.entity.Member;

public interface MemberService {
    Member getMyInfo(Long memberId);
}
