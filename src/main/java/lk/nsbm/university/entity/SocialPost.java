package lk.nsbm.university.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "social_posts")
public class SocialPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialVisibility visibility = SocialVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialPostStatus status = SocialPostStatus.ACTIVE;

    @Column(nullable = false)
    private boolean pinned = false;

    @Column(name = "target_semester_id")
    private Long targetSemesterId;

    @Column(name = "target_faculty_id")
    private Long targetFacultyId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SocialPostMedia> media = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SocialPostReaction> reactions = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SocialPostComment> comments = new ArrayList<>();

    public SocialPost() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public SocialVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(SocialVisibility visibility) {
        this.visibility = visibility;
    }

    public SocialPostStatus getStatus() {
        return status;
    }

    public void setStatus(SocialPostStatus status) {
        this.status = status;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public Long getTargetSemesterId() {
        return targetSemesterId;
    }

    public void setTargetSemesterId(Long targetSemesterId) {
        this.targetSemesterId = targetSemesterId;
    }

    public Long getTargetFacultyId() {
        return targetFacultyId;
    }

    public void setTargetFacultyId(Long targetFacultyId) {
        this.targetFacultyId = targetFacultyId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<SocialPostMedia> getMedia() {
        return media;
    }

    public void setMedia(List<SocialPostMedia> media) {
        this.media = media;
    }

    public List<SocialPostReaction> getReactions() {
        return reactions;
    }

    public void setReactions(List<SocialPostReaction> reactions) {
        this.reactions = reactions;
    }

    public List<SocialPostComment> getComments() {
        return comments;
    }

    public void setComments(List<SocialPostComment> comments) {
        this.comments = comments;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
