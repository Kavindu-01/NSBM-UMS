package lk.nsbm.university.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/timetables")
public class TimetableController {

    @GetMapping({"", "/", "/manage"})
    public String redirectTimetableWorkspace(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("infoMessage", "Timetable management lives in the Semester Hub now.");
        return "redirect:/semesters";
    }

    @PostMapping({"", "/{timetableId}/update", "/{timetableId}/delete"})
    public String handleLegacyTimetablePosts(@PathVariable(required = false) Long timetableId,
                                             RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("warningMessage", "Timetable CMS has moved. Use the Semester Hub workspace to edit schedules.");
        return "redirect:/semesters";
    }
}
