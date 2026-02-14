package lk.nsbm.university.controller;

import lk.nsbm.university.entity.Faculty;
import lk.nsbm.university.service.AcademicStructureService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/faculties")
public class FacultyController {

    private final AcademicStructureService academicService;

    public FacultyController(AcademicStructureService academicService) {
        this.academicService = academicService;
    }

    @GetMapping
    public String listFaculties(Model model) {
        model.addAttribute("faculties", academicService.getFaculties());
        return "admin/faculties";
    }

    @PostMapping
    public String createFaculty(@RequestParam String name,
                                @RequestParam String code,
                                @RequestParam(required = false) String description,
                                RedirectAttributes redirectAttributes) {
        try {
            academicService.createFaculty(name, code, description);
            redirectAttributes.addFlashAttribute("successMessage", "Faculty created");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/faculties";
    }

    @PostMapping("/{facultyId}/update")
    public String updateFaculty(@PathVariable Long facultyId,
                                @RequestParam String name,
                                @RequestParam String code,
                                @RequestParam(required = false) String description,
                                RedirectAttributes redirectAttributes) {
        try {
            academicService.updateFaculty(facultyId, name, code, description);
            redirectAttributes.addFlashAttribute("successMessage", "Faculty updated");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/faculties";
    }

    @PostMapping("/{facultyId}/delete")
    public String deleteFaculty(@PathVariable Long facultyId, RedirectAttributes redirectAttributes) {
        try {
            academicService.deleteFaculty(facultyId);
            redirectAttributes.addFlashAttribute("successMessage", "Faculty deleted");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/faculties";
    }

    @GetMapping("/{facultyId}/degrees")
    public String listDegrees(@PathVariable Long facultyId, Model model) {
        Faculty faculty = academicService.getFaculty(facultyId);
        model.addAttribute("faculty", faculty);
        model.addAttribute("degrees", academicService.getDegreesForFaculty(facultyId));
        return "admin/degrees";
    }

    @PostMapping("/{facultyId}/degrees")
    public String createDegree(@PathVariable Long facultyId,
                               @RequestParam String name,
                               @RequestParam String code,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) String duration,
                               RedirectAttributes redirectAttributes) {
        try {
            academicService.createDegree(facultyId, name, code, description, duration);
            redirectAttributes.addFlashAttribute("successMessage", "Degree created");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/faculties/" + facultyId + "/degrees";
    }

    @PostMapping("/{facultyId}/degrees/{degreeId}/update")
    public String updateDegree(@PathVariable Long facultyId,
                               @PathVariable Long degreeId,
                               @RequestParam String name,
                               @RequestParam String code,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) String duration,
                               RedirectAttributes redirectAttributes) {
        try {
            academicService.updateDegree(degreeId, facultyId, name, code, description, duration);
            redirectAttributes.addFlashAttribute("successMessage", "Degree updated");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/faculties/" + facultyId + "/degrees";
    }

    @PostMapping("/{facultyId}/degrees/{degreeId}/delete")
    public String deleteDegree(@PathVariable Long facultyId,
                               @PathVariable Long degreeId,
                               RedirectAttributes redirectAttributes) {
        try {
            academicService.deleteDegree(degreeId);
            redirectAttributes.addFlashAttribute("successMessage", "Degree deleted");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/faculties/" + facultyId + "/degrees";
    }
}
