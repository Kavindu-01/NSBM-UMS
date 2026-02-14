package lk.nsbm.university.repository;

import lk.nsbm.university.entity.SocialCommentStatus;
import lk.nsbm.university.entity.SocialPostComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SocialPostCommentRepository extends JpaRepository<SocialPostComment, Long> {

    List<SocialPostComment> findByPostIdAndStatusOrderByCreatedAtAsc(Long postId, SocialCommentStatus status);

    List<SocialPostComment> findByPostIdInAndStatusOrderByCreatedAtAsc(Collection<Long> postIds,
                                                                      SocialCommentStatus status);
}
