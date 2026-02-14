package lk.nsbm.university.config;

import jakarta.annotation.PostConstruct;
import lk.nsbm.university.entity.Content;
import lk.nsbm.university.entity.ContentCategory;
import lk.nsbm.university.entity.Role;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.repository.ContentRepository;
import lk.nsbm.university.repository.UserRepository;
import lk.nsbm.university.service.SemesterService;
import lk.nsbm.university.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer {

    private final SemesterService semesterService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    public DataInitializer(SemesterService semesterService,
                           UserService userService,
                           UserRepository userRepository,
                           ContentRepository contentRepository) {
        this.semesterService = semesterService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.contentRepository = contentRepository;
    }

    @PostConstruct
    public void seedReferenceData() {
        semesterService.initializeDefaultSemesters();
        createUserIfMissing("ADMIN01", "Admin@123", Role.SUPER_ADMIN);
        createUserIfMissing("MOD01", "Mod@123", Role.MODERATOR);
        createUserIfMissing("CONTENT01", "Content@123", Role.CONTENT_MANAGER);
        createUserIfMissing("EDITOR01", "Editor@123", Role.EDITOR, "Editorial Desk", "+94 11 544 5555");
        userService.promoteDefaultAdminToSuperAdmin("ADMIN01");
        seedDefaultContent();
    }

    private void createUserIfMissing(String studentId, String password, Role role) {
        createUserIfMissing(studentId, password, role, null, null);
    }

    private void createUserIfMissing(String studentId,
                                     String password,
                                     Role role,
                                     String fullName,
                                     String contactNumber) {
        if (userRepository.existsByStudentId(studentId)) {
            return;
        }
        try {
            userService.createApprovedUser(studentId, password, role, fullName, contactNumber);
            log.info("Seeded default {} account", studentId);
        } catch (Exception ex) {
            log.error("Failed to seed {}", studentId, ex);
        }
    }

    private void seedDefaultContent() {
        if (contentRepository.count() > 0) {
            return;
        }
        User creator = userRepository.findByStudentId("CONTENT01")
                .or(() -> userRepository.findByStudentId("ADMIN01"))
                .orElse(null);
        if (creator == null) {
            log.warn("Skipping default content seeding because no privileged user was found.");
            return;
        }

        List<Content> samples = List.of(
                buildContent("Shuttle Routes & Timings",
                        "Keep track of Green, Blue, and City shuttles including first/last departures.",
                        ContentCategory.SHUTTLE,
                        "Weekday first shuttle leaves at 6:15 AM from Homagama. Hotline: +94 11 544 5000",
                        creator),
                buildContent("Student Boarding Support",
                        "Verified boarding houses within 2km of campus with transparent pricing tiers.",
                        ContentCategory.BOARDING,
                        "Email boarding@nsbm.ac.lk for fresh vacancies or to list your property.",
                        creator),
                buildContent("Club & Society Directory",
                        "Meetups for IEEE, Rotaract, Media Club, and more. Find your next passion project.",
                        ContentCategory.CLUB,
                        "Sign-up booth opens every Wednesday, Student Centre Level 1.",
                        creator),
                buildContent("Academic Resource Pack",
                        "Quick access to LMS, library bookings, and peer mentoring sessions.",
                        ContentCategory.ACADEMIC,
                        "Library hotline: +94 11 544 5454 | Mentoring: mentoring@nsbm.ac.lk",
                        creator)
        );
        contentRepository.saveAll(samples);
        log.info("Seeded {} starter content items", samples.size());
    }

    private Content buildContent(String title, String description, ContentCategory category,
                                 String additionalInfo, User creator) {
        Content content = new Content();
        content.setTitle(title);
        content.setDescription(description);
        content.setCategory(category);
        content.setAdditionalInfo(additionalInfo);
        content.setCreatedBy(creator);
        return content;
    }
}
