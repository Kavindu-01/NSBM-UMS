package lk.nsbm.university.controller;

import lk.nsbm.university.entity.Notification;
import lk.nsbm.university.service.NotificationService;
import lk.nsbm.university.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    public String listNotifications(Model model, Principal principal) {
        var user = userService.getUserByStudentId(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        List<Notification> notifications = notificationService.getNotificationsForUser(user.getId());
        model.addAttribute("notifications", notifications);
        return "notifications/list";
    }

    @PostMapping("/mark-read/{id}")
    public String markAsRead(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        var user = userService.getUserByStudentId(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        notificationService.markAsRead(id);
        redirectAttributes.addFlashAttribute("successMessage", "Notification marked as read");
        return "redirect:/notifications";
    }

    // Admin: send notification to user
    @PostMapping("/send")
    public String sendNotification(@RequestParam Long userId, @RequestParam String title, @RequestParam String message, RedirectAttributes redirectAttributes) {
        notificationService.sendNotification(userId, title, message);
        redirectAttributes.addFlashAttribute("successMessage", "Notification sent");
        return "redirect:/admin/users/" + userId;
    }
}
