package lk.nsbm.university.controller;

import lk.nsbm.university.entity.Role;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.service.UserService;
import lk.nsbm.university.util.AvatarGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final Path mediaRoot;
    private static final String RECOVERY_CODE_REGEX = "^[0-9]{6}$";

    public ProfileController(UserService userService,
                             @Value("${storage.media-root:uploads/media}") String mediaRoot) {
        this.userService = userService;
        this.mediaRoot = Paths.get(mediaRoot).toAbsolutePath().normalize();
    }

    @GetMapping
    public String viewProfile(Model model, Principal principal) {
        User user = requireCurrentUser(principal);
        populateModel(model, user);
        return "profile/index";
    }

    @PostMapping("/info")
    public String updateProfileInfo(@RequestParam(required = false) String fullName,
                                    @RequestParam(required = false) String contactNumber,
                                    @RequestParam(required = false) String profileBio,
                                    Principal principal,
                                    RedirectAttributes redirectAttributes) {
        User user = requireCurrentUser(principal);
        try {
            if (!StringUtils.hasText(fullName) && !StringUtils.hasText(contactNumber) && !StringUtils.hasText(profileBio)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Provide at least one field to update");
            } else {
                userService.updateProfileDetails(user.getId(), fullName, contactNumber, profileBio);
                redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully");
            }
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/avatar")
    public String updateAvatar(@RequestParam("avatar") MultipartFile avatarFile,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        User user = requireCurrentUser(principal);
        try {
            userService.updateProfileAvatar(user.getId(), avatarFile, mediaRoot);
            redirectAttributes.addFlashAttribute("successMessage", "Profile picture updated");
        } catch (IOException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to store avatar: " + ex.getMessage());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/avatar/reset")
    public String resetAvatar(Principal principal, RedirectAttributes redirectAttributes) {
        User user = requireCurrentUser(principal);
        try {
            userService.removeProfileAvatar(user.getId(), mediaRoot);
            redirectAttributes.addFlashAttribute("successMessage", "Avatar reset to auto-generated style");
        } catch (IOException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reset avatar: " + ex.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/credentials/password")
    public String updatePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        User user = requireCurrentUser(principal);
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password must be at least 8 characters long");
            return "redirect:/profile#credential-card";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match");
            return "redirect:/profile#credential-card";
        }
        try {
            userService.updatePasswordSelf(user.getId(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/profile#credential-card";
    }

    @PostMapping("/credentials/recovery")
    public String updateRecoveryCode(@RequestParam String recoveryCode,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes) {
        User user = requireCurrentUser(principal);
        if (!StringUtils.hasText(recoveryCode) || !recoveryCode.trim().matches(RECOVERY_CODE_REGEX)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Recovery code must be 6 digits");
            return "redirect:/profile#credential-card";
        }
        try {
            userService.updateRecoveryCode(user.getId(), recoveryCode);
            redirectAttributes.addFlashAttribute("successMessage", "Recovery code saved");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/profile#credential-card";
    }

    private User requireCurrentUser(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return userService.getUserByStudentId(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void populateModel(Model model, User user) {
        model.addAttribute("profileUser", user);
        model.addAttribute("avatarUrl", StringUtils.hasText(user.getAvatarPath()) ? "/media/" + user.getAvatarPath() : null);
        model.addAttribute("avatarInitials", AvatarGenerator.initials(user));
        model.addAttribute("avatarGradient", AvatarGenerator.gradient(user));
        model.addAttribute("isSuperAdmin", user.getRole() == Role.SUPER_ADMIN);
        model.addAttribute("selfManagedCredentials", userService.isSelfManagedRole(user.getRole()));
    }
}
