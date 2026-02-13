package lk.nsbm.university.repository;

import lk.nsbm.university.entity.Semester;
import lk.nsbm.university.entity.SemesterResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SemesterResourceRepository extends JpaRepository<SemesterResource, Long> {

    List<SemesterResource> findBySemesterOrderByCreatedAtDesc(Semester semester);

    List<SemesterResource> findBySemesterAndFacultyIdOrderByCreatedAtDesc(Semester semester, Long facultyId);

    List<SemesterResource> findBySemesterAndDegreeIdOrderByCreatedAtDesc(Semester semester, Long degreeId);

    List<SemesterResource> findBySemesterAndFacultyIdAndDegreeIdOrderByCreatedAtDesc(Semester semester, Long facultyId, Long degreeId);

    long countBySemester(Semester semester);

    void deleteBySemester(Semester semester);
}
