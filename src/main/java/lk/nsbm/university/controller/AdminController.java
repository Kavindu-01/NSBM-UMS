package lk.nsbm.university.controller;

import lk.nsbm.university.entity.AccountStatus;
import lk.nsbm.university.entity.Role;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.entity.Club;
import lk.nsbm.university.service.BatchService;
import lk.nsbm.university.service.ClubService;
import lk.nsbm.university.service.SemesterService;
import lk.nsbm.university.service.UserService;
import lk.nsbm.university.service.AcademicStructureService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final BatchService batchService;
    private final SemesterService semesterService;
    private final ClubService clubService;
    private final AcademicStructureService academicStructureService;
        private static final Set<Role> MANAGED_ROLES = Collections.unmodifiableSet(EnumSet.of(
            Role.ADMIN,
            Role.MODERATOR,
            Role.CONTENT_MANAGER,
            Role.EDITOR,
            Role.SHUTTLE_DRIVER,
            Role.CLUB_PRESIDENT
        ));
    private static final DateTimeFormatter BACKUP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Map<String, String> SORT_OPTIONS;

    static {
        Map<String, String> sortMap = new LinkedHashMap<>();
        sortMap.put("newest", "Newest first");
        sortMap.put("oldest", "Oldest first");
        sortMap.put("name", "Name A → Z");
        sortMap.put("faculty", "Faculty A → Z");
        sortMap.put("degree", "Degree A → Z");
        SORT_OPTIONS = Collections.unmodifiableMap(sortMap);
    }

    public AdminController(UserService userService,
                           BatchService batchService,
                           SemesterService semesterService,
                           ClubService clubService,
                           AcademicStructureService academicStructureService) {
        this.userService = userService;
        this.batchService = batchService;
        this.semesterService = semesterService;
        this.clubService = clubService;
        this.academicStructureService = academicStructureService;
    }

    @GetMapping("/users")
    public String manageUsers(@RequestParam(value = "status", required = false) String statusParam,
                              @RequestParam(value = "role", required = false, defaultValue = "USER") String roleParam,
                              @RequestParam(value = "studentId", required = false) String studentId,
                              @RequestParam(value = "facultyId", required = false) String facultyIdParam,
                              @RequestParam(value = "degreeId", required = false) String degreeIdParam,
                              @RequestParam(value = "sort", required = false, defaultValue = "newest") String sortParam,
                              @PageableDefault(size = 12) Pageable pageable,
                              Model model,
                              Principal principal) {
        AccountStatus status = resolveStatus(statusParam);
        Role roleFilter = resolveRole(roleParam);
        Pageable sortedPageable = applySorting(pageable, sortParam);
        User actingUser = resolveActingUser(principal);
        Long facultyId = null;
        Long degreeId = null;
        if (facultyIdParam != null && facultyIdParam.matches("^\\d+$")) {
            try {
                facultyId = Long.parseLong(facultyIdParam);
            } catch (NumberFormatException e) {
                System.err.println("Invalid facultyId: " + facultyIdParam);
            }
        }
        if (degreeIdParam != null && degreeIdParam.matches("^\\d+$")) {
            try {
                degreeId = Long.parseLong(degreeIdParam);
            } catch (NumberFormatException e) {
                System.err.println("Invalid degreeId: " + degreeIdParam);
            }
        }
        Page<User> page;
        try {
            if (facultyId != null || degreeId != null) {
                page = userService.getUsersFiltered(status, roleFilter, facultyId, degreeId, sortedPageable);
            } else if (StringUtils.hasText(studentId)) {
                if (roleFilter != null && status != null) {
                    page = userService.searchUsersByStudentIdAndRoleAndStatus(studentId, roleFilter, status, sortedPageable);
                } else if (roleFilter != null) {
                    page = userService.searchUsersByStudentIdAndRole(studentId, roleFilter, sortedPageable);
                } else if (status != null) {
                    page = userService.searchUsersByStudentIdAndStatus(studentId, status, sortedPageable);
                } else {
                    page = userService.searchUsersByStudentId(studentId, sortedPageable);
                }
            } else {
                page = userService.getUsers(status, roleFilter, sortedPageable);
            }
        } catch (Exception ex) {
            System.err.println("Error loading users: " + ex.getMessage());
            ex.printStackTrace();
            page = Page.empty();
        }
        var clubs = clubService.getAllClubs();
        Map<Long, Long> clubAssignments = buildClubAssignments(clubs);
        Long superAdminId = userService.getSuperAdmin().map(User::getId).orElse(null);
        model.addAttribute("userPage", page);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedRole", roleFilter);
        model.addAttribute("searchStudentId", studentId);
        model.addAttribute("selectedFacultyId", facultyId);
        model.addAttribute("selectedDegreeId", degreeId);
        model.addAttribute("selectedSort", StringUtils.hasText(sortParam) ? sortParam : "newest");
        model.addAttribute("statuses", AccountStatus.values());
        model.addAttribute("roles", Role.values());
        model.addAttribute("managedRoles", MANAGED_ROLES);
        model.addAttribute("batches", batchService.getAllBatches(Pageable.unpaged()).getContent());
        model.addAttribute("semesters", semesterService.getAllSemesters());
        model.addAttribute("clubs", clubs);
        model.addAttribute("clubAssignments", clubAssignments);
        model.addAttribute("superAdminId", superAdminId);
        model.addAttribute("canAssignSuperAdmin", userService.isSuperAdmin(actingUser));
        model.addAttribute("faculties", academicStructureService.getFaculties());
        model.addAttribute("degrees", academicStructureService.getDegrees());
        model.addAttribute("sortOptions", SORT_OPTIONS);
        attachStatusCounters(model);
        try {
            Path backupDir = getBackupDirectory();
            model.addAttribute("backupDirectory", backupDir.toString());
            model.addAttribute("availableBackups", listAvailableBackups(backupDir));
        } catch (IOException ex) {
            model.addAttribute("backupDirectoryError", "Unable to access backup directory: " + ex.getMessage());
            model.addAttribute("availableBackups", Collections.emptyList());
        }
        return "admin/users";
    }

    @GetMapping("/users/{userId}")
    public String viewUser(@PathVariable Long userId, Model model, Principal principal) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        User actingUser = resolveActingUser(principal);
        var clubs = clubService.getAllClubs();
        Map<Long, Long> clubAssignments = buildClubAssignments(clubs);
        Long superAdminId = userService.getSuperAdmin().map(User::getId).orElse(null);
        model.addAttribute("userRecord", user);
        model.addAttribute("roles", Role.values());
        model.addAttribute("managedRoles", MANAGED_ROLES);
        model.addAttribute("batches", batchService.getAllBatches(Pageable.unpaged()).getContent());
        model.addAttribute("semesters", semesterService.getAllSemesters());
        model.addAttribute("clubs", clubs);
        model.addAttribute("assignedClubId", clubAssignments.get(user.getId()));
        model.addAttribute("superAdminId", superAdminId);
        model.addAttribute("canAssignSuperAdmin", userService.isSuperAdmin(actingUser));
        model.addAttribute("faculties", academicStructureService.getFaculties());
        model.addAttribute("degrees", academicStructureService.getDegrees());
        attachStatusCounters(model);
        return "admin/user-detail";
    }

    @PostMapping("/users/staff")
    public String createStaffAccount(@RequestParam String studentId,
                                     @RequestParam String password,
                                     @RequestParam Role role,
                                     @RequestParam(required = false) String fullName,
                                     @RequestParam(required = false) String contactNumber,
                                     @RequestParam(required = false) Long clubId,
                                     RedirectAttributes redirectAttributes) {
        try {
            if (role == Role.CLUB_PRESIDENT && clubId == null) {
                throw new IllegalArgumentException("Select a club when creating a club president account");
            }
            User created = userService.createManagedUser(studentId, password, role, fullName, contactNumber);
            if (role == Role.CLUB_PRESIDENT) {
                clubService.assignPresident(clubId, created.getId());
            }
            redirectAttributes.addFlashAttribute("successMessage", "Account created and approved");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/approve")
    public String approveUser(@PathVariable Long userId,
                              @RequestParam(required = false) Role role,
                              @RequestParam(required = false) Long batchId,
                              @RequestParam(required = false) Long semesterId,
                              @RequestParam(required = false) Long clubId,
                              @RequestParam(required = false) Long facultyId,
                              @RequestParam(required = false) Long degreeId,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            User actingUser = principal != null
                    ? userService.getUserByStudentId(principal.getName()).orElse(null)
                    : null;
            Role resolvedRole = role;
            if (resolvedRole == null) {
                resolvedRole = userService.getUserById(userId)
                        .map(User::getRole)
                        .orElse(null);
            }
            if (resolvedRole == null) {
                throw new IllegalArgumentException("Assign a role before approving this account");
            }
            if (resolvedRole == Role.CLUB_PRESIDENT && clubId == null) {
                throw new IllegalArgumentException("Assign a club before approving a club president");
            }
            userService.approveUserWithAcademic(userId, role, batchId, semesterId, facultyId, degreeId, actingUser);
            if (resolvedRole == Role.CLUB_PRESIDENT) {
                clubService.assignPresident(clubId, userId);
            }
            redirectAttributes.addFlashAttribute("successMessage", "User approved successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/reject")
    public String rejectUser(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        try {
            userService.rejectUser(userId);
            redirectAttributes.addFlashAttribute("successMessage", "User rejected");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/assign")
    public String assignBatchSemesterFacultyDegree(@PathVariable Long userId,
                                         @RequestParam(required = false) Long batchId,
                                         @RequestParam(required = false) Long semesterId,
                                         @RequestParam(required = false) Long facultyId,
                                         @RequestParam(required = false) Long degreeId,
                                         RedirectAttributes redirectAttributes) {
        try {
            userService.assignBatchSemesterFacultyDegree(userId, batchId, semesterId, facultyId, degreeId);
            redirectAttributes.addFlashAttribute("successMessage", "Assignment updated");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/credentials")
    public String updateCredentials(@PathVariable Long userId,
                                    @RequestParam(required = false) String studentId,
                                    @RequestParam(required = false) String password,
                                    RedirectAttributes redirectAttributes) {
        try {
            if (!StringUtils.hasText(studentId) && !StringUtils.hasText(password)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Provide a student ID or password to update");
            } else {
                userService.updateCredentials(userId, studentId, password);
                redirectAttributes.addFlashAttribute("successMessage", "Credentials updated");
            }
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/profile")
    public String updateProfile(@PathVariable Long userId,
                                @RequestParam(required = false) String fullName,
                                @RequestParam(required = false) String contactNumber,
                                @RequestParam(required = false) String profileBio,
                                RedirectAttributes redirectAttributes) {
        try {
            if (!StringUtils.hasText(fullName) && !StringUtils.hasText(contactNumber) && !StringUtils.hasText(profileBio)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Provide at least one field to update");
            } else {
                userService.updateProfileDetails(userId, fullName, contactNumber, profileBio);
                redirectAttributes.addFlashAttribute("successMessage", "Profile updated");
            }
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/delete")
    public String deleteUser(@PathVariable Long userId,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            User target = userService.getUserById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (principal != null && target.getStudentId().equalsIgnoreCase(principal.getName())) {
                throw new IllegalArgumentException("You cannot delete your own account");
            }
            if (target.getRole() == Role.SUPER_ADMIN) {
                throw new IllegalArgumentException("The Super Admin account cannot be deleted");
            }
            userService.deleteUser(userId);
            redirectAttributes.addFlashAttribute("successMessage", "User deleted");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/backup")
    public String backupUsers(@RequestParam("filePath") String filePath,
                              RedirectAttributes redirectAttributes) {
        try {
            Path destination = resolveBackupDestination(filePath);
            userService.backupUsersToFile(destination);
            redirectAttributes.addFlashAttribute("successMessage", "Backup created at " + destination);
        } catch (IOException | RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Backup failed: " + ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/restore")
    public String restoreUsers(@RequestParam("filePath") String filePath,
                               RedirectAttributes redirectAttributes) {
        try {
            Path source = resolveRestoreSource(filePath);
            int restored = userService.restoreUsersFromFile(source);
            redirectAttributes.addFlashAttribute("successMessage", restored + " users restored");
        } catch (IOException | RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Restore failed: " + ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/batches")
    public String manageBatches(@PageableDefault(size = 12) Pageable pageable, Model model) {
        model.addAttribute("batchPage", batchService.getAllBatches(pageable));
        model.addAttribute("semesters", semesterService.getAllSemesters());
        return "admin/batches";
    }

    private AccountStatus resolveStatus(String rawStatus) {
        if (!StringUtils.hasText(rawStatus)) {
            return null;
        }
        try {
            return AccountStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Pageable applySorting(Pageable pageable, String sortParam) {
        String effectiveKey = StringUtils.hasText(sortParam) ? sortParam : "newest";
        Sort sort = switch (effectiveKey) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "name" -> Sort.by(Sort.Direction.ASC, "fullName").and(Sort.by("studentId"));
            case "faculty" -> Sort.by(Sort.Direction.ASC, "faculty.name").and(Sort.by("degree.name")).and(Sort.by("studentId"));
            case "degree" -> Sort.by(Sort.Direction.ASC, "degree.name").and(Sort.by("faculty.name")).and(Sort.by("studentId"));
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private User resolveActingUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return userService.getUserByStudentId(principal.getName()).orElse(null);
    }

    private Role resolveRole(String rawRole) {
        if (!StringUtils.hasText(rawRole) || "ALL".equalsIgnoreCase(rawRole)) {
            return null;
        }
        try {
            return Role.valueOf(rawRole.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Path getBackupDirectory() throws IOException {
        Path backupDir = Paths.get("uploads", "backups").toAbsolutePath().normalize();
        Files.createDirectories(backupDir);
        return backupDir;
    }

    private Map<Long, Long> buildClubAssignments(List<Club> clubs) {
        return clubs.stream()
                .filter(club -> club.getPresident() != null)
                .collect(Collectors.toMap(club -> club.getPresident().getId(), Club::getId, (existing, replacement) -> existing));
    }

    private void attachStatusCounters(Model model) {
        model.addAttribute("pendingCount", userService.countUsersByStatus(AccountStatus.PENDING));
        model.addAttribute("approvedCount", userService.countUsersByStatus(AccountStatus.APPROVED));
        model.addAttribute("rejectedCount", userService.countUsersByStatus(AccountStatus.REJECTED));
    }

    private List<String> listAvailableBackups(Path backupDir) throws IOException {
        try (Stream<Path> stream = Files.list(backupDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparingLong(this::lastModifiedSafe).reversed())
                    .map(path -> path.getFileName().toString())
                    .toList();
        }
    }

    private Path resolveBackupDestination(String rawInput) throws IOException {
        if (StringUtils.hasText(rawInput)) {
            String trimmed = rawInput.trim();
            try {
                Path candidate = Paths.get(trimmed);
                if (candidate.isAbsolute()) {
                    if (Files.exists(candidate) && Files.isDirectory(candidate)) {
                        return generateDefaultBackupFile(candidate);
                    }
                    if (!Files.exists(candidate) && looksLikeDirectoryInput(trimmed)) {
                        return generateDefaultBackupFile(candidate);
                    }
                    if (candidate.getParent() != null) {
                        Files.createDirectories(candidate.getParent());
                    }
                    return candidate.normalize();
                }
                Path backupDir = getBackupDirectory();
                Path resolved = backupDir.resolve(candidate).normalize();
                ensureInsideBackupDirectory(backupDir, resolved);
                if (Files.exists(resolved) && Files.isDirectory(resolved)) {
                    return generateDefaultBackupFile(resolved);
                }
                if (!Files.exists(resolved) && looksLikeDirectoryInput(trimmed)) {
                    return generateDefaultBackupFile(resolved);
                }
                if (resolved.getParent() != null) {
                    Files.createDirectories(resolved.getParent());
                }
                return resolved;
            } catch (InvalidPathException ignored) {
                // fall back to sanitized filename
            }
        }
        Path backupDir = getBackupDirectory();
        String fileName = sanitizeFileName(rawInput, true);
        return backupDir.resolve(fileName);
    }

    private Path resolveRestoreSource(String rawInput) throws IOException {
        if (StringUtils.hasText(rawInput)) {
            String trimmed = rawInput.trim();
            try {
                Path candidate = Paths.get(trimmed);
                if (candidate.isAbsolute()) {
                    if (!Files.exists(candidate)) {
                        if (looksLikeDirectoryInput(trimmed)) {
                            throw new IllegalArgumentException("Backup folder not found: " + candidate);
                        }
                        throw new IllegalArgumentException("Backup file not found: " + candidate);
                    }
                    if (Files.isDirectory(candidate)) {
                        Path latest = findLatestBackup(candidate);
                        if (latest == null) {
                            throw new IllegalArgumentException("No backup files inside " + candidate);
                        }
                        return latest;
                    }
                    return candidate.normalize();
                }
                Path backupDir = getBackupDirectory();
                Path resolved = backupDir.resolve(candidate).normalize();
                ensureInsideBackupDirectory(backupDir, resolved);
                if (!Files.exists(resolved)) {
                    if (looksLikeDirectoryInput(trimmed)) {
                        throw new IllegalArgumentException("Backup folder not found: " + resolved);
                    }
                    throw new IllegalArgumentException("Backup file not found: " + resolved.getFileName());
                }
                if (Files.isDirectory(resolved)) {
                    Path latest = findLatestBackup(resolved);
                    if (latest == null) {
                        throw new IllegalArgumentException("No backup files inside " + resolved.getFileName());
                    }
                    return latest;
                }
                return resolved;
            } catch (InvalidPathException ignored) {
                Path backupDir = getBackupDirectory();
                String fileName = sanitizeFileName(rawInput, false);
                if (!StringUtils.hasText(fileName)) {
                    Path latest = findLatestBackup(backupDir);
                    if (latest == null) {
                        throw new IllegalArgumentException("No backups available to restore.");
                    }
                    return latest;
                }
                Path resolved = backupDir.resolve(fileName).normalize();
                ensureInsideBackupDirectory(backupDir, resolved);
                if (!Files.exists(resolved)) {
                    throw new IllegalArgumentException("Backup file not found: " + resolved.getFileName());
                }
                return resolved;
            }
        }
        Path backupDir = getBackupDirectory();
        Path latest = findLatestBackup(backupDir);
        if (latest == null) {
            throw new IllegalArgumentException("No backups available to restore.");
        }
        return latest;
    }

    private void ensureInsideBackupDirectory(Path backupDir, Path candidate) {
        if (!candidate.normalize().startsWith(backupDir)) {
            throw new IllegalArgumentException("Backups must stay inside " + backupDir);
        }
    }

    private String sanitizeFileName(String rawInput, boolean allowTimestampFallback) {
        String base = StringUtils.hasText(rawInput) ? rawInput.trim() : "";
        if (base.isBlank()) {
            if (!allowTimestampFallback) {
                return "";
            }
            base = "users-backup-" + LocalDateTime.now().format(BACKUP_FORMATTER);
        }
        base = base.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!base.endsWith(".xlsx")) {
            base = base + ".xlsx";
        }
        return base;
    }

    private boolean looksLikeDirectoryInput(String rawInput) {
        if (!StringUtils.hasText(rawInput)) {
            return false;
        }
        String trimmed = rawInput.trim();
        return trimmed.endsWith("/") || trimmed.endsWith("\\");
    }

    private Path generateDefaultBackupFile(Path directory) throws IOException {
        Files.createDirectories(directory);
        String fileName = sanitizeFileName("", true);
        return directory.toAbsolutePath().normalize().resolve(fileName);
    }

    private Path findLatestBackup(Path backupDir) throws IOException {
        try (Stream<Path> stream = Files.list(backupDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .max(Comparator.comparingLong(this::lastModifiedSafe))
                    .orElse(null);
        }
    }

    private long lastModifiedSafe(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ex) {
            return 0L;
        }
    }

    @PostMapping("/batches")
    public String createBatch(@RequestParam String name,
                              @RequestParam Long semesterId,
                              RedirectAttributes redirectAttributes) {
        try {
            batchService.createBatch(name, semesterId);
            redirectAttributes.addFlashAttribute("successMessage", "Batch created");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/batches";
    }

    @PostMapping("/batches/{batchId}/move")
    public String moveBatch(@PathVariable Long batchId,
                            @RequestParam Long semesterId,
                            RedirectAttributes redirectAttributes) {
        try {
            batchService.moveBatch(batchId, semesterId);
            redirectAttributes.addFlashAttribute("successMessage", "Batch moved");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/batches";
    }

    @PostMapping("/batches/{batchId}/delete")
    public String deleteBatch(@PathVariable Long batchId,
                              RedirectAttributes redirectAttributes) {
        try {
            batchService.deleteBatch(batchId);
            redirectAttributes.addFlashAttribute("successMessage", "Batch removed");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/batches";
    }
}
