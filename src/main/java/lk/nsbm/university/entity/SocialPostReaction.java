package lk.nsbm.university.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "social_post_reactions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
public class SocialPostReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private SocialPost post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false)
    private SocialReactionType reactionType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public SocialPostReaction() {
    }

    public SocialPostReaction(SocialPost post, User user) {
        this(post, user, SocialReactionType.LIKE);
    }

    public SocialPostReaction(SocialPost post, User user, SocialReactionType reactionType) {
        this.post = post;
        this.user = user;
        this.reactionType = reactionType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SocialPost getPost() {
        return post;
    }

    public void setPost(SocialPost post) {
        this.post = post;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public SocialReactionType getReactionType() {
        return reactionType;
    }

    public void setReactionType(SocialReactionType reactionType) {
        this.reactionType = reactionType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (reactionType == null) {
            reactionType = SocialReactionType.LIKE;
        }
    }
}
