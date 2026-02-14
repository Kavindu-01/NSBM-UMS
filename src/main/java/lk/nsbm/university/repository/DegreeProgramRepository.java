package lk.nsbm.university.repository;

import lk.nsbm.university.entity.DegreeProgram;
import lk.nsbm.university.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DegreeProgramRepository extends JpaRepository<DegreeProgram, Long> {
    List<DegreeProgram> findByFacultyOrderByNameAsc(Faculty faculty);
    boolean existsByCodeIgnoreCase(String code);
    Optional<DegreeProgram> findByCodeIgnoreCase(String code);
}
