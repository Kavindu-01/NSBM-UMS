package lk.nsbm.university.repository;

import lk.nsbm.university.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    
    Page<Announcement> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    List<Announcement> findTop5ByOrderByCreatedAtDesc();
    
    Page<Announcement> findByImportantTrue(Pageable pageable);
}
