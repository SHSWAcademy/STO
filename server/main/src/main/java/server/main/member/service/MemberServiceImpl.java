package server.main.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.member.dto.MemberMeResponse;
import server.main.member.entity.Member;
import server.main.member.repository.MemberRepository;
import server.main.member.repository.MemberTokenHoldingRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final MemberTokenHoldingRepository memberTokenHoldingRepository;

    @Override
    public MemberMeResponse getMyInfo(Long memberId, String role) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return new MemberMeResponse(
                member.getMemberId(),
                member.getEmail(),
                member.getMemberName(),
                role
        );
    }
}
