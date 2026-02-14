package lk.nsbm.university.repository;

import lk.nsbm.university.entity.ClubPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface ClubPostRepository extends JpaRepository<ClubPost, Long> {

    Page<ClubPost> findByClubId(Long clubId, Pageable pageable);

    Page<ClubPost> findByClubIdIn(Collection<Long> clubIds, Pageable pageable);
}
