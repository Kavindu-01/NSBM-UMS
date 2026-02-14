package lk.nsbm.university.service;

import lk.nsbm.university.entity.Announcement;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.repository.AnnouncementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    @Transactional(readOnly = true)
    public Page<Announcement> getAnnouncements(Pageable pageable) {
        return announcementRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Announcement getAnnouncement(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Announcement not found"));
    }

    @Transactional(readOnly = true)
    public List<Announcement> getLatestAnnouncements() {
        return announcementRepository.findTop5ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public long countAllAnnouncements() {
        return announcementRepository.count();
    }

    @Transactional
    public Announcement createAnnouncement(Announcement announcement, User createdBy) {
        announcement.setCreatedBy(createdBy);
        return announcementRepository.save(announcement);
    }

    @Transactional
    public Announcement updateAnnouncement(Long announcementId, Announcement updated, User updatedBy) {
        Announcement existing = getAnnouncement(announcementId);
        existing.setTitle(updated.getTitle());
        existing.setContent(updated.getContent());
        existing.setImportant(Boolean.TRUE.equals(updated.getImportant()));
        existing.setCreatedBy(updatedBy);
        return announcementRepository.save(existing);
    }

    @Transactional
    public void deleteAnnouncement(Long announcementId) {
        announcementRepository.deleteById(announcementId);
    }
}
