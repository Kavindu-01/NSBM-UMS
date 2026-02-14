package lk.nsbm.university.repository;

import lk.nsbm.university.entity.SocialPost;
import lk.nsbm.university.entity.SocialPostStatus;
import lk.nsbm.university.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SocialPostRepository extends JpaRepository<SocialPost, Long> {

    Page<SocialPost> findByStatusOrderByCreatedAtDesc(SocialPostStatus status, Pageable pageable);

    long countByStatus(SocialPostStatus status);

    Page<SocialPost> findByAuthorAndStatusOrderByCreatedAtDesc(User author, SocialPostStatus status, Pageable pageable);

    @Query("""
            SELECT p FROM SocialPost p
            WHERE p.status = :status AND (
                p.visibility = lk.nsbm.university.entity.SocialVisibility.PUBLIC
                OR (:semesterId IS NOT NULL AND p.visibility = lk.nsbm.university.entity.SocialVisibility.SEMESTER AND p.targetSemesterId = :semesterId)
                OR (:facultyId IS NOT NULL AND p.visibility = lk.nsbm.university.entity.SocialVisibility.FACULTY AND p.targetFacultyId = :facultyId)
                OR (:userId IS NOT NULL AND p.author.id = :userId)
            )
            ORDER BY p.createdAt DESC
            """)
    Page<SocialPost> findFeedForUser(@Param("status") SocialPostStatus status,
                                     @Param("semesterId") Long semesterId,
                                     @Param("facultyId") Long facultyId,
                                     @Param("userId") Long userId,
                                     Pageable pageable);
}
