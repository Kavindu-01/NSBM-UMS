package lk.nsbm.university.repository;

import lk.nsbm.university.entity.BoardingListing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardingListingRepository extends JpaRepository<BoardingListing, Long> {

    List<BoardingListing> findTop20ByOrderByCreatedAtDesc();
}
