package lk.nsbm.university.repository;

import lk.nsbm.university.entity.Semester;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {
    
    Optional<Semester> findByName(String name);
    
    boolean existsByName(String name);
    
    List<Semester> findAll(Sort sort);
}
