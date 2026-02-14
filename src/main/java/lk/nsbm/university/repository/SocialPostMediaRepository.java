package lk.nsbm.university.repository;

import lk.nsbm.university.entity.SocialPostMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SocialPostMediaRepository extends JpaRepository<SocialPostMedia, Long> {

    List<SocialPostMedia> findByPostId(Long postId);
}
