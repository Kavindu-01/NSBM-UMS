package lk.nsbm.university.repository;

import lk.nsbm.university.entity.ContentReaction;
import lk.nsbm.university.entity.ContentReactionType;
import lk.nsbm.university.repository.projection.ContentReactionCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContentReactionRepository extends JpaRepository<ContentReaction, Long> {

    Optional<ContentReaction> findByContentIdAndUserId(Long contentId, Long userId);

    List<ContentReaction> findByContentIdInAndUserId(Collection<Long> contentIds, Long userId);

    @Query("select r.content.id as contentId, r.reactionType as reactionType, count(r) as total " +
            "from ContentReaction r where r.content.id in :contentIds group by r.content.id, r.reactionType")
    List<ContentReactionCountProjection> countByContentIds(Collection<Long> contentIds);

    long countByContentIdAndReactionType(Long contentId, ContentReactionType reactionType);
}
