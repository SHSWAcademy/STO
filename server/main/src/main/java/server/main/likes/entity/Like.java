package server.main.likes.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.main.asset.entity.Asset;
import server.main.global.util.BaseEntity;
import server.main.member.entity.Member;

@Entity
@Getter
@Table(
        name = "likes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_likes_member_asset", columnNames = {"member_id", "asset_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Like extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Long likeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    private Like(Member member, Asset asset) {
        this.member = member;
        this.asset = asset;
    }

    public static Like create(Member member, Asset asset) {
        return new Like(member, asset);
    }
}
