package lk.nsbm.university.repository;

import lk.nsbm.university.entity.Content;
import lk.nsbm.university.entity.ContentCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface ContentRepository extends JpaRepository<Content, Long> {
    
    Page<Content> findAll(Pageable pageable);
    
    Page<Content> findByCategory(ContentCategory category, Pageable pageable);

    Page<Content> findByCategoryIn(Collection<ContentCategory> categories, Pageable pageable);
}
