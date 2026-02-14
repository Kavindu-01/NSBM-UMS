package lk.nsbm.university.controller;

import lk.nsbm.university.entity.Batch;
import lk.nsbm.university.entity.Role;
import lk.nsbm.university.entity.Semester;
import lk.nsbm.university.entity.SemesterResource;
import lk.nsbm.university.entity.SemesterResourceType;
import lk.nsbm.university.entity.Timetable;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.service.BatchService;
import lk.nsbm.university.service.SemesterResourceService;
import lk.nsbm.university.service.SemesterService;
import lk.nsbm.university.service.TimetableService;
import lk.nsbm.university.service.UserService;
import lk.nsbm.university.service.AcademicStructureService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/semesters")
public class SemesterPageController {

    private static final List<String> WEEK_DAYS = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");

    private final SemesterService semesterService;
    private final BatchService batchService;
    private final SemesterResourceService semesterResourceService;
    private final TimetableService timetableService;
    private final UserService userService;
    private final AcademicStructureService academicStructureService;

    public SemesterPageController(SemesterService semesterService,
                                  BatchService batchService,
                                  SemesterResourceService semesterResourceService,
                                  TimetableService timetableService,
                                  UserService userService,
                                  AcademicStructureService academicStructureService) {
        this.semesterService = semesterService;
        this.batchService = batchService;
        this.semesterResourceService = semesterResourceService;
        this.timetableService = timetableService;
        this.userService = userService;
        this.academicStructureService = academicStructureService;
    }

    @GetMapping
    public String listSemesters(Model model,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        User currentUser = resolveCurrentUser(principal, false);
        if (currentUser != null && !isAdministrativeUser(currentUser) && currentUser.getRole() != Role.USER) {
            redirectAttributes.addFlashAttribute("warningMessage", "Semester hubs are limited to student accounts.");
            return "redirect:/dashboard";
        }
        if (currentUser != null && !isAdministrativeUser(currentUser)) {
            if (currentUser.getSemester() == null) {
                model.addAttribute("currentUser", currentUser);
                return "semesters/pending";
            }
            return "redirect:/semesters/" + currentUser.getSemester().getId();
        }

        List<Semester> semesters = semesterService.getAllSemesters();
        Map<Long, Long> batchCounts = new HashMap<>();
        Map<Long, Long> resourceCounts = new HashMap<>();
        for (Semester semester : semesters) {
            batchCounts.put(semester.getId(), (long) batchService.getBatchesForSemester(semester.getId()).size());
            resourceCounts.put(semester.getId(), semesterResourceService.countBySemester(semester.getId()));
        }
        Long currentSemesterId = currentUser != null && currentUser.getSemester() != null
                ? currentUser.getSemester().getId() : null;
        model.addAttribute("semesters", semesters);
        model.addAttribute("batchCounts", batchCounts);
        model.addAttribute("resourceCounts", resourceCounts);
        model.addAttribute("currentSemesterId", currentSemesterId);
        return "semesters/index";
    }

