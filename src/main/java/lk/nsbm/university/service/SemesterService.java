package lk.nsbm.university.service;

import lk.nsbm.university.entity.Semester;
import lk.nsbm.university.repository.SemesterRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SemesterService {

    private final SemesterRepository semesterRepository;
    
    public SemesterService(SemesterRepository semesterRepository) {
        this.semesterRepository = semesterRepository;
    }

    private static final List<DefaultSemester> DEFAULT_SEMESTERS = List.of(
            new DefaultSemester("Y1 S1", 1, 1),
            new DefaultSemester("Y1 S2", 1, 2),
            new DefaultSemester("Y2 S1", 2, 1),
            new DefaultSemester("Y2 S2", 2, 2),
            new DefaultSemester("Y3 S1", 3, 1),
            new DefaultSemester("Y3 S2", 3, 2),
            new DefaultSemester("Y4 S1", 4, 1),
            new DefaultSemester("Y4 S2", 4, 2)
    );

    @Transactional
    public void initializeDefaultSemesters() {
        for (DefaultSemester defaultSemester : DEFAULT_SEMESTERS) {
            if (!semesterRepository.existsByName(defaultSemester.name())) {
                Semester semester = new Semester();
                semester.setName(defaultSemester.name());
                semester.setYearLevel(defaultSemester.yearLevel());
                semester.setSemesterNumber(defaultSemester.semesterNumber());
                semesterRepository.save(semester);
            }
        }
    }

    @Transactional
    public Semester createSemester(String name, int yearLevel, int semesterNumber) {
        if (semesterRepository.existsByName(name)) {
            throw new IllegalArgumentException("Semester already exists");
        }
        Semester semester = new Semester();
        semester.setName(name);
        semester.setYearLevel(yearLevel);
        semester.setSemesterNumber(semesterNumber);
        return semesterRepository.save(semester);
    }

    @Transactional(readOnly = true)
    public List<Semester> getAllSemesters() {
        return semesterRepository.findAll(Sort.by(Sort.Direction.ASC, "yearLevel", "semesterNumber"));
    }

    @Transactional(readOnly = true)
    public Semester getSemesterById(Long semesterId) {
        return semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Semester not found"));
    }

    @Transactional
    public Semester renameSemester(Long semesterId, String newName) {
        Semester semester = getSemesterById(semesterId);
        semester.setName(newName);
        return semesterRepository.save(semester);
    }

    private record DefaultSemester(String name, int yearLevel, int semesterNumber) {}
}
