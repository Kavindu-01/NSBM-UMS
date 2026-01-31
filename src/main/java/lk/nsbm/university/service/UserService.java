package lk.nsbm.university.service;

import lk.nsbm.university.entity.AccountStatus;
import lk.nsbm.university.entity.Batch;
import lk.nsbm.university.entity.Role;
import lk.nsbm.university.entity.Semester;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.repository.BatchRepository;
import lk.nsbm.university.repository.SemesterRepository;
import lk.nsbm.university.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final SemesterRepository semesterRepository;
    private final PasswordEncoder passwordEncoder;
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
            "Contact Number"
    };

    public UserService(UserRepository userRepository,
                       BatchRepository batchRepository,
                       SemesterRepository semesterRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.batchRepository = batchRepository;
        this.semesterRepository = semesterRepository;
        this.passwordEncoder = passwordEncoder;
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
        return userRepository.findByStudentId(studentId);
    }

    @Transactional
    public User registerUser(String studentId, String rawPassword) {
        return registerPendingAccount(studentId, rawPassword, Role.USER);
    }

    @Transactional
    public User registerGuardian(String studentId, String rawPassword) {
        return registerGuardian(studentId, rawPassword, null, null);
    }

    public User registerGuardian(String studentId,
                                 String rawPassword,
                                 String fullName,
                                 String contactNumber) {
        return registerPendingAccount(studentId, rawPassword, Role.BOARDING_GUARDIAN, fullName, contactNumber);
    }

    @Transactional
    public User registerShuttleDriver(String studentId, String rawPassword) {
        return registerShuttleDriver(studentId, rawPassword, null, null);
    }

    public User registerShuttleDriver(String studentId,
                                      String rawPassword,
                                      String fullName,
                                      String contactNumber) {
        return registerPendingAccount(studentId, rawPassword, Role.SHUTTLE_DRIVER, fullName, contactNumber);
    }

    @Transactional
    public User createApprovedUser(String studentId, String rawPassword, Role role) {
        return createApprovedUser(studentId, rawPassword, role, null, null);
    }

    public User createApprovedUser(String studentId,
                                   String rawPassword,
                                   Role role,
                                   String fullName,
                                   String contactNumber) {
        if (!StringUtils.hasText(studentId) || !StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("Student ID and password are required");
        }
        if (userRepository.existsByStudentId(studentId)) {
            throw new IllegalArgumentException("Student ID already registered");
        }
        User user = new User();
        user.setStudentId(studentId.trim());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setAccountStatus(AccountStatus.APPROVED);
        user.setFullName(normalizeProfileValue(fullName));
        user.setContactNumber(normalizeProfileValue(contactNumber));
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
        return createApprovedUser(studentId, rawPassword, role, fullName, contactNumber);
    }

    private User registerPendingAccount(String studentId, String rawPassword, Role role) {
        return registerPendingAccount(studentId, rawPassword, role, null, null);
    }

    private User registerPendingAccount(String studentId,
                                        String rawPassword,
                                        Role role,
                                        String fullName,
                                        String contactNumber) {
        if (!StringUtils.hasText(studentId) || !StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("ID and password are required");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role selection is required");
        }
        if (userRepository.existsByStudentId(studentId)) {
            throw new IllegalArgumentException("ID already registered");
        }
        User user = new User();
        user.setStudentId(studentId.trim());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setAccountStatus(AccountStatus.PENDING);
        user.setFullName(normalizeProfileValue(fullName));
        user.setContactNumber(normalizeProfileValue(contactNumber));
        return userRepository.save(user);
    }

    @Transactional
    public void approveUser(Long userId, Role targetRole, Long batchId, Long semesterId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Role resolvedRole = targetRole != null ? targetRole : user.getRole();
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
    public void rejectUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
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
    public void updateCredentials(Long userId, String newStudentId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean updated = false;
        if (StringUtils.hasText(newStudentId) && !newStudentId.equalsIgnoreCase(user.getStudentId())) {
            if (userRepository.existsByStudentId(newStudentId.trim())) {
                throw new IllegalArgumentException("Student ID already registered");
            }
            user.setStudentId(newStudentId.trim());
            updated = true;
        }
        if (StringUtils.hasText(newPassword)) {
            user.setPassword(passwordEncoder.encode(newPassword));
            updated = true;
        }
        if (updated) {
            userRepository.save(user);
        }
    }

    @Transactional
    public void updateProfileDetails(Long userId, String fullName, String contactNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String normalizedName = normalizeProfileValue(fullName);
        String normalizedContact = normalizeProfileValue(contactNumber);

        boolean updated = false;
        if (!java.util.Objects.equals(user.getFullName(), normalizedName)) {
            user.setFullName(normalizedName);
            updated = true;
        }
        if (!java.util.Objects.equals(user.getContactNumber(), normalizedContact)) {
            user.setContactNumber(normalizedContact);
            updated = true;
        }
        if (updated) {
            userRepository.save(user);
        }
    }

    @Transactional(readOnly = true)
    public long countUsersByStatus(AccountStatus status) {
        return userRepository.countByAccountStatus(status);
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found");
        }
        userRepository.deleteById(userId);
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
                row.createCell(col).setCellValue(user.getContactNumber() != null ? user.getContactNumber() : "");
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
                user.setContactNumber(normalizeProfileValue(readCellValue(row, 9, formatter)));
                userRepository.save(user);
                restoredCount++;
            }
        }
        return restoredCount;
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
}
