package lk.nsbm.university.repository;

import java.util.List;

import lk.nsbm.university.entity.AccountStatus;
import lk.nsbm.university.entity.Batch;
import lk.nsbm.university.entity.DegreeProgram;
import lk.nsbm.university.entity.Faculty;
import lk.nsbm.university.entity.Role;
import lk.nsbm.university.entity.Semester;
import lk.nsbm.university.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Page<User> findByFacultyAndDegreeAndAccountStatusAndRole(Faculty faculty, DegreeProgram degree, AccountStatus status, Role role, Pageable pageable);
    Page<User> findByFacultyAndAccountStatusAndRole(Faculty faculty, AccountStatus status, Role role, Pageable pageable);
    Page<User> findByDegreeAndAccountStatusAndRole(DegreeProgram degree, AccountStatus status, Role role, Pageable pageable);
    
    Optional<User> findByStudentId(String studentId);

    Optional<User> findByContactNumber(String contactNumber);
    
    boolean existsByStudentId(String studentId);

    boolean existsByContactNumber(String contactNumber);
    
    Page<User> findByAccountStatus(AccountStatus status, Pageable pageable);

    Page<User> findByRole(Role role, Pageable pageable);

    List<User> findByRole(Role role);

    Page<User> findByRoleAndAccountStatus(Role role, AccountStatus status, Pageable pageable);

    Page<User> findByStudentIdContainingIgnoreCase(String studentId, Pageable pageable);

    Page<User> findByStudentIdContainingIgnoreCaseAndRole(String studentId, Role role, Pageable pageable);

    Page<User> findByStudentIdContainingIgnoreCaseAndAccountStatus(String studentId, AccountStatus status, Pageable pageable);

    Page<User> findByStudentIdContainingIgnoreCaseAndRoleAndAccountStatus(String studentId, Role role, AccountStatus status, Pageable pageable);

    Optional<User> findFirstByRole(Role role);

    long countByRole(Role role);

    List<User> findByBatch(Batch batch);

    long countByAccountStatus(AccountStatus status);

    long countByFaculty(Faculty faculty);

    long countByDegree(DegreeProgram degreeProgram);

    List<User> findByFaculty(Faculty faculty);

    List<User> findByDegree(DegreeProgram degreeProgram);

    List<User> findBySemesterAndAccountStatus(Semester semester, AccountStatus status);

    List<User> findByFacultyAndAccountStatus(Faculty faculty, AccountStatus status);

    List<User> findByDegreeAndAccountStatus(DegreeProgram degreeProgram, AccountStatus status);

    List<User> findByAccountStatus(AccountStatus status);
}
