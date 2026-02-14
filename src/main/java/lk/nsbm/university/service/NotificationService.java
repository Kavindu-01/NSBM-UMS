package lk.nsbm.university.service;

import lk.nsbm.university.entity.Notification;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.repository.NotificationRepository;
import lk.nsbm.university.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Notification sendNotification(Long userId, String title, String message) {
        User recipient = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Notification notification = new Notification(recipient, title, message);
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(Long userId) {
        User recipient = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        User recipient = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return notificationRepository.countByRecipientAndIsReadFalse(recipient);
    }
}
