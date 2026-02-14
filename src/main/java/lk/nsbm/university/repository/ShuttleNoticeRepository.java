package lk.nsbm.university.repository;

import lk.nsbm.university.entity.ShuttleNotice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShuttleNoticeRepository extends JpaRepository<ShuttleNotice, Long> {

    List<ShuttleNotice> findTop20ByOrderByCreatedAtDesc();
}
