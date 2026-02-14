package lk.nsbm.university.controller;

import lk.nsbm.university.entity.Club;
import lk.nsbm.university.entity.ClubPost;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.service.ClubPostService;
import lk.nsbm.university.service.ClubService;
import lk.nsbm.university.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/clubs")
public class ClubDirectoryController {

    private final ClubService clubService;
    private final ClubPostService postService;
    private final UserService userService;

    public ClubDirectoryController(ClubService clubService,
                                   ClubPostService postService,
                                   UserService userService) {
        this.clubService = clubService;
        this.postService = postService;
        this.userService = userService;
    }

    @GetMapping
    public String viewDirectory(Model model,
                                Principal principal) {
        User user = resolveUser(principal);
        List<Club> clubs = clubService.getAllClubs();
        Set<Long> joinedClubIds = clubService.getMembershipClubIds(user);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ClubPost> feed = postService.getPostsForClubs(joinedClubIds, pageable);
        List<Club> joinedClubs = clubs.stream()
            .filter(club -> joinedClubIds.contains(club.getId()))
            .collect(Collectors.toList());
        List<Club> discoverClubs = clubs.stream()
            .filter(club -> !joinedClubIds.contains(club.getId()))
            .collect(Collectors.toList());
        model.addAttribute("clubs", clubs);
        model.addAttribute("joinedClubIds", joinedClubIds);
        model.addAttribute("joinedClubs", joinedClubs);
        model.addAttribute("discoverClubs", discoverClubs);
        model.addAttribute("feed", feed);
        return "clubs/directory";
    }

    @PostMapping("/{clubId}/join")
    public String joinClub(@PathVariable Long clubId,
                           Principal principal,
                           RedirectAttributes redirectAttributes) {
        try {
            clubService.joinClub(clubId, resolveUser(principal));
            redirectAttributes.addFlashAttribute("successMessage", "Joined club successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/clubs";
    }

    @PostMapping("/{clubId}/leave")
    public String leaveClub(@PathVariable Long clubId,
                            Principal principal,
                            RedirectAttributes redirectAttributes) {
        try {
            clubService.leaveClub(clubId, resolveUser(principal));
            redirectAttributes.addFlashAttribute("successMessage", "Left club");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/clubs";
    }

    private User resolveUser(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("Active session required");
        }
        return userService.getUserByStudentId(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
