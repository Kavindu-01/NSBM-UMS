package lk.nsbm.university.repository;

import lk.nsbm.university.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {

    Optional<Club> findByNameIgnoreCase(String name);

    boolean existsByPresident_Id(Long presidentId);

    List<Club> findByPresident_Id(Long presidentId);
}
