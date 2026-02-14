package lk.nsbm.university.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/announcements")
public class AnnouncementController {

    @GetMapping({"", "/", "/manage"})
    public String redirectAnnouncements(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("infoMessage", "Announcements now live inside the Semester Hub workspace.");
        return "redirect:/semesters";
    }

    @GetMapping("/{announcementId}")
    public String redirectAnnouncementDetail(@PathVariable Long announcementId,
                                             RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("infoMessage", "Announcement feeds are now curated per semester. Redirected to hub.");
        return "redirect:/semesters";
    }

    @PostMapping({"", "/{announcementId}/update", "/{announcementId}/delete"})
    public String handleLegacyAnnouncementPosts(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("warningMessage", "Announcement CMS has been retired. Publish notices via the Semester Hub instead.");
        return "redirect:/semesters";
    }
}
