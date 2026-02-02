package lk.nsbm.university.service;

import lk.nsbm.university.entity.Club;
import lk.nsbm.university.entity.ClubPost;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.repository.ClubPostRepository;
import lk.nsbm.university.repository.ClubRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;

@Service
public class ClubPostService {

    private final ClubPostRepository postRepository;
    private final ClubRepository clubRepository;

    public ClubPostService(ClubPostRepository postRepository,
                           ClubRepository clubRepository) {
        this.postRepository = postRepository;
        this.clubRepository = clubRepository;
    }

    @Transactional
    public ClubPost createPost(Long clubId,
                               String title,
                               String content,
                               String imageUrl,
                               User createdBy) {
        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            throw new IllegalArgumentException("Title and content are required");
        }
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found"));
        ClubPost post = new ClubPost();
        post.setClub(club);
        post.setTitle(title.trim());
        post.setContent(content.trim());
        post.setImageUrl(StringUtils.hasText(imageUrl) ? imageUrl.trim() : null);
        post.setCreatedBy(createdBy);
        return postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public Page<ClubPost> getPostsForClub(Long clubId, Pageable pageable) {
        return postRepository.findByClubId(clubId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ClubPost> getPostsForClubs(Collection<Long> clubIds, Pageable pageable) {
        if (clubIds == null || clubIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return postRepository.findByClubIdIn(clubIds, pageable);
    }
}
