package lk.nsbm.university.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "social_post_media")
public class SocialPostMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private SocialPost post;

    @Column(nullable = false, length = 512)
    private String storagePath;

    @Column(length = 120)
    private String mediaType;

    public SocialPostMedia() {
    }

    public SocialPostMedia(SocialPost post, String storagePath, String mediaType) {
        this.post = post;
        this.storagePath = storagePath;
        this.mediaType = mediaType;
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

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }
}
