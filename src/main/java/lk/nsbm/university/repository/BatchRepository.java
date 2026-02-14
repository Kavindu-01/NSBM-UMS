package lk.nsbm.university.repository;

import lk.nsbm.university.entity.Batch;
import lk.nsbm.university.entity.Semester;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {
    
    Optional<Batch> findByName(String name);
    
    boolean existsByName(String name);
    
    Page<Batch> findAll(Pageable pageable);
    
    List<Batch> findBySemester(Semester semester);
}
