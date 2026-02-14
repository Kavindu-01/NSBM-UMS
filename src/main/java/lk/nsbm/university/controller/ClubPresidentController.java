package lk.nsbm.university.controller;

import lk.nsbm.university.entity.Club;
import lk.nsbm.university.entity.ClubPost;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.service.ClubPostService;
import lk.nsbm.university.service.ClubService;
import lk.nsbm.university.service.MediaStorageService;
import lk.nsbm.university.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/club/manage")
public class ClubPresidentController {

    private final ClubService clubService;
    private final ClubPostService postService;
    private final UserService userService;
    private final MediaStorageService mediaStorageService;

    public ClubPresidentController(ClubService clubService,
                                   ClubPostService postService,
                                   UserService userService,
                                   MediaStorageService mediaStorageService) {
        this.clubService = clubService;
        this.postService = postService;
        this.userService = userService;
        this.mediaStorageService = mediaStorageService;
    }

    @GetMapping
    public String manageLanding(Model model,
                                Principal principal) {
        User user = resolveUser(principal);
        List<Club> managedClubs = clubService.getClubsManagedBy(user.getId());
        model.addAttribute("managedClubs", managedClubs);
        return "clubs/manage";
    }

    @GetMapping("/{clubId}")
    public String manageClub(@PathVariable Long clubId,
                              Model model,
                              Principal principal) {
        User user = resolveUser(principal);
        Club club = ensurePresidentAccess(clubId, user);
        PageRequest pageable = PageRequest.of(0, 10);
        Page<ClubPost> posts = postService.getPostsForClub(clubId, pageable);
        model.addAttribute("club", club);
        model.addAttribute("posts", posts);
        return "clubs/manage-detail";
    }

    @PostMapping("/{clubId}/details")
    public String updateDetails(@PathVariable Long clubId,
                                 @RequestParam String tagline,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(required = false) String whatsappLink,
                                 @RequestParam(required = false) String existingCoverImage,
                                 @RequestParam(required = false, name = "coverImage") MultipartFile coverImage,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        ensurePresidentAccess(clubId, user);
        try {
            if (!org.springframework.util.StringUtils.hasText(tagline)) {
                throw new IllegalArgumentException("Tagline is required");
            }
            String uploadedCover = mediaStorageService.store(coverImage, "clubs");
            String coverImagePath = StringUtils.hasText(uploadedCover) ? uploadedCover : existingCoverImage;
            clubService.updateClub(clubId, null, tagline, description, whatsappLink, coverImagePath, null);
            redirectAttributes.addFlashAttribute("successMessage", "Club details updated");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/club/manage/" + clubId;
    }

    @PostMapping("/{clubId}/posts")
    public String createPost(@PathVariable Long clubId,
                              @RequestParam String title,
                              @RequestParam(required = false, name = "updateBody") String updateBody,
                              @RequestParam(required = false, name = "infoDetails") String infoDetails,
                              @RequestParam(required = false, name = "resourceLabel") String resourceLabel,
                              @RequestParam(required = false, name = "imageFile") MultipartFile imageFile,
                              @RequestParam(required = false, name = "resourceFile") MultipartFile resourceFile,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        ensurePresidentAccess(clubId, user);
        try {
            String imagePath = mediaStorageService.store(imageFile, "club-posts");
            String resourcePath = mediaStorageService.store(resourceFile, "club-posts");
            String combinedContent = composeContent(updateBody, infoDetails);
            postService.createPost(clubId, title, combinedContent, imagePath, resourceLabel, resourcePath, user);
            redirectAttributes.addFlashAttribute("successMessage", "Post published");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/club/manage/" + clubId;
    }

    @PostMapping("/{clubId}/posts/{postId}/delete")
    public String deletePost(@PathVariable Long clubId,
                             @PathVariable Long postId,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        ensurePresidentAccess(clubId, user);
        try {
            postService.deletePost(clubId, postId);
            redirectAttributes.addFlashAttribute("successMessage", "Post removed");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/club/manage/" + clubId;
    }

    private String composeContent(String updateBody, String infoDetails) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(updateBody)) {
            builder.append(updateBody.trim());
        }
        if (StringUtils.hasText(infoDetails)) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append(infoDetails.trim());
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private User resolveUser(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("Active session required");
        }
        return userService.getUserByStudentId(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    private Club ensurePresidentAccess(Long clubId, User user) {
        Club club = clubService.getClub(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found"));
        if (club.getPresident() == null || !club.getPresident().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You do not manage this club");
        }
        return club;
    }
}
