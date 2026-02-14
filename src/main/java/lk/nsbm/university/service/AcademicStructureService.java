package lk.nsbm.university.service;

import lk.nsbm.university.entity.DegreeProgram;
import lk.nsbm.university.entity.Faculty;
import lk.nsbm.university.repository.DegreeProgramRepository;
import lk.nsbm.university.repository.FacultyRepository;
import lk.nsbm.university.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;


@Service
public class AcademicStructureService {

    private final FacultyRepository facultyRepository;
    private final DegreeProgramRepository degreeRepository;
    private final UserRepository userRepository;

    public AcademicStructureService(FacultyRepository facultyRepository,
                                    DegreeProgramRepository degreeRepository,
                                    UserRepository userRepository) {
        this.facultyRepository = facultyRepository;
        this.degreeRepository = degreeRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Faculty> getFaculties() {
        return facultyRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Transactional(readOnly = true)
    public List<DegreeProgram> getDegrees() {
        return degreeRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Transactional(readOnly = true)
    public List<DegreeProgram> getDegreesForFaculty(Long facultyId) {
        if (facultyId == null) {
            return List.of();
        }
        Faculty faculty = getFaculty(facultyId);
        return degreeRepository.findByFacultyOrderByNameAsc(faculty);
    }

    @Transactional(readOnly = true)
    public Faculty getFaculty(Long facultyId) {
        if (facultyId == null) {
            throw new IllegalArgumentException("Faculty is required");
        }
        return facultyRepository.findById(facultyId)
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found"));
    }

    @Transactional(readOnly = true)
    public DegreeProgram getDegree(Long degreeId) {
        if (degreeId == null) {
            throw new IllegalArgumentException("Degree is required");
        }
        return degreeRepository.findById(degreeId)
                .orElseThrow(() -> new IllegalArgumentException("Degree not found"));
    }

    @Transactional
    public Faculty createFaculty(String name, String code, String description) {
        String cleanedName = cleanup(name);
        String cleanedCode = cleanup(code);
        if (!StringUtils.hasText(cleanedName) || !StringUtils.hasText(cleanedCode)) {
            throw new IllegalArgumentException("Faculty name and code are required");
        }
        if (facultyRepository.existsByNameIgnoreCase(cleanedName) || facultyRepository.existsByCodeIgnoreCase(cleanedCode)) {
            throw new IllegalArgumentException("Faculty name or code already exists");
        }
        Faculty faculty = new Faculty();
        faculty.setName(cleanedName);
        faculty.setCode(cleanedCode.toUpperCase());
        faculty.setDescription(cleanup(description));
        return facultyRepository.save(faculty);
    }

    @Transactional
    public Faculty updateFaculty(Long facultyId, String name, String code, String description) {
        Faculty existing = getFaculty(facultyId);
        String cleanedName = cleanup(name);
        String cleanedCode = cleanup(code);
        if (!StringUtils.hasText(cleanedName) || !StringUtils.hasText(cleanedCode)) {
            throw new IllegalArgumentException("Faculty name and code are required");
        }
        boolean nameChanged = !existing.getName().equalsIgnoreCase(cleanedName);
        boolean codeChanged = !existing.getCode().equalsIgnoreCase(cleanedCode);
        if (nameChanged && facultyRepository.existsByNameIgnoreCase(cleanedName)) {
            throw new IllegalArgumentException("Another faculty already uses this name");
        }
        if (codeChanged && facultyRepository.existsByCodeIgnoreCase(cleanedCode)) {
            throw new IllegalArgumentException("Another faculty already uses this code");
        }
        existing.setName(cleanedName);
        existing.setCode(cleanedCode.toUpperCase());
        existing.setDescription(cleanup(description));
        return facultyRepository.save(existing);
    }

    @Transactional
    public void deleteFaculty(Long facultyId) {
        Faculty faculty = getFaculty(facultyId);
        long degreeCount = degreeRepository.findByFacultyOrderByNameAsc(faculty).size();
        long userCount = userRepository.countByFaculty(faculty);
        if (degreeCount > 0 || userCount > 0) {
            throw new IllegalArgumentException("Remove degrees and unassign users before deleting this faculty");
        }
        facultyRepository.delete(faculty);
    }

    @Transactional
    public DegreeProgram createDegree(Long facultyId, String name, String code, String description, String duration) {
        Faculty faculty = getFaculty(facultyId);
        String cleanedName = cleanup(name);
        String cleanedCode = cleanup(code);
        if (!StringUtils.hasText(cleanedName) || !StringUtils.hasText(cleanedCode)) {
            throw new IllegalArgumentException("Degree name and code are required");
        }
        if (degreeRepository.existsByCodeIgnoreCase(cleanedCode)) {
            throw new IllegalArgumentException("Degree code already exists");
        }
        DegreeProgram degree = new DegreeProgram();
        degree.setFaculty(faculty);
        degree.setName(cleanedName);
        degree.setCode(cleanedCode.toUpperCase());
        degree.setDescription(cleanup(description));
        degree.setDuration(cleanup(duration));
        return degreeRepository.save(degree);
    }

    @Transactional
    public DegreeProgram updateDegree(Long degreeId,
                                      Long facultyId,
                                      String name,
                                      String code,
                                      String description,
                                      String duration) {
        DegreeProgram degree = getDegree(degreeId);
        Faculty faculty = facultyId != null ? getFaculty(facultyId) : degree.getFaculty();
        String cleanedName = cleanup(name);
        String cleanedCode = cleanup(code);
        if (!StringUtils.hasText(cleanedName) || !StringUtils.hasText(cleanedCode)) {
            throw new IllegalArgumentException("Degree name and code are required");
        }
        boolean codeChanged = !degree.getCode().equalsIgnoreCase(cleanedCode);
        if (codeChanged && degreeRepository.existsByCodeIgnoreCase(cleanedCode)) {
            throw new IllegalArgumentException("Another degree already uses this code");
        }
        degree.setFaculty(faculty);
        degree.setName(cleanedName);
        degree.setCode(cleanedCode.toUpperCase());
        degree.setDescription(cleanup(description));
        degree.setDuration(cleanup(duration));
        return degreeRepository.save(degree);
    }

    @Transactional
    public void deleteDegree(Long degreeId) {
        DegreeProgram degree = getDegree(degreeId);
        long userCount = userRepository.countByDegree(degree);
        if (userCount > 0) {
            throw new IllegalArgumentException("Unassign this degree from users before deleting it");
        }
        degreeRepository.delete(degree);
    }

    private String cleanup(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