    @GetMapping("/{semesterId}")
    public String viewSemester(@PathVariable Long semesterId,
                               Model model,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        Semester semester = semesterService.getSemesterById(semesterId);
        List<Batch> batches = batchService.getBatchesForSemester(semesterId);
        User currentUser = resolveCurrentUser(principal, false);
        boolean isAdmin = isAdministrativeUser(currentUser);
        List<SemesterResource> resources;
        boolean isStudentAssigned = false;
        // Fix: Read filterFacultyId and filterDegreeId from request params if present (for admin)
        Long filterFacultyId = null;
        Long filterDegreeId = null;
        Object facultyParam = model.asMap().get("filterFacultyId");
        Object degreeParam = model.asMap().get("filterDegreeId");
        if (facultyParam instanceof Long) filterFacultyId = (Long) facultyParam;
        else if (facultyParam instanceof String) try { filterFacultyId = Long.parseLong((String) facultyParam); } catch (Exception ignored) {}
        if (degreeParam instanceof Long) filterDegreeId = (Long) degreeParam;
        else if (degreeParam instanceof String) try { filterDegreeId = Long.parseLong((String) degreeParam); } catch (Exception ignored) {}
        if (isAdmin) {
            // If filterFacultyId or filterDegreeId are set, filter accordingly
            if (filterFacultyId != null && filterDegreeId != null) {
                resources = semesterResourceService.getResourcesForSemesterAndFacultyAndDegree(semesterId, filterFacultyId, filterDegreeId);
            } else if (filterFacultyId != null) {
                resources = semesterResourceService.getResourcesForSemesterAndFaculty(semesterId, filterFacultyId);
            } else if (filterDegreeId != null) {
                resources = semesterResourceService.getResourcesForSemesterAndDegree(semesterId, filterDegreeId);
            } else {
                resources = semesterResourceService.getResourcesForSemester(semesterId);
            }
            isStudentAssigned = true; // Admins always see content
        } else if (currentUser != null && currentUser.getRole() == Role.USER) {
            // Only students
            if (currentUser.getSemester() != null && currentUser.getFaculty() != null && currentUser.getDegree() != null
                && currentUser.getSemester().getId().equals(semesterId)) {
                resources = semesterResourceService.getResourcesForSemesterAndFacultyAndDegree(semesterId, currentUser.getFaculty().getId(), currentUser.getDegree().getId());
                isStudentAssigned = true;
            } else {
                resources = List.of(); // Empty list, not assigned
                isStudentAssigned = false;
            }
        } else {
            // fallback for other roles
            resources = semesterResourceService.getResourcesForSemester(semesterId);
            isStudentAssigned = true;
        }
        // Add faculties and degrees for resource upload form
        List<lk.nsbm.university.entity.Faculty> faculties = academicStructureService.getFaculties();
        List<lk.nsbm.university.entity.DegreeProgram> degrees = academicStructureService.getDegrees();
        List<Timetable> timetableEntries = timetableService.getTimetablesForSemester(semesterId);

        if (currentUser != null && !isAdministrativeUser(currentUser) && currentUser.getRole() != Role.USER) {
            redirectAttributes.addFlashAttribute("warningMessage", "Semester hubs are limited to student accounts.");
            return "redirect:/dashboard";
        }
        if (currentUser != null && !isAdministrativeUser(currentUser)) {
            if (currentUser.getSemester() == null) {
                redirectAttributes.addFlashAttribute("warningMessage", "You are awaiting semester assignment.");
                return "redirect:/semesters";
            }
            if (!currentUser.getSemester().getId().equals(semesterId)) {
                redirectAttributes.addFlashAttribute("warningMessage", "You can only view your assigned semester hub.");
                return "redirect:/semesters/" + currentUser.getSemester().getId();
            }
        }
        // (removed duplicate declaration of isAdmin)
        Long currentBatchId = currentUser != null && currentUser.getBatch() != null
                ? currentUser.getBatch().getId() : null;

        model.addAttribute("semester", semester);
        model.addAttribute("batches", batches);
        model.addAttribute("resources", resources);
        model.addAttribute("resourceTypes", SemesterResourceType.values());
        model.addAttribute("timetableEntries", timetableEntries);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("currentBatchId", currentBatchId);
        model.addAttribute("faculties", faculties);
        model.addAttribute("degrees", degrees);
        model.addAttribute("isStudentAssigned", isStudentAssigned);
        model.addAttribute("weekDays", WEEK_DAYS);
        return "semesters/detail";
    }

