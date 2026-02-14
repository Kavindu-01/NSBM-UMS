package lk.nsbm.university.repository;

import lk.nsbm.university.entity.Club;
import lk.nsbm.university.entity.ClubMembership;
import lk.nsbm.university.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClubMembershipRepository extends JpaRepository<ClubMembership, Long> {

    Optional<ClubMembership> findByClubIdAndUserId(Long clubId, Long userId);

    List<ClubMembership> findByUserId(Long userId);

    List<ClubMembership> findByClubId(Long clubId);

    boolean existsByClubAndUser(Club club, User user);

    List<ClubMembership> findByClubIdIn(Collection<Long> clubIds);
}
