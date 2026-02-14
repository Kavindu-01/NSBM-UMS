package lk.nsbm.university.service;

import lk.nsbm.university.entity.AccountStatus;
import lk.nsbm.university.entity.Batch;
import lk.nsbm.university.entity.Role;
import lk.nsbm.university.entity.Semester;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.entity.Faculty;
import lk.nsbm.university.entity.DegreeProgram;
import lk.nsbm.university.repository.BatchRepository;
import lk.nsbm.university.repository.SemesterRepository;
import lk.nsbm.university.repository.UserRepository;
import lk.nsbm.university.util.ContactNumberFormatter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class UserService {
    @Transactional(readOnly = true)
    public Page<User> getUsersFiltered(AccountStatus status, Role role, Long facultyId, Long degreeId, Pageable pageable) {
        Faculty faculty = null;
        DegreeProgram degree = null;
        if (facultyId != null) {
            faculty = academicStructureService.getFaculty(facultyId);
        }
        if (degreeId != null) {
            degree = academicStructureService.getDegree(degreeId);
        }
        if (faculty != null && degree != null) {
            return userRepository.findByFacultyAndDegreeAndAccountStatusAndRole(faculty, degree, status, role, pageable);
        } else if (faculty != null) {
            return userRepository.findByFacultyAndAccountStatusAndRole(faculty, status, role, pageable);
        } else if (degree != null) {
            return userRepository.findByDegreeAndAccountStatusAndRole(degree, status, role, pageable);
        } else {
            return getUsers(status, role, pageable);
        }
    }
    @Transactional(readOnly = true)
    public Page<User> searchUsersByStudentId(String studentId, Pageable pageable) {
        return userRepository.findByStudentIdContainingIgnoreCase(studentId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> searchUsersByStudentIdAndRole(String studentId, Role role, Pageable pageable) {
        return userRepository.findByStudentIdContainingIgnoreCaseAndRole(studentId, role, pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> searchUsersByStudentIdAndStatus(String studentId, AccountStatus status, Pageable pageable) {
        return userRepository.findByStudentIdContainingIgnoreCaseAndAccountStatus(studentId, status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> searchUsersByStudentIdAndRoleAndStatus(String studentId, Role role, AccountStatus status, Pageable pageable) {
        return userRepository.findByStudentIdContainingIgnoreCaseAndRoleAndAccountStatus(studentId, role, status, pageable);
    }

    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final SemesterRepository semesterRepository;
        private final PasswordEncoder passwordEncoder;
        private final AcademicStructureService academicStructureService;
        private static final String[] BACKUP_HEADERS = {
            "Student ID",
            "Password Hash",
            "Role",
            "Status",
            "Batch ID",
            "Batch Name",
            "Semester ID",
            "Semester Name",
            "Full Name",
            "Contact Number",
            "Avatar Path",
            "Profile Bio",
            "Recovery Code Hash"
    };

        private static final int MIN_PASSWORD_LENGTH = 6;
        private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
        private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
        private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
        private static final Pattern SYMBOL_PATTERN = Pattern.compile("[^A-Za-z0-9\\s]");

        private static final Map<String, String> ALLOWED_AVATAR_TYPES = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/webp", "webp"
        );

        private static final Set<String> ALLOWED_AVATAR_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");

        private static final String GUARDIAN_ID_PREFIX = "BG-";
        private static final int GUARDIAN_ID_DIGITS = 6;

        private static final Set<Role> SELF_MANAGED_ROLES = EnumSet.of(
            Role.USER,
            Role.BOARDING_GUARDIAN,
            Role.SHUTTLE_DRIVER
        );
        private static final String RECOVERY_CODE_REGEX = "^[0-9]{6}$";

        private final SecureRandom secureRandom = new SecureRandom();

    public UserService(UserRepository userRepository,
                       BatchRepository batchRepository,
                       SemesterRepository semesterRepository,
                       PasswordEncoder passwordEncoder,
                       AcademicStructureService academicStructureService) {
        this.userRepository = userRepository;
        this.batchRepository = batchRepository;
        this.semesterRepository = semesterRepository;
        this.passwordEncoder = passwordEncoder;
        this.academicStructureService = academicStructureService;
    }

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> getUsersByStatus(AccountStatus status, Pageable pageable) {
        return userRepository.findByAccountStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> getUsers(AccountStatus status, Role role, Pageable pageable) {
        if (role != null && status != null) {
            return userRepository.findByRoleAndAccountStatus(role, status, pageable);
        }
        if (role != null) {
            return userRepository.findByRole(role, pageable);
        }
        if (status != null) {
            return getUsersByStatus(status, pageable);
        }
        return getAllUsers(pageable);
    }

    @Transactional(readOnly = true)
    public List<User> getUsersByRole(Role role) {
        if (role == null) {
            return List.of();
        }
        return userRepository.findByRole(role);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByStudentId(String studentId) {
        return userRepository.findByStudentId(studentId)
                .map(this::primeUserContext);
    }

    @Transactional
    public User registerUser(String studentId, String rawPassword, String recoveryCode) {
        return registerPendingAccount(studentId, rawPassword, Role.USER, null, null, recoveryCode);
    }

    @Transactional
    public User registerGuardian(String studentId, String rawPassword) {
        return registerGuardianAccount(rawPassword, null, null, null);
    }

    public User registerGuardian(String studentId,
                                 String rawPassword,
                                 String fullName,
                                 String contactNumber,
                                 String recoveryCode) {
        return registerGuardianAccount(rawPassword, fullName, contactNumber, recoveryCode);
    }

    private User registerGuardianAccount(String rawPassword,
                                         String fullName,
                                         String contactNumber,
                                         String recoveryCode) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("Password is required");
        }
        String normalizedContact = normalizeContactNumber(contactNumber);
        if (!StringUtils.hasText(normalizedContact)) {
            throw new IllegalArgumentException("Contact number is required to register as a boarding guardian");
        }
        ensureGuardianContactUniqueness(normalizedContact, null);
        String guardianId = generateGuardianId();
        return registerPendingAccount(guardianId, rawPassword, Role.BOARDING_GUARDIAN, fullName, normalizedContact, recoveryCode);
    }

    @Transactional
    public User registerShuttleDriver(String studentId, String rawPassword) {
        return registerShuttleDriver(studentId, rawPassword, null, null, null);
    }

    public User registerShuttleDriver(String studentId,
                                      String rawPassword,
                                      String fullName,
                                      String contactNumber,
                                      String recoveryCode) {
        return registerPendingAccount(studentId, rawPassword, Role.SHUTTLE_DRIVER, fullName, contactNumber, recoveryCode);
    }

    @Transactional
    public User createApprovedUser(String studentId, String rawPassword, Role role) {
        return createApprovedUser(studentId, rawPassword, role, null, null, null);
    }

    public User createApprovedUser(String studentId,
                                   String rawPassword,
                                   Role role,
                                   String fullName,
                                   String contactNumber) {
        return createApprovedUser(studentId, rawPassword, role, fullName, contactNumber, null);
    }

    public User createApprovedUser(String studentId,
                                   String rawPassword,
                                   Role role,
                                   String fullName,
                                   String contactNumber,
                                   String recoveryCode) {
        if (!StringUtils.hasText(studentId) || !StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("Student ID and password are required");
        }
        if (userRepository.existsByStudentId(studentId)) {
            throw new IllegalArgumentException("Student ID already registered");
        }
        requirePasswordStrength(rawPassword);
        ensureSuperAdminUniqueness(role, null);
        User user = new User();
        user.setStudentId(studentId.trim());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setAccountStatus(AccountStatus.APPROVED);
        user.setFullName(normalizeProfileValue(fullName));
        user.setContactNumber(normalizeContactNumber(contactNumber));
        user.setRecoveryCodeHash(encodeRecoveryCode(recoveryCode, role));
        return userRepository.save(user);
    }

    @Transactional
    public User createManagedUser(String studentId, String rawPassword, Role role) {
        return createManagedUser(studentId, rawPassword, role, null, null);
    }

    public User createManagedUser(String studentId,
                                  String rawPassword,
                                  Role role,
                                  String fullName,
                                  String contactNumber) {
        if (role == null || role == Role.USER || role == Role.BOARDING_GUARDIAN) {
            throw new IllegalArgumentException("This role must self-register through its own portal");
        }
        if (role == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("The Super Admin role is reserved and cannot be provisioned from this form");
        }
        return createApprovedUser(studentId, rawPassword, role, fullName, contactNumber, null);
    }

    private User registerPendingAccount(String studentId,
                                        String rawPassword,
                                        Role role,
                                        String fullName,
                                        String contactNumber,
                                        String recoveryCode) {
        if (!StringUtils.hasText(studentId) || !StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("ID and password are required");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role selection is required");
        }
        if (userRepository.existsByStudentId(studentId)) {
            throw new IllegalArgumentException("ID already registered");
        }
        requirePasswordStrength(rawPassword);
        ensureSuperAdminUniqueness(role, null);
        String normalizedContact = normalizeContactNumber(contactNumber);
        if (role == Role.BOARDING_GUARDIAN && !StringUtils.hasText(normalizedContact)) {
            throw new IllegalArgumentException("Contact number is required to register as a boarding guardian");
        }
        String encodedRecovery = encodeRecoveryCode(recoveryCode, role);
        User user = new User();
        user.setStudentId(studentId.trim());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setAccountStatus(AccountStatus.PENDING);
        user.setFullName(normalizeProfileValue(fullName));
        user.setContactNumber(normalizedContact);
        user.setRecoveryCodeHash(encodedRecovery);
        return userRepository.save(user);
    }

    private User primeUserContext(User user) {
        if (user == null) {
            return null;
        }
        if (user.getBatch() != null) {
            user.getBatch().getName();
        }
        if (user.getSemester() != null) {
            user.getSemester().getName();
        }
        if (user.getFaculty() != null) {
            user.getFaculty().getName();
        }
        if (user.getDegree() != null) {
            user.getDegree().getName();
        }
        return user;
    }

    @Transactional
    public void approveUser(Long userId, Role targetRole, Long batchId, Long semesterId, User actingUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Role resolvedRole = targetRole != null ? targetRole : user.getRole();
        enforceSuperAdminAssignmentPrivileges(resolvedRole, actingUser);
        ensureSuperAdminUniqueness(resolvedRole, user.getId());
        user.setRole(resolvedRole);
        user.setAccountStatus(AccountStatus.APPROVED);
        boolean requiresCohort = resolvedRole == Role.USER;
        if (requiresCohort && batchId == null) {
            throw new IllegalArgumentException("Select a batch before approving a student account");
        }
        Batch batch = resolveBatch(batchId);
        user.setBatch(batch);
        user.setSemester(resolveSemesterForAssignment(semesterId, batch));
        userRepository.save(user);
    }

    @Transactional
    public void approveUserWithAcademic(Long userId, Role targetRole, Long batchId, Long semesterId, Long facultyId, Long degreeId, User actingUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Role resolvedRole = targetRole != null ? targetRole : user.getRole();
        enforceSuperAdminAssignmentPrivileges(resolvedRole, actingUser);
        ensureSuperAdminUniqueness(resolvedRole, user.getId());
        user.setRole(resolvedRole);
        user.setAccountStatus(AccountStatus.APPROVED);
        boolean requiresCohort = resolvedRole == Role.USER;
        if (requiresCohort && batchId == null) {
            throw new IllegalArgumentException("Select a batch before approving a student account");
        }
        Batch batch = resolveBatch(batchId);
        user.setBatch(batch);
        user.setSemester(resolveSemesterForAssignment(semesterId, batch));
        applyAcademicAssignments(user, facultyId, degreeId);
        userRepository.save(user);
    }

    @Transactional
    public void rejectUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        enforceSuperAdminProtection(user, "rejected");
        user.setAccountStatus(AccountStatus.REJECTED);
        userRepository.save(user);
    }

    @Transactional
    public void assignBatchAndSemester(Long userId, Long batchId, Long semesterId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean requiresCohort = user.getRole() == Role.USER;
        if (requiresCohort && batchId == null) {
            throw new IllegalArgumentException("Assign a batch before updating this student");
        }
        Batch batch = resolveBatch(batchId);
        user.setBatch(batch);
        user.setSemester(resolveSemesterForAssignment(semesterId, batch));
        userRepository.save(user);
    }

    @Transactional
    public void assignBatchSemesterFacultyDegree(Long userId, Long batchId, Long semesterId, Long facultyId, Long degreeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean requiresCohort = user.getRole() == Role.USER;
        if (requiresCohort && batchId == null) {
            throw new IllegalArgumentException("Assign a batch before updating this student");
        }
        Batch batch = resolveBatch(batchId);
        user.setBatch(batch);
        user.setSemester(resolveSemesterForAssignment(semesterId, batch));
        applyAcademicAssignments(user, facultyId, degreeId);
        userRepository.save(user);
    }

    @Transactional
    public void updateCredentials(Long userId, String newStudentId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (isSelfManagedRole(user.getRole())) {
            throw new IllegalArgumentException("This role manages its own credentials");
        }

        boolean updated = false;
        if (StringUtils.hasText(newStudentId) && !newStudentId.equalsIgnoreCase(user.getStudentId())) {
            if (userRepository.existsByStudentId(newStudentId.trim())) {
                throw new IllegalArgumentException("Student ID already registered");
            }
            user.setStudentId(newStudentId.trim());
            updated = true;
        }
        if (StringUtils.hasText(newPassword)) {
            requirePasswordStrength(newPassword);
            user.setPassword(passwordEncoder.encode(newPassword));
            updated = true;
        }
        if (updated) {
            userRepository.save(user);
        }
    }

    @Transactional
    public void updatePasswordSelf(Long userId, String currentPassword, String newPassword) {
        if (!StringUtils.hasText(currentPassword)) {
            throw new IllegalArgumentException("Enter your current password");
        }
        requirePasswordStrength(newPassword);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        enforcePasswordDifference(user, newPassword, currentPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void updateRecoveryCode(Long userId, String recoveryCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!isSelfManagedRole(user.getRole())) {
            throw new IllegalArgumentException("Recovery codes are only required for specific roles");
        }
        String normalized = normalizeRecoveryCode(recoveryCode);
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("Recovery code is required");
        }
        validateRecoveryCodeFormat(normalized);
        user.setRecoveryCodeHash(passwordEncoder.encode(normalized));
        userRepository.save(user);
    }

    @Transactional
    public void resetPasswordWithRecovery(String identifier, String recoveryCode, String newPassword) {
        if (!StringUtils.hasText(identifier) || !StringUtils.hasText(recoveryCode)) {
            throw new IllegalArgumentException("Identifier and recovery code are required");
        }
        requirePasswordStrength(newPassword);
        User user = resolveUserByIdentifier(identifier);
        if (!isSelfManagedRole(user.getRole())) {
            throw new IllegalArgumentException("This account resets credentials through administrators");
        }
        if (user.getRecoveryCodeHash() == null) {
            throw new IllegalArgumentException("Recovery code is not set for this account");
        }
        String normalizedCode = normalizeRecoveryCode(recoveryCode);
        if (!StringUtils.hasText(normalizedCode) || !passwordEncoder.matches(normalizedCode, user.getRecoveryCodeHash())) {
            throw new IllegalArgumentException("Recovery code is invalid");
        }
        enforcePasswordDifference(user, newPassword, null);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void updateProfileDetails(Long userId, String fullName, String contactNumber, String profileBio) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String normalizedName = normalizeProfileValue(fullName);
        String normalizedContact = normalizeContactNumber(contactNumber);
        String normalizedBio = normalizeBio(profileBio);

        boolean updated = false;
        if (!Objects.equals(user.getFullName(), normalizedName)) {
            user.setFullName(normalizedName);
            updated = true;
        }
        boolean contactChanged = !Objects.equals(user.getContactNumber(), normalizedContact);
        if (user.getRole() == Role.BOARDING_GUARDIAN) {
            if (!StringUtils.hasText(normalizedContact)) {
                throw new IllegalArgumentException("Boarding guardians must keep a contact number on file");
            }
            if (contactChanged) {
                ensureGuardianContactUniqueness(normalizedContact, user.getId());
            }
        }
        if (contactChanged) {
            user.setContactNumber(normalizedContact);
            updated = true;
        }
        if (!Objects.equals(user.getProfileBio(), normalizedBio)) {
            user.setProfileBio(normalizedBio);
            updated = true;
        }
        if (updated) {
            userRepository.save(user);
        }
    }

    @Transactional
    public void updateProfileAvatar(Long userId, MultipartFile avatarFile, Path mediaRoot) throws IOException {
        if (avatarFile == null || avatarFile.isEmpty()) {
            throw new IllegalArgumentException("Attach an image to update the avatar");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String extension = resolveAvatarExtension(avatarFile);
        Path avatarDir = mediaRoot.resolve("avatars").normalize();
        Files.createDirectories(avatarDir);
        String fileName = buildAvatarFileName(userId, extension);
        Path target = avatarDir.resolve(fileName).normalize();
        if (!target.startsWith(avatarDir)) {
            throw new IllegalArgumentException("Invalid avatar destination path");
        }
        try (InputStream inputStream = avatarFile.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
        deleteExistingAvatarFile(user, mediaRoot);
        user.setAvatarPath("avatars/" + fileName);
        userRepository.save(user);
    }

    @Transactional
    public void removeProfileAvatar(Long userId, Path mediaRoot) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        deleteExistingAvatarFile(user, mediaRoot);
        user.setAvatarPath(null);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public long countUsersByStatus(AccountStatus status) {
        return userRepository.countByAccountStatus(status);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        enforceSuperAdminProtection(user, "deleted");
        userRepository.delete(user);
    }

    @Transactional
    public Path backupUsersToFile(Path destinationFile) throws IOException {
        Path parent = destinationFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Users");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < BACKUP_HEADERS.length; i++) {
                headerRow.createCell(i).setCellValue(BACKUP_HEADERS[i]);
            }
            int rowIndex = 1;
            for (User user : userRepository.findAll()) {
                Row row = sheet.createRow(rowIndex++);
                int col = 0;
                row.createCell(col++).setCellValue(user.getStudentId());
                row.createCell(col++).setCellValue(user.getPassword());
                row.createCell(col++).setCellValue(user.getRole().name());
                row.createCell(col++).setCellValue(user.getAccountStatus().name());
                row.createCell(col++).setCellValue(user.getBatch() != null && user.getBatch().getId() != null ? user.getBatch().getId().toString() : "");
                row.createCell(col++).setCellValue(user.getBatch() != null && user.getBatch().getName() != null ? user.getBatch().getName() : "");
                row.createCell(col++).setCellValue(user.getSemester() != null && user.getSemester().getId() != null ? user.getSemester().getId().toString() : "");
                row.createCell(col++).setCellValue(user.getSemester() != null && user.getSemester().getName() != null ? user.getSemester().getName() : "");
                row.createCell(col++).setCellValue(user.getFullName() != null ? user.getFullName() : "");
                row.createCell(col++).setCellValue(user.getContactNumber() != null ? user.getContactNumber() : "");
                row.createCell(col++).setCellValue(user.getAvatarPath() != null ? user.getAvatarPath() : "");
                row.createCell(col++).setCellValue(user.getProfileBio() != null ? user.getProfileBio() : "");
                row.createCell(col).setCellValue(user.getRecoveryCodeHash() != null ? user.getRecoveryCodeHash() : "");
            }
            for (int i = 0; i < BACKUP_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            try (OutputStream outputStream = Files.newOutputStream(destinationFile)) {
                workbook.write(outputStream);
            }
        }
        return destinationFile;
    }

    @Transactional
    public int restoreUsersFromFile(Path sourceFile) throws IOException {
        if (!Files.exists(sourceFile)) {
            throw new IllegalArgumentException("Backup file not found: " + sourceFile);
        }
        int restoredCount = 0;
        try (InputStream inputStream = Files.newInputStream(sourceFile);
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return 0;
            }
            DataFormatter formatter = new DataFormatter();
            int firstDataRow = sheet.getFirstRowNum() + 1;
            for (int rowIndex = firstDataRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                String studentId = readCellValue(row, 0, formatter);
                if (!StringUtils.hasText(studentId)) {
                    continue;
                }
                Role role = parseRoleSafely(readCellValue(row, 2, formatter));
                AccountStatus status = parseStatusSafely(readCellValue(row, 3, formatter));
                if (role == null || status == null) {
                    continue;
                }
                User user = userRepository.findByStudentId(studentId).orElseGet(User::new);
                user.setStudentId(studentId);
                String passwordHash = readCellValue(row, 1, formatter);
                if (StringUtils.hasText(passwordHash)) {
                    user.setPassword(passwordHash);
                }
                user.setRole(role);
                user.setAccountStatus(status);
                Batch resolvedBatch = resolveBatchFromBackup(
                        readCellValue(row, 4, formatter),
                        readCellValue(row, 5, formatter)
                );
                user.setBatch(resolvedBatch);

                Semester resolvedSemester = resolveSemesterFromBackup(
                        readCellValue(row, 6, formatter),
                        readCellValue(row, 7, formatter)
                );
                user.setSemester(resolvedSemester);

                user.setFullName(normalizeProfileValue(readCellValue(row, 8, formatter)));
                user.setContactNumber(normalizeContactNumber(readCellValue(row, 9, formatter)));
                user.setAvatarPath(sanitizeAvatarPath(readCellValue(row, 10, formatter)));
                user.setProfileBio(normalizeBio(readCellValue(row, 11, formatter)));
                String recoveryHash = readCellValue(row, 12, formatter);
                user.setRecoveryCodeHash(StringUtils.hasText(recoveryHash) ? recoveryHash : null);
                ensureSuperAdminUniqueness(user.getRole(), user.getId());
                userRepository.save(user);
                restoredCount++;
            }
        }
        return restoredCount;
    }

    private String resolveAvatarExtension(MultipartFile avatarFile) {
        String contentType = avatarFile.getContentType();
        if (contentType != null && ALLOWED_AVATAR_TYPES.containsKey(contentType)) {
            return ALLOWED_AVATAR_TYPES.get(contentType);
        }
        String originalExtension = StringUtils.getFilenameExtension(avatarFile.getOriginalFilename());
        if (!StringUtils.hasText(originalExtension)) {
            throw new IllegalArgumentException("Unsupported image format. Use PNG, JPG, or WEBP");
        }
        String normalized = originalExtension.trim().toLowerCase();
        if (!ALLOWED_AVATAR_EXTENSIONS.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported image format. Use PNG, JPG, or WEBP");
        }
        return normalized.equals("jpeg") ? "jpg" : normalized;
    }

    private String buildAvatarFileName(Long userId, String extension) {
        return "avatar-" + userId + "-" + System.currentTimeMillis() + "." + extension;
    }

    private void deleteExistingAvatarFile(User user, Path mediaRoot) throws IOException {
        if (!StringUtils.hasText(user.getAvatarPath())) {
            return;
        }
        Path avatarFile = mediaRoot.resolve(user.getAvatarPath()).normalize();
        if (avatarFile.startsWith(mediaRoot)) {
            Files.deleteIfExists(avatarFile);
        }
    }

    private Batch resolveBatchFromBackup(String batchIdValue, String batchNameValue) {
        if (StringUtils.hasText(batchIdValue)) {
            Long batchId = parseLongSilently(batchIdValue);
            if (batchId != null) {
                try {
                    return resolveBatch(batchId);
                } catch (IllegalArgumentException ignored) {
                    // Fall back to name lookup
                }
            }
        }
        if (StringUtils.hasText(batchNameValue)) {
            return batchRepository.findByName(batchNameValue.trim()).orElse(null);
        }
        return null;
    }

    private Semester resolveSemesterFromBackup(String semesterIdValue, String semesterNameValue) {
        if (StringUtils.hasText(semesterIdValue)) {
            Long semesterId = parseLongSilently(semesterIdValue);
            if (semesterId != null) {
                try {
                    return resolveSemester(semesterId);
                } catch (IllegalArgumentException ignored) {
                    // Fall back to name lookup
                }
            }
        }
        if (StringUtils.hasText(semesterNameValue)) {
            return semesterRepository.findByName(semesterNameValue.trim()).orElse(null);
        }
        return null;
    }

    private Batch resolveBatch(Long batchId) {
        if (batchId == null) {
            return null;
        }
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found"));
    }

    private Semester resolveSemester(Long semesterId) {
        if (semesterId == null) {
            return null;
        }
        return semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Semester not found"));
    }

    private Semester resolveSemesterForAssignment(Long semesterId, Batch batch) {
        if (batch != null && batch.getSemester() != null) {
            if (semesterId != null && !batch.getSemester().getId().equals(semesterId)) {
                throw new IllegalArgumentException("Selected semester does not match the batch assignment");
            }
            return batch.getSemester();
        }
        if (semesterId != null) {
            return resolveSemester(semesterId);
        }
        return null;
    }

    private Long parseLongSilently(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Role parseRoleSafely(String value) {
        try {
            return Role.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return null;
        }
    }

    private AccountStatus parseStatusSafely(String value) {
        try {
            return AccountStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return null;
        }
    }

    private void applyAcademicAssignments(User user, Long facultyId, Long degreeId) {
        boolean facultyProvided = facultyId != null;
        boolean degreeProvided = degreeId != null;

        Faculty faculty = null;
        if (facultyProvided) {
            faculty = academicStructureService.getFaculty(facultyId);
            user.setFaculty(faculty);
            if (!degreeProvided) {
                user.setDegree(null);
            }
        }

        if (degreeProvided) {
            DegreeProgram degree = academicStructureService.getDegree(degreeId);
            Faculty effectiveFaculty = facultyProvided ? faculty : user.getFaculty();
            if (effectiveFaculty == null) {
                throw new IllegalArgumentException("Select a faculty before assigning a degree");
            }
            if (degree.getFaculty() == null || !degree.getFaculty().getId().equals(effectiveFaculty.getId())) {
                throw new IllegalArgumentException("Degree does not belong to the selected faculty");
            }
            user.setDegree(degree);
        }
    }

    private String readCellValue(Row row, int index, DataFormatter formatter) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(index, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        return formatter.formatCellValue(cell).trim();
    }

    private String normalizeProfileValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void requirePasswordStrength(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Password is required");
        }
        if (value.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }
        if (!UPPERCASE_PATTERN.matcher(value).find()) {
            throw new IllegalArgumentException("Password must include at least one uppercase letter");
        }
        if (!LOWERCASE_PATTERN.matcher(value).find()) {
            throw new IllegalArgumentException("Password must include at least one lowercase letter");
        }
        if (!DIGIT_PATTERN.matcher(value).find()) {
            throw new IllegalArgumentException("Password must include at least one number");
        }
        if (!SYMBOL_PATTERN.matcher(value).find()) {
            throw new IllegalArgumentException("Password must include at least one symbol");
        }
    }

    private String normalizeContactNumber(String value) {
        return ContactNumberFormatter.normalize(value);
    }

    private String normalizeRecoveryCode(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateRecoveryCodeFormat(String code) {
        if (!StringUtils.hasText(code) || !code.matches(RECOVERY_CODE_REGEX)) {
            throw new IllegalArgumentException("Recovery code must be exactly 6 digits");
        }
    }

    private String normalizeBio(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        int max = 600;
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    private String sanitizeAvatarPath(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.replace("\\", "/").trim();
        if (normalized.contains("..")) {
            return null;
        }
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String encodeRecoveryCode(String recoveryCode, Role role) {
        if (!isSelfManagedRole(role)) {
            return null;
        }
        String normalized = normalizeRecoveryCode(recoveryCode);
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("Recovery code is required for this role");
        }
        validateRecoveryCodeFormat(normalized);
        return passwordEncoder.encode(normalized);
    }

    private void ensureGuardianContactUniqueness(String contactNumber, Long currentUserId) {
        if (!StringUtils.hasText(contactNumber)) {
            return;
        }
        userRepository.findByContactNumber(contactNumber)
                .filter(existing -> currentUserId == null || !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Contact number already registered");
                });
    }

    private String generateGuardianId() {
        int max = (int) Math.pow(10, GUARDIAN_ID_DIGITS);
        for (int attempt = 0; attempt < 10; attempt++) {
            int number = secureRandom.nextInt(max);
            String candidate = GUARDIAN_ID_PREFIX + String.format("%0" + GUARDIAN_ID_DIGITS + "d", number);
            if (!userRepository.existsByStudentId(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to allocate a guardian ID. Please try again in a moment.");
    }

    private User resolveUserByIdentifier(String identifier) {
        String trimmed = StringUtils.hasText(identifier) ? identifier.trim() : "";
        if (!StringUtils.hasText(trimmed)) {
            throw new IllegalArgumentException("Provide a student ID or phone number");
        }
        Optional<User> byStudentId = userRepository.findByStudentId(trimmed);
        if (byStudentId.isPresent()) {
            return byStudentId.get();
        }
        String normalizedContact = normalizeContactNumber(trimmed);
        if (StringUtils.hasText(normalizedContact)) {
            return userRepository.findByContactNumber(normalizedContact)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found for the provided identifier"));
        }
        throw new IllegalArgumentException("Account not found for the provided identifier");
    }

    @Transactional(readOnly = true)
    public Optional<User> getSuperAdmin() {
        return userRepository.findFirstByRole(Role.SUPER_ADMIN);
    }

    public boolean isSuperAdmin(User user) {
        return user != null && user.getRole() == Role.SUPER_ADMIN;
    }

    public boolean isSelfManagedRole(Role role) {
        return role != null && SELF_MANAGED_ROLES.contains(role);
    }

    @Transactional
    public void promoteDefaultAdminToSuperAdmin(String studentId) {
        if (!StringUtils.hasText(studentId)) {
            return;
        }
        if (userRepository.findFirstByRole(Role.SUPER_ADMIN).isPresent()) {
            return;
        }
        userRepository.findByStudentId(studentId.trim())
                .ifPresent(user -> {
                    user.setRole(Role.SUPER_ADMIN);
                    userRepository.save(user);
                });
    }

    private void ensureSuperAdminUniqueness(Role role, Long currentUserId) {
        if (role != Role.SUPER_ADMIN) {
            return;
        }
        Optional<User> existing = userRepository.findFirstByRole(Role.SUPER_ADMIN);
        if (existing.isPresent() && (currentUserId == null || !existing.get().getId().equals(currentUserId))) {
            throw new IllegalStateException("A Super Admin already exists. Demote the current Super Admin before assigning another.");
        }
    }

    private void enforceSuperAdminProtection(User user, String action) {
        if (isSuperAdmin(user)) {
            throw new IllegalArgumentException("The Super Admin account cannot be " + action + ".");
        }
    }

    private void enforceSuperAdminAssignmentPrivileges(Role roleToAssign, User actingUser) {
        if (roleToAssign != Role.SUPER_ADMIN) {
            return;
        }
        if (actingUser == null || actingUser.getRole() != Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("Only the Super Admin can assign the Super Admin role.");
        }
    }

    // Prevents trivial password reuse even when only the hashed value is available.
    private void enforcePasswordDifference(User user, String newPassword, String referencePlaintext) {
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password cannot match the previous password");
        }
        if (StringUtils.hasText(referencePlaintext) && isPasswordTooSimilar(referencePlaintext, newPassword)) {
            throw new IllegalArgumentException("New password is too similar to the current password");
        }
    }

    private boolean isPasswordTooSimilar(String oldPassword, String newPassword) {
        String normalizedOld = normalizePasswordForSimilarity(oldPassword);
        String normalizedNew = normalizePasswordForSimilarity(newPassword);
        if (!StringUtils.hasText(normalizedOld) || !StringUtils.hasText(normalizedNew)) {
            return false;
        }
        if (normalizedOld.equals(normalizedNew)) {
            return true;
        }
        int distance = computeLevenshteinDistance(normalizedOld, normalizedNew);
        int maxLength = Math.max(normalizedOld.length(), normalizedNew.length());
        int threshold = Math.max(1, maxLength / 4);
        return distance <= threshold;
    }

    private String normalizePasswordForSimilarity(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private int computeLevenshteinDistance(String first, String second) {
        int len1 = first.length();
        int len2 = second.length();
        int[] previous = new int[len2 + 1];
        int[] current = new int[len2 + 1];
        for (int j = 0; j <= len2; j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= len1; i++) {
            current[0] = i;
            for (int j = 1; j <= len2; j++) {
                int cost = first.charAt(i - 1) == second.charAt(j - 1) ? 0 : 1;
                int insertion = current[j - 1] + 1;
                int deletion = previous[j] + 1;
                int substitution = previous[j - 1] + cost;
                current[j] = Math.min(Math.min(insertion, deletion), substitution);
            }
            int[] temp = previous;
            previous = current;
            current = temp;
        }
        return previous[len2];
    }
}
