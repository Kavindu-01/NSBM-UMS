package lk.nsbm.university.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lk.nsbm.university.entity.Club;
import lk.nsbm.university.entity.Role;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.service.ClubService;
import lk.nsbm.university.service.MediaStorageService;
import lk.nsbm.university.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin/clubs")
@Validated
public class ClubAdminController {

    private final ClubService clubService;
    private final UserService userService;
    private final MediaStorageService mediaStorageService;

    public ClubAdminController(ClubService clubService,
                               UserService userService,
                               MediaStorageService mediaStorageService) {
        this.clubService = clubService;
        this.userService = userService;
        this.mediaStorageService = mediaStorageService;
    }

    @GetMapping
    public String manageClubs(@PageableDefault(size = 10) Pageable pageable,
                              Model model) {
        Page<Club> clubPage = clubService.getClubs(pageable);
        List<User> presidents = userService.getUsersByRole(Role.CLUB_PRESIDENT);
        model.addAttribute("clubPage", clubPage);
        model.addAttribute("clubForm", new ClubForm());
        model.addAttribute("presidents", presidents);
        return "admin/clubs";
    }

    @PostMapping
    public String createClub(@ModelAttribute("clubForm") @Validated ClubForm form,
                             BindingResult bindingResult,
                             Principal principal,
                             @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Fix validation errors and try again.");
            return "redirect:/admin/clubs";
        }
        try {
            String imagePath = mediaStorageService.store(coverImage, "clubs");
            if (StringUtils.hasText(imagePath)) {
                form.setCoverImageUrl(imagePath);
            }
            User creator = userService.getUserByStudentId(principal.getName())
                    .orElseThrow(() -> new IllegalStateException("Active admin not found"));
            clubService.createClub(form.getName(), form.getTagline(), form.getDescription(),
                    form.getWhatsappLink(), form.getCoverImageUrl(), form.getPresidentId(), creator);
            redirectAttributes.addFlashAttribute("successMessage", "Club created successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/clubs";
    }

    @PostMapping("/{clubId}/update")
    public String updateClub(@PathVariable Long clubId,
                             @ModelAttribute("clubForm") ClubForm form,
                             @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
                             RedirectAttributes redirectAttributes) {
        try {
            String imagePath = mediaStorageService.store(coverImage, "clubs");
            if (StringUtils.hasText(imagePath)) {
                form.setCoverImageUrl(imagePath);
            }
            clubService.updateClub(clubId, form.getName(), form.getTagline(), form.getDescription(),
                    form.getWhatsappLink(), form.getCoverImageUrl(), form.getPresidentId());
            redirectAttributes.addFlashAttribute("successMessage", "Club updated");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/clubs";
    }

    @PostMapping("/{clubId}/assign-president")
    public String assignPresident(@PathVariable Long clubId,
                                  @RequestParam Long presidentId,
                                  RedirectAttributes redirectAttributes) {
        try {
            clubService.assignPresident(clubId, presidentId);
            redirectAttributes.addFlashAttribute("successMessage", "President assigned");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/clubs";
    }

    @PostMapping("/{clubId}/delete")
    public String deleteClub(@PathVariable Long clubId,
                             RedirectAttributes redirectAttributes) {
        try {
            clubService.deleteClub(clubId);
            redirectAttributes.addFlashAttribute("successMessage", "Club deleted");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/clubs";
    }

    public static class ClubForm {
        @NotBlank
        @Size(max = 120)
        private String name;

        @NotBlank
        @Size(max = 300)
        private String tagline;

        @Size(max = 2000)
        private String description;

        @Size(max = 500)
        private String whatsappLink;

        @Size(max = 500)
        private String coverImageUrl;

        private Long presidentId;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTagline() {
            return tagline;
        }

        public void setTagline(String tagline) {
            this.tagline = tagline;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getWhatsappLink() {
            return whatsappLink;
        }

        public void setWhatsappLink(String whatsappLink) {
            this.whatsappLink = whatsappLink;
        }

        public String getCoverImageUrl() {
            return coverImageUrl;
        }

        public void setCoverImageUrl(String coverImageUrl) {
            this.coverImageUrl = coverImageUrl;
        }

        public Long getPresidentId() {
            return presidentId;
        }

        public void setPresidentId(Long presidentId) {
            this.presidentId = presidentId;
        }
    }
}
