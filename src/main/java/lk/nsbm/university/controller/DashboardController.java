package lk.nsbm.university.controller;

import lk.nsbm.university.entity.AccountStatus;
import lk.nsbm.university.entity.Role;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.service.AnnouncementService;
import lk.nsbm.university.service.SemesterResourceService;
import lk.nsbm.university.service.SocialPostService;
import lk.nsbm.university.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class DashboardController {

    private final UserService userService;
    private final AnnouncementService announcementService;
    private final SemesterResourceService semesterResourceService;
    private final SocialPostService socialPostService;

    public DashboardController(UserService userService,
                               AnnouncementService announcementService,
                               SemesterResourceService semesterResourceService,
                               SocialPostService socialPostService) {
        this.userService = userService;
        this.announcementService = announcementService;
        this.semesterResourceService = semesterResourceService;
        this.socialPostService = socialPostService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        User currentUser = userService.getUserByStudentId(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("latestAnnouncements", announcementService.getLatestAnnouncements());
        model.addAttribute("featuredPosts",
            socialPostService.getFeed(currentUser, PageRequest.of(0, 4)).getContent());
        model.addAttribute("pendingCount", userService.countUsersByStatus(AccountStatus.PENDING));
        model.addAttribute("approvedCount", userService.countUsersByStatus(AccountStatus.APPROVED));
        model.addAttribute("rejectedCount", userService.countUsersByStatus(AccountStatus.REJECTED));
        model.addAttribute("semesterResourceCount", semesterResourceService.countAllResources());
        model.addAttribute("announcementCount", announcementService.countAllAnnouncements());
        model.addAttribute("streamPostCount", socialPostService.countActivePosts());

        Role role = currentUser.getRole();
        return switch (role) {
            case SUPER_ADMIN, ADMIN -> "dashboard/admin";
            case MODERATOR -> "dashboard/moderator";
            case CONTENT_MANAGER, EDITOR -> "dashboard/user";
            case BOARDING_GUARDIAN -> "dashboard/guardian";
            case SHUTTLE_DRIVER -> "dashboard/shuttle";
            case CLUB_PRESIDENT -> "dashboard/club";
            default -> "dashboard/user";
        };
    }
}
