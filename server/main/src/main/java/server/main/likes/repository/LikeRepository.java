package server.main.likes.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.likes.entity.Like;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    boolean existsByMember_MemberIdAndAsset_AssetId(Long memberId, Long assetId);

    Optional<Like> findByMember_MemberIdAndAsset_AssetId(Long memberId, Long assetId);

    @Query("""
            select l
            from Like l
            join fetch l.asset a
            where l.member.memberId = :memberId
            order by l.createdAt desc
            """)
    List<Like> findAllByMemberIdWithAsset(@Param("memberId") Long memberId);
}
