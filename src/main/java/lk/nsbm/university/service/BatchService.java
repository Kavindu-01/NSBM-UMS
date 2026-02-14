package lk.nsbm.university.service;

import lk.nsbm.university.entity.Batch;
import lk.nsbm.university.entity.Semester;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.repository.BatchRepository;
import lk.nsbm.university.repository.SemesterRepository;
import lk.nsbm.university.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BatchService {

    private final BatchRepository batchRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final SemesterResourceService semesterResourceService;
    private final TimetableService timetableService;

    public BatchService(BatchRepository batchRepository,
                       SemesterRepository semesterRepository,
                       UserRepository userRepository,
                       SemesterResourceService semesterResourceService,
                       TimetableService timetableService) {
        this.batchRepository = batchRepository;
        this.semesterRepository = semesterRepository;
        this.userRepository = userRepository;
        this.semesterResourceService = semesterResourceService;
        this.timetableService = timetableService;
    }

    @Transactional(readOnly = true)
    public Page<Batch> getAllBatches(Pageable pageable) {
        return batchRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Batch getBatchById(Long batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found"));
    }

    @Transactional
    public Batch createBatch(String name, Long semesterId) {
        if (batchRepository.existsByName(name)) {
            throw new IllegalArgumentException("Batch already exists");
        }
        Batch batch = new Batch();
        batch.setName(name.trim());
        batch.setSemester(resolveSemester(semesterId));
        Batch saved = batchRepository.save(batch);
        refreshSemesterWorkspace(saved.getSemester().getId());
        return saved;
    }

    @Transactional
    public Batch updateBatch(Long batchId, String newName, Long semesterId) {
        Batch batch = getBatchById(batchId);
        boolean semesterChanged = false;
        if (newName != null && !newName.isBlank()) {
            batch.setName(newName.trim());
        }
        if (semesterId != null) {
            Long currentSemesterId = batch.getSemester() != null ? batch.getSemester().getId() : null;
            if (!semesterId.equals(currentSemesterId)) {
                Semester newSemester = resolveSemester(semesterId);
                batch.setSemester(newSemester);
                refreshSemesterWorkspace(newSemester.getId());
                semesterChanged = true;
            }
        }
        Batch saved = batchRepository.save(batch);
        if (semesterChanged) {
            syncStudentsWithBatchSemester(saved);
        }
        return saved;
    }

    @Transactional
    public Batch moveBatch(Long batchId, Long targetSemesterId) {
        Batch batch = getBatchById(batchId);
        Semester targetSemester = resolveSemester(targetSemesterId);
        batch.setSemester(targetSemester);
        Batch saved = batchRepository.save(batch);
        syncStudentsWithBatchSemester(saved);
        refreshSemesterWorkspace(targetSemester.getId());
        return saved;
    }

    @Transactional
    public void deleteBatch(Long batchId) {
        batchRepository.deleteById(batchId);
    }

    @Transactional(readOnly = true)
    public List<Batch> getBatchesForSemester(Long semesterId) {
        Semester semester = resolveSemester(semesterId);
        return batchRepository.findBySemester(semester);
    }

    private Semester resolveSemester(Long semesterId) {
        if (semesterId == null) {
            throw new IllegalArgumentException("Semester is required");
        }
        return semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Semester not found"));
    }

    private void refreshSemesterWorkspace(Long semesterId) {
        if (semesterId == null) {
            return;
        }
        semesterResourceService.resetResourcesForSemester(semesterId);
        timetableService.clearSemesterSchedule(semesterId);
    }

    private void syncStudentsWithBatchSemester(Batch batch) {
        if (batch == null) {
            return;
        }
        List<User> users = userRepository.findByBatch(batch);
        if (users.isEmpty()) {
            return;
        }
        Semester semester = batch.getSemester();
        users.forEach(user -> user.setSemester(semester));
        userRepository.saveAll(users);
    }
}
