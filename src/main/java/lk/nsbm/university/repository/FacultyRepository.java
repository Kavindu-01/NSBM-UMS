package lk.nsbm.university.repository;

import lk.nsbm.university.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByNameIgnoreCase(String name);
    Optional<Faculty> findByCodeIgnoreCase(String code);
    Optional<Faculty> findByNameIgnoreCase(String name);
}
