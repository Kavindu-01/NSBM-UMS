package lk.nsbm.university.repository;

import lk.nsbm.university.entity.SocialPostReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SocialPostReactionRepository extends JpaRepository<SocialPostReaction, Long> {

    Optional<SocialPostReaction> findByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);

    List<SocialPostReaction> findByPostIdIn(Collection<Long> postIds);

    List<SocialPostReaction> findByPostIdInAndUserId(Collection<Long> postIds, Long userId);
}
