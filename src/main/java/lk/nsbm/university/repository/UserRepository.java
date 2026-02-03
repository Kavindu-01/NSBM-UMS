package lk.nsbm.university.repository;

import java.util.List;

import lk.nsbm.university.entity.AccountStatus;
import lk.nsbm.university.entity.Batch;
import lk.nsbm.university.entity.Role;
import lk.nsbm.university.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByStudentId(String studentId);
    
    boolean existsByStudentId(String studentId);
    
    Page<User> findByAccountStatus(AccountStatus status, Pageable pageable);

    Page<User> findByRole(Role role, Pageable pageable);

    List<User> findByRole(Role role);

    Page<User> findByRoleAndAccountStatus(Role role, AccountStatus status, Pageable pageable);

    List<User> findByBatch(Batch batch);

    long countByAccountStatus(AccountStatus status);
}
