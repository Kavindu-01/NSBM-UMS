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
                               String resourceLabel,
                               String resourceUrl,
                               User createdBy) {
        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("Give your post a short title");
        }
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found"));
        boolean hasContent = StringUtils.hasText(content);
        boolean hasImage = StringUtils.hasText(imageUrl);
        boolean hasResource = StringUtils.hasText(resourceUrl);
        if (!hasContent && !hasImage && !hasResource) {
            throw new IllegalArgumentException("Share a message, image, or file before publishing");
        }
        String normalizedContent = hasContent ? content.trim() : null;
        if (normalizedContent == null) {
            if (hasResource) {
                normalizedContent = "New resource: " + (StringUtils.hasText(resourceLabel) ? resourceLabel.trim() : "Download the attached file");
            } else if (hasImage) {
                normalizedContent = "New gallery update";
            }
        }
        ClubPost post = new ClubPost();
        post.setClub(club);
        post.setTitle(title.trim());
        post.setContent(normalizedContent);
        post.setImageUrl(StringUtils.hasText(imageUrl) ? imageUrl.trim() : null);
        post.setResourceLabel(StringUtils.hasText(resourceLabel) ? resourceLabel.trim() : null);
        post.setResourceUrl(StringUtils.hasText(resourceUrl) ? resourceUrl.trim() : null);
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

    @Transactional
    public void deletePost(Long clubId, Long postId) {
        ClubPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        if (!post.getClub().getId().equals(clubId)) {
            throw new IllegalArgumentException("Post does not belong to this club");
        }
        postRepository.delete(post);
    }
}