    @PostMapping("/{semesterId}/resources")
    public String addResource(@PathVariable Long semesterId,
                              @RequestParam SemesterResourceType type,
                              @RequestParam String title,
                              @RequestParam(required = false) Long batchId,
                              @RequestParam(required = false) Long facultyId,
                              @RequestParam(required = false) Long degreeId,
                              @RequestParam(required = false) String textContent,
                              @RequestParam(required = false) MultipartFile file,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        User admin = resolveCurrentUser(principal, true);
        try {
            semesterResourceService.addResource(semesterId, batchId, facultyId, degreeId, type, title, textContent, file, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Resource published successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/semesters/" + semesterId;
    }

    @PostMapping("/{semesterId}/resources/{resourceId}/delete")
    public String deleteResource(@PathVariable Long semesterId,
                                 @PathVariable Long resourceId,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        resolveCurrentUser(principal, true);
        try {
            semesterResourceService.deleteResource(resourceId);
            redirectAttributes.addFlashAttribute("successMessage", "Resource removed");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/semesters/" + semesterId;
    }

    @PostMapping("/{semesterId}/timetable")
    public String createTimetableEntry(@PathVariable Long semesterId,
                                       @RequestParam Long batchId,
                                       @RequestParam String title,
                                       @RequestParam String subject,
                                       @RequestParam String dayOfWeek,
                                       @RequestParam String timeSlot,
                                       @RequestParam(required = false) String lecturer,
                                       @RequestParam(required = false) String location,
                                       Principal principal,
                                       RedirectAttributes redirectAttributes) {
        User admin = resolveCurrentUser(principal, true);
        try {
            Timetable timetable = buildTimetablePayload(title, subject, dayOfWeek, timeSlot, lecturer, location);
            timetableService.createTimetable(timetable, batchId, semesterId, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Timetable entry created");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/semesters/" + semesterId + "#timetable-section";
    }

    @PostMapping("/{semesterId}/timetable/{timetableId}/update")
    public String updateTimetableEntry(@PathVariable Long semesterId,
                                       @PathVariable Long timetableId,
                                       @RequestParam Long batchId,
                                       @RequestParam String title,
                                       @RequestParam String subject,
                                       @RequestParam String dayOfWeek,
                                       @RequestParam String timeSlot,
                                       @RequestParam(required = false) String lecturer,
                                       @RequestParam(required = false) String location,
                                       Principal principal,
                                       RedirectAttributes redirectAttributes) {
        resolveCurrentUser(principal, true);
        try {
            Timetable payload = buildTimetablePayload(title, subject, dayOfWeek, timeSlot, lecturer, location);
            timetableService.updateTimetable(timetableId, payload, batchId, semesterId);
            redirectAttributes.addFlashAttribute("successMessage", "Timetable entry updated");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/semesters/" + semesterId + "#timetable-section";
    }

    @PostMapping("/{semesterId}/timetable/{timetableId}/delete")
    public String deleteTimetableEntry(@PathVariable Long semesterId,
                                       @PathVariable Long timetableId,
                                       Principal principal,
                                       RedirectAttributes redirectAttributes) {
        resolveCurrentUser(principal, true);
        try {
            timetableService.deleteTimetable(timetableId);
            redirectAttributes.addFlashAttribute("successMessage", "Timetable entry removed");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/semesters/" + semesterId + "#timetable-section";
    }

    @GetMapping("/resources/{resourceId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadResource(@PathVariable Long resourceId,
                                                                                Principal principal) {
        SemesterResource resourceMeta = semesterResourceService.getResource(resourceId);
        User currentUser = resolveCurrentUser(principal, false);
        enforceDownloadAccess(currentUser, resourceMeta);
        org.springframework.core.io.Resource file = semesterResourceService.loadFile(resourceId);
        String filename = resourceMeta.getFileName() != null ? resourceMeta.getFileName() : "semester-resource";
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (resourceMeta.getFileType() != null) {
            try {
                mediaType = MediaType.parseMediaType(resourceMeta.getFileType());
            } catch (IllegalArgumentException ignored) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }
        long contentLength = -1L;
        try {
            contentLength = file.contentLength();
        } catch (IOException ignored) {
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .contentLength(contentLength > -1 ? contentLength : resourceMeta.getFileSize() != null ? resourceMeta.getFileSize() : 0)
                .body(file);
    }

    private Timetable buildTimetablePayload(String title,
                                            String subject,
                                            String dayOfWeek,
                                            String timeSlot,
                                            String lecturer,
                                            String location) {
        Timetable timetable = new Timetable();
        timetable.setTitle(requireText(title, "Title"));
        timetable.setSubject(requireText(subject, "Subject"));
        timetable.setDayOfWeek(normalizeDayOfWeek(dayOfWeek));
        timetable.setTimeSlot(requireText(timeSlot, "Time slot"));
        timetable.setLecturer(cleanOptional(lecturer));
        timetable.setLocation(cleanOptional(location));
        return timetable;
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String cleanOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeDayOfWeek(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Select a day of week");
        }
        String normalized = value.trim();
        return WEEK_DAYS.stream()
                .filter(day -> day.equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid day of week"));
    }

    private User resolveCurrentUser(Principal principal, boolean enforceAdmin) {
        if (principal == null) {
            if (enforceAdmin) {
                throw new AccessDeniedException("Only administrators can modify semester resources");
            }
            return null;
        }
        User user = userService.getUserByStudentId(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (enforceAdmin && !isAdministrativeUser(user)) {
            throw new AccessDeniedException("Only administrators can modify semester resources");
        }
        return user;
    }

    private boolean isAdministrativeUser(User user) {
        return user != null && (user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN);
    }

    private void enforceDownloadAccess(User user, SemesterResource resource) {
        if (user == null) {
            throw new AccessDeniedException("Sign in to download semester files");
        }
        if (isAdministrativeUser(user)) {
            return;
        }
        if (user.getSemester() == null || resource.getSemester() == null
                || !user.getSemester().getId().equals(resource.getSemester().getId())) {
            throw new AccessDeniedException("You can only download files shared with your semester");
        }
    }
}
