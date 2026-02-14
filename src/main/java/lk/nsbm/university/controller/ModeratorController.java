package lk.nsbm.university.controller;

import lk.nsbm.university.entity.AccountStatus;
import lk.nsbm.university.entity.Role;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.service.BatchService;
import lk.nsbm.university.service.SemesterService;
import lk.nsbm.university.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/moderator")
public class ModeratorController {

    private final UserService userService;
    private final BatchService batchService;
    private final SemesterService semesterService;

    public ModeratorController(UserService userService,
                               BatchService batchService,
                               SemesterService semesterService) {
        this.userService = userService;
        this.batchService = batchService;
        this.semesterService = semesterService;
    }

    @GetMapping("/approvals")
    public String approvals(@PageableDefault(size = 12) Pageable pageable, Model model) {
        Page<User> pending = userService.getUsersByStatus(AccountStatus.PENDING, pageable);
        model.addAttribute("pendingPage", pending);
        model.addAttribute("roles", Role.values());
        model.addAttribute("batches", batchService.getAllBatches(Pageable.unpaged()).getContent());
        model.addAttribute("semesters", semesterService.getAllSemesters());
        return "moderator/approvals";
    }

    @PostMapping("/users/{userId}/approve")
        public String approve(@PathVariable Long userId,
                  @RequestParam(required = false) Role role,
                  @RequestParam(required = false) Long batchId,
                  @RequestParam(required = false) Long semesterId,
                  Principal principal,
                  RedirectAttributes redirectAttributes) {
        try {
            User actingUser = principal != null
                ? userService.getUserByStudentId(principal.getName()).orElse(null)
                : null;
            userService.approveUser(userId, role, batchId, semesterId, actingUser);
            redirectAttributes.addFlashAttribute("successMessage", "User approved");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/moderator/approvals";
    }

    @PostMapping("/users/{userId}/reject")
    public String reject(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        try {
            userService.rejectUser(userId);
            redirectAttributes.addFlashAttribute("successMessage", "User rejected");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/moderator/approvals";
    }
}
