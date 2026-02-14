package lk.nsbm.university.service;

import lk.nsbm.university.entity.Batch;
import lk.nsbm.university.entity.Semester;
import lk.nsbm.university.entity.Timetable;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.repository.BatchRepository;
import lk.nsbm.university.repository.SemesterRepository;
import lk.nsbm.university.repository.TimetableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TimetableService {

    private final TimetableRepository timetableRepository;
    private final BatchRepository batchRepository;
    private final SemesterRepository semesterRepository;

    public TimetableService(TimetableRepository timetableRepository,
                            BatchRepository batchRepository,
                            SemesterRepository semesterRepository) {
        this.timetableRepository = timetableRepository;
        this.batchRepository = batchRepository;
        this.semesterRepository = semesterRepository;
    }

    @Transactional(readOnly = true)
    public Page<Timetable> getTimetables(Pageable pageable) {
        return timetableRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Timetable getTimetableById(Long timetableId) {
        return timetableRepository.findById(timetableId)
                .orElseThrow(() -> new IllegalArgumentException("Timetable entry not found"));
    }

    @Transactional
    public Timetable createTimetable(Timetable timetable, Long batchId, Long semesterId, User createdBy) {
        timetable.setBatch(resolveBatch(batchId));
        timetable.setSemester(resolveSemester(semesterId));
        timetable.setCreatedBy(createdBy);
        return timetableRepository.save(timetable);
    }

    @Transactional
    public Timetable updateTimetable(Long timetableId, Timetable updatedDetails, Long batchId, Long semesterId) {
        Timetable existing = getTimetableById(timetableId);
        existing.setTitle(updatedDetails.getTitle());
        existing.setSubject(updatedDetails.getSubject());
        existing.setDayOfWeek(updatedDetails.getDayOfWeek());
        existing.setTimeSlot(updatedDetails.getTimeSlot());
        existing.setLecturer(updatedDetails.getLecturer());
        existing.setLocation(updatedDetails.getLocation());
        existing.setBatch(resolveBatch(batchId));
        existing.setSemester(resolveSemester(semesterId));
        return timetableRepository.save(existing);
    }

    @Transactional
    public void deleteTimetable(Long timetableId) {
        timetableRepository.deleteById(timetableId);
    }

    @Transactional(readOnly = true)
    public List<Timetable> getStructuredTimetable(Long batchId, Long semesterId) {
        Batch batch = resolveBatch(batchId);
        Semester semester = resolveSemester(semesterId);
        return timetableRepository.findByBatchAndSemesterOrderByDayOfWeekAscTimeSlotAsc(batch, semester);
    }

    @Transactional(readOnly = true)
    public List<Timetable> getTimetablesForSemester(Long semesterId) {
        Semester semester = resolveSemester(semesterId);
        return timetableRepository.findBySemesterOrderByDayOfWeekAscTimeSlotAsc(semester);
    }

    @Transactional
    public void clearSemesterSchedule(Long semesterId) {
        Semester semester = resolveSemester(semesterId);
        timetableRepository.deleteBySemester(semester);
    }

    private Batch resolveBatch(Long batchId) {
        if (batchId == null) {
            throw new IllegalArgumentException("Batch is required");
        }
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found"));
    }

    private Semester resolveSemester(Long semesterId) {
        if (semesterId == null) {
            throw new IllegalArgumentException("Semester is required");
        }
        return semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Semester not found"));
    }
}
