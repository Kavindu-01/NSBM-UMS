package lk.nsbm.university.service;

import lk.nsbm.university.entity.Club;
import lk.nsbm.university.entity.ClubMembership;
import lk.nsbm.university.entity.Role;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.repository.ClubMembershipRepository;
import lk.nsbm.university.repository.ClubRepository;
import lk.nsbm.university.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public ClubService(ClubRepository clubRepository,
                       ClubMembershipRepository membershipRepository,
                       UserRepository userRepository) {
        this.clubRepository = clubRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<Club> getClubs(Pageable pageable) {
        return clubRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Club> getAllClubs() {
        return clubRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Club> getClubsManagedBy(Long presidentId) {
        if (presidentId == null) {
            return List.of();
        }
        return clubRepository.findByPresident_Id(presidentId);
    }

    @Transactional
    public Club createClub(String name,
                           String tagline,
                           String description,
                           String whatsappLink,
                           String coverImageUrl,
                           Long presidentId,
                           User createdBy) {
        validateClubFields(name, tagline);
        if (clubRepository.findByNameIgnoreCase(name.trim()).isPresent()) {
            throw new IllegalArgumentException("A club with this name already exists");
        }
        Club club = new Club();
        populateClub(club, name, tagline, description, whatsappLink, coverImageUrl);
        if (presidentId != null) {
            club.setPresident(resolvePresident(presidentId));
        }
        club.setCreatedBy(createdBy);
        return clubRepository.save(club);
    }

    @Transactional
    public Club updateClub(Long clubId,
                           String name,
                           String tagline,
                           String description,
                           String whatsappLink,
                           String coverImageUrl,
                           Long presidentId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found"));
        if (StringUtils.hasText(name) && !club.getName().equalsIgnoreCase(name.trim())) {
            if (clubRepository.findByNameIgnoreCase(name.trim()).isPresent()) {
                throw new IllegalArgumentException("Another club already uses that name");
            }
        }
        populateClub(club, name, tagline, description, whatsappLink, coverImageUrl);
        if (presidentId != null) {
            club.setPresident(resolvePresident(presidentId));
        }
        return clubRepository.save(club);
    }

    @Transactional
    public void assignPresident(Long clubId, Long presidentId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found"));
        club.setPresident(resolvePresident(presidentId));
        clubRepository.save(club);
    }

    @Transactional(readOnly = true)
    public Optional<Club> getClub(Long clubId) {
        return clubRepository.findById(clubId);
    }

    @Transactional
    public void deleteClub(Long clubId) {
        membershipRepository.findByClubId(clubId).forEach(membershipRepository::delete);
        clubRepository.deleteById(clubId);
    }

    @Transactional
    public void joinClub(Long clubId, User user) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found"));
        if (membershipRepository.existsByClubAndUser(club, user)) {
            return;
        }
        ClubMembership membership = new ClubMembership();
        membership.setClub(club);
        membership.setUser(user);
        membershipRepository.save(membership);
    }

    @Transactional
    public void leaveClub(Long clubId, User user) {
        membershipRepository.findByClubIdAndUserId(clubId, user.getId())
                .ifPresent(membershipRepository::delete);
    }

    @Transactional(readOnly = true)
    public Set<Long> getMembershipClubIds(User user) {
        List<ClubMembership> memberships = membershipRepository.findByUserId(user.getId());
        Set<Long> clubIds = new HashSet<>();
        for (ClubMembership membership : memberships) {
            clubIds.add(membership.getClub().getId());
        }
        return clubIds;
    }

    @Transactional(readOnly = true)
    public List<ClubMembership> getMembershipsForClub(Long clubId) {
        return membershipRepository.findByClubId(clubId);
    }

    private void populateClub(Club club,
                              String name,
                              String tagline,
                              String description,
                              String whatsappLink,
                              String coverImageUrl) {
        if (StringUtils.hasText(name)) {
            club.setName(name.trim());
        }
        if (StringUtils.hasText(tagline)) {
            club.setTagline(tagline.trim());
        }
        club.setDescription(StringUtils.hasText(description) ? description.trim() : null);
        club.setWhatsappLink(StringUtils.hasText(whatsappLink) ? whatsappLink.trim() : null);
        club.setCoverImageUrl(StringUtils.hasText(coverImageUrl) ? coverImageUrl.trim() : null);
    }

    private void validateClubFields(String name, String tagline) {
        if (!StringUtils.hasText(name) || !StringUtils.hasText(tagline)) {
            throw new IllegalArgumentException("Club name and tagline are required");
        }
    }

    private User resolvePresident(Long presidentId) {
        User president = userRepository.findById(presidentId)
                .orElseThrow(() -> new IllegalArgumentException("Club president account not found"));
        if (president.getRole() != Role.CLUB_PRESIDENT) {
            throw new IllegalArgumentException("Selected user is not a club president account");
        }
        return president;
    }
}
