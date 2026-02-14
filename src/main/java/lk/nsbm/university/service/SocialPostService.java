package lk.nsbm.university.service;

import org.springframework.transaction.annotation.Transactional;
import lk.nsbm.university.entity.SocialCommentStatus;
import lk.nsbm.university.dto.SocialReactionSummary;
import lk.nsbm.university.entity.SocialPost;
import lk.nsbm.university.entity.SocialPostComment;
import lk.nsbm.university.entity.SocialPostMedia;
import lk.nsbm.university.entity.SocialPostReaction;
import lk.nsbm.university.entity.SocialPostStatus;
import lk.nsbm.university.entity.SocialReactionType;
import lk.nsbm.university.entity.SocialVisibility;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.repository.SocialPostCommentRepository;
import lk.nsbm.university.repository.SocialPostReactionRepository;
import lk.nsbm.university.repository.SocialPostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SocialPostService {

    private static final int MAX_MEDIA_ATTACHMENTS = 4;

    private final SocialPostRepository postRepository;
    private final SocialPostReactionRepository reactionRepository;
    private final SocialPostCommentRepository commentRepository;
    private final MediaStorageService mediaStorageService;

    public SocialPostService(SocialPostRepository postRepository,
                             SocialPostReactionRepository reactionRepository,
                             SocialPostCommentRepository commentRepository,
                             MediaStorageService mediaStorageService) {
        this.postRepository = postRepository;
        this.reactionRepository = reactionRepository;
        this.commentRepository = commentRepository;
        this.mediaStorageService = mediaStorageService;
    }

    @Transactional
    public SocialPost createPost(User author,
                                 String body,
                                 SocialVisibility visibility,
                                 Long semesterId,
                                 Long facultyId,
                                 List<MultipartFile> mediaFiles) {
        if (author == null) {
            throw new IllegalArgumentException("Author is required");
        }
        String sanitizedBody = sanitizeContent(body);
        if (!StringUtils.hasText(sanitizedBody)) {
            throw new IllegalArgumentException("Share an update before publishing");
        }
        SocialVisibility safeVisibility = visibility != null ? visibility : SocialVisibility.PUBLIC;
        validateAudience(author, safeVisibility, semesterId, facultyId);

        SocialPost post = new SocialPost();
        post.setAuthor(author);
        post.setBody(sanitizedBody);
        post.setVisibility(safeVisibility);
        if (safeVisibility == SocialVisibility.SEMESTER) {
            post.setTargetSemesterId(resolveSemesterTarget(author, semesterId));
        }
        if (safeVisibility == SocialVisibility.FACULTY) {
            post.setTargetFacultyId(resolveFacultyTarget(author, facultyId));
        }
        SocialPost persisted = postRepository.save(post);
        attachMedia(persisted, mediaFiles);
        return persisted;
    }

    @Transactional
    public void reactToPost(Long postId, User user, SocialReactionType reactionType) {
        SocialPost post = requireActivePost(postId);
        Objects.requireNonNull(user, "User is required");
        SocialReactionType safeType = reactionType != null ? reactionType : SocialReactionType.LIKE;
        Optional<SocialPostReaction> existing = reactionRepository.findByPostIdAndUserId(postId, user.getId());
        if (existing.isPresent()) {
            SocialPostReaction current = existing.get();
            SocialReactionType currentType = normalizeReactionType(current.getReactionType());
            if (currentType == safeType) {
                reactionRepository.delete(current);
            } else {
                current.setReactionType(safeType);
                reactionRepository.save(current);
            }
        } else {
            reactionRepository.save(new SocialPostReaction(post, user, safeType));
        }
    }

    @Transactional
    public SocialPostComment addComment(Long postId, User user, String body, Long parentCommentId) {
        SocialPost post = requireActivePost(postId);
        Objects.requireNonNull(user, "User is required");
        String sanitizedBody = sanitizeContent(body);
        if (!StringUtils.hasText(sanitizedBody)) {
            throw new IllegalArgumentException("Comments cannot be empty");
        }
        SocialPostComment comment = new SocialPostComment();
        comment.setPost(post);
        comment.setAuthor(user);
        comment.setBody(sanitizedBody);
        if (parentCommentId != null) {
            SocialPostComment parent = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));
            if (!Objects.equals(parent.getPost().getId(), postId)) {
                throw new IllegalArgumentException("Parent comment mismatch");
            }
            comment.setParent(parent);
        }
        return commentRepository.save(comment);
    }

    @Transactional
    public void removeComment(Long commentId, User user) {
        SocialPostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        if (!Objects.equals(comment.getAuthor().getId(), user.getId())
                && !isAdministrative(user)) {
            throw new IllegalArgumentException("You cannot remove this comment");
        }
        comment.setStatus(SocialCommentStatus.REMOVED);
        commentRepository.save(comment);
    }

    @Transactional
    public void archivePost(Long postId, User requester) {
        SocialPost post = requireActivePost(postId);
        if (!Objects.equals(post.getAuthor().getId(), requester.getId())
                && !isAdministrative(requester)) {
            throw new IllegalArgumentException("You cannot archive this post");
        }
        post.setStatus(SocialPostStatus.ARCHIVED);
        postRepository.save(post);
    }

    @Transactional
    public Page<SocialPost> getFeed(User user, Pageable pageable) {
        if (user == null) {
            throw new IllegalArgumentException("User context is required");
        }
        Page<SocialPost> page;
        if (isAdministrative(user)) {
            page = postRepository.findByStatusOrderByCreatedAtDesc(SocialPostStatus.ACTIVE, pageable);
        } else {
            Long semesterId = user.getSemester() != null ? user.getSemester().getId() : null;
            Long facultyId = user.getFaculty() != null ? user.getFaculty().getId() : null;
            page = postRepository.findFeedForUser(SocialPostStatus.ACTIVE, semesterId, facultyId, user.getId(), pageable);
        }
        // Force-load author/media before leaving the transaction so Thymeleaf rendering stays safe.
        page.getContent().forEach(this::primePostForView);
        return page;
    }

    @Transactional(readOnly = true)
    public long countActivePosts() {
        return postRepository.countByStatus(SocialPostStatus.ACTIVE);
    }

    @Transactional
    public void deletePost(Long postId, User requester) {
        if (requester == null) {
            throw new IllegalArgumentException("Login required");
        }
        SocialPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        if (!canDeletePost(post, requester)) {
            throw new IllegalArgumentException("You can only remove posts you published");
        }
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public boolean canDeletePost(SocialPost post, User requester) {
        if (post == null || requester == null) {
            return false;
        }
        if (isAdministrative(requester)) {
            return true;
        }
        return post.getAuthor() != null
                && post.getAuthor().getId() != null
                && post.getAuthor().getId().equals(requester.getId());
    }

    @Transactional
    public Page<SocialPost> getProfilePosts(User profileOwner, Pageable pageable) {
        Objects.requireNonNull(profileOwner, "Profile owner is required");
        Page<SocialPost> page = postRepository.findByAuthorAndStatusOrderByCreatedAtDesc(profileOwner, SocialPostStatus.ACTIVE, pageable);
        // Same hydration for profile feeds to avoid lazy-loading issues in the view layer.
        page.getContent().forEach(this::primePostForView);
        return page;
    }

    @Transactional
    public Map<Long, List<SocialPostComment>> getCommentsGrouped(Collection<Long> postIds) {
        if (CollectionUtils.isEmpty(postIds)) {
            return Map.of();
        }
        List<SocialPostComment> comments = commentRepository
                .findByPostIdInAndStatusOrderByCreatedAtAsc(postIds, SocialCommentStatus.ACTIVE);
        Map<Long, List<SocialPostComment>> grouped = new HashMap<>();
        for (SocialPostComment comment : comments) {
            grouped.computeIfAbsent(comment.getPost().getId(), key -> new ArrayList<>()).add(comment);
            // prime lazy fields that templates expect
            comment.getAuthor().getStudentId();
        }
        // guarantee every requested post id maps to a non-null comment list so the view layer stays safe
        for (Long postId : postIds) {
            grouped.computeIfAbsent(postId, key -> new ArrayList<>());
        }
        return grouped;
    }

    @Transactional
    public Map<Long, SocialReactionSummary> getReactionSummaries(Collection<Long> postIds) {
        if (CollectionUtils.isEmpty(postIds)) {
            return Map.of();
        }
        List<SocialPostReaction> reactions = reactionRepository.findByPostIdIn(postIds);
        Map<Long, long[]> aggregates = new HashMap<>();
        for (SocialPostReaction reaction : reactions) {
            SocialReactionType type = normalizeReactionType(reaction.getReactionType());
            long[] bucket = aggregates.computeIfAbsent(reaction.getPost().getId(), key -> new long[2]);
            if (type == SocialReactionType.DISLIKE) {
                bucket[1]++;
            } else {
                bucket[0]++;
            }
        }
        Map<Long, SocialReactionSummary> summaries = new HashMap<>();
        for (Long postId : postIds) {
            long[] bucket = aggregates.getOrDefault(postId, new long[2]);
            summaries.put(postId, new SocialReactionSummary(bucket[0], bucket[1]));
        }
        return summaries;
    }

    @Transactional
    public Map<Long, String> getUserReactions(Collection<Long> postIds, User user) {
        if (user == null || CollectionUtils.isEmpty(postIds)) {
            return Map.of();
        }
        return reactionRepository.findByPostIdInAndUserId(postIds, user.getId())
                .stream()
                .collect(Collectors.toMap(
                        reaction -> reaction.getPost().getId(),
                        reaction -> normalizeReactionType(reaction.getReactionType()).name(),
                        (existing, replacement) -> replacement));
    }

    public List<SocialVisibility> getAvailableVisibilities(User user) {
        EnumSet<SocialVisibility> visibilities = EnumSet.of(SocialVisibility.PUBLIC, SocialVisibility.PROFILE);
        if (user != null && user.getSemester() != null) {
            visibilities.add(SocialVisibility.SEMESTER);
        }
        if (user != null && user.getFaculty() != null) {
            visibilities.add(SocialVisibility.FACULTY);
        }
        if (isAdministrative(user)) {
            visibilities = EnumSet.allOf(SocialVisibility.class);
        }
        return new ArrayList<>(visibilities);
    }

    private String sanitizeContent(String value) {
        return value == null ? "" : value.strip();
    }

    private void attachMedia(SocialPost post, List<MultipartFile> mediaFiles) {
        if (CollectionUtils.isEmpty(mediaFiles)) {
            return;
        }
        List<MultipartFile> limited = mediaFiles.stream()
                .filter(file -> file != null && !file.isEmpty())
                .limit(MAX_MEDIA_ATTACHMENTS)
                .collect(Collectors.toList());
        for (MultipartFile file : limited) {
            String storedPath = mediaStorageService.store(file, "social");
            SocialPostMedia asset = new SocialPostMedia(post, storedPath, file.getContentType());
            post.getMedia().add(asset);
        }
        postRepository.save(post);
    }

    private void validateAudience(User author, SocialVisibility visibility, Long semesterId, Long facultyId) {
        if (visibility == SocialVisibility.SEMESTER && author.getSemester() == null && semesterId == null) {
            throw new IllegalArgumentException("Assign yourself to a semester before targeting it");
        }
        if (visibility == SocialVisibility.FACULTY && author.getFaculty() == null && facultyId == null) {
            throw new IllegalArgumentException("Assign yourself to a faculty before targeting it");
        }
    }

    private Long resolveSemesterTarget(User author, Long providedSemesterId) {
        if (providedSemesterId != null) {
            return providedSemesterId;
        }
        return author.getSemester() != null ? author.getSemester().getId() : null;
    }

    private Long resolveFacultyTarget(User author, Long providedFacultyId) {
        if (providedFacultyId != null) {
            return providedFacultyId;
        }
        return author.getFaculty() != null ? author.getFaculty().getId() : null;
    }

    private SocialPost requireActivePost(Long postId) {
        SocialPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        if (post.getStatus() != SocialPostStatus.ACTIVE) {
            throw new IllegalArgumentException("Post is archived");
        }
        return post;
    }

    private boolean isAdministrative(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return switch (user.getRole()) {
            case SUPER_ADMIN, ADMIN, MODERATOR, CONTENT_MANAGER, EDITOR -> true;
            default -> false;
        };
    }

    private void primePostForView(SocialPost post) {
        if (post == null) {
            return;
        }
        if (post.getAuthor() != null) {
            post.getAuthor().getStudentId();
        }
        if (post.getMedia() != null) {
            post.getMedia().forEach(media -> media.getStoragePath());
        }
    }

    private SocialReactionType normalizeReactionType(SocialReactionType type) {
        return type != null ? type : SocialReactionType.LIKE;
    }
}
