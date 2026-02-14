package lk.nsbm.university.service;

import lk.nsbm.university.entity.Batch;
import lk.nsbm.university.entity.Faculty;
import lk.nsbm.university.entity.DegreeProgram;
import lk.nsbm.university.entity.Semester;
import lk.nsbm.university.entity.SemesterResource;
import lk.nsbm.university.entity.SemesterResourceType;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.repository.BatchRepository;
import lk.nsbm.university.repository.SemesterRepository;
import lk.nsbm.university.repository.SemesterResourceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class SemesterResourceService {

    private final SemesterResourceRepository resourceRepository;
    private final SemesterRepository semesterRepository;
    private final BatchRepository batchRepository;
    private final AcademicStructureService academicStructureService;
    private final Path storageRoot;

    public SemesterResourceService(SemesterResourceRepository resourceRepository,
                                   SemesterRepository semesterRepository,
                                   BatchRepository batchRepository,
                                   AcademicStructureService academicStructureService,
                                   @Value("${storage.semester-root:uploads/semester}") String storageRoot) {
        this.resourceRepository = resourceRepository;
        this.semesterRepository = semesterRepository;
        this.batchRepository = batchRepository;
        this.academicStructureService = academicStructureService;
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageRoot);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to initialize semester resource storage", ex);
        }
    }


    @Transactional(readOnly = true)
    public List<SemesterResource> getResourcesForSemester(Long semesterId) {
        Semester semester = resolveSemester(semesterId);
        return resourceRepository.findBySemesterOrderByCreatedAtDesc(semester);
    }

    @Transactional(readOnly = true)
    public List<SemesterResource> getResourcesForSemesterAndFaculty(Long semesterId, Long facultyId) {
        Semester semester = resolveSemester(semesterId);
        return resourceRepository.findBySemesterAndFacultyIdOrderByCreatedAtDesc(semester, facultyId);
    }

    @Transactional(readOnly = true)
    public List<SemesterResource> getResourcesForSemesterAndDegree(Long semesterId, Long degreeId) {
        Semester semester = resolveSemester(semesterId);
        return resourceRepository.findBySemesterAndDegreeIdOrderByCreatedAtDesc(semester, degreeId);
    }

    @Transactional(readOnly = true)
    public List<SemesterResource> getResourcesForSemesterAndFacultyAndDegree(Long semesterId, Long facultyId, Long degreeId) {
        Semester semester = resolveSemester(semesterId);
        return resourceRepository.findBySemesterAndFacultyIdAndDegreeIdOrderByCreatedAtDesc(semester, facultyId, degreeId);
    }

    @Transactional(readOnly = true)
    public long countBySemester(Long semesterId) {
        Semester semester = semesterRepository.getReferenceById(semesterId);
        return resourceRepository.countBySemester(semester);
    }

    @Transactional(readOnly = true)
    public long countAllResources() {
        return resourceRepository.count();
    }

    @Transactional(readOnly = true)
    public SemesterResource getResource(Long resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Semester resource not found"));
    }

    @Transactional
    public SemesterResource addResource(Long semesterId,
                                        Long batchId,
                                        Long facultyId,
                                        Long degreeId,
                                        SemesterResourceType type,
                                        String title,
                                        String textContent,
                                        MultipartFile file,
                                        User uploadedBy) {
        Semester semester = resolveSemester(semesterId);
        Batch batch = resolveBatch(batchId);
        Faculty faculty = null;
        DegreeProgram degree = null;
        if (facultyId != null) {
            faculty = academicStructureService.getFaculty(facultyId);
        }
        if (degreeId != null) {
            degree = academicStructureService.getDegree(degreeId);
        }

        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("Title is required");
        }

        boolean hasFile = file != null && !file.isEmpty();
        boolean hasText = StringUtils.hasText(textContent);
        if (!hasFile && !hasText) {
            throw new IllegalArgumentException("Provide a description or upload a file");
        }

        SemesterResource resource = new SemesterResource();
        resource.setSemester(semester);
        resource.setBatch(batch);
        resource.setFaculty(faculty);
        resource.setDegree(degree);
        resource.setType(type);
        resource.setTitle(title.trim());
        resource.setTextContent(hasText ? textContent.trim() : null);
        resource.setUploadedBy(uploadedBy);

        if (hasFile) {
            FilePayload payload = storeFile(file, semester.getId());
            resource.setFileName(payload.originalName());
            resource.setFileType(payload.contentType());
            resource.setFileSize(payload.size());
            resource.setStoragePath(payload.relativePath());
        }

        return resourceRepository.save(resource);
    }

    @Transactional
    public void deleteResource(Long resourceId) {
        SemesterResource resource = getResource(resourceId);
        deleteFileIfPresent(resource.getStoragePath());
        resourceRepository.delete(resource);
    }

    @Transactional
    public void resetResourcesForSemester(Long semesterId) {
        Semester semester = resolveSemester(semesterId);
        resourceRepository.findBySemesterOrderByCreatedAtDesc(semester)
                .forEach(resource -> deleteFileIfPresent(resource.getStoragePath()));
        resourceRepository.deleteBySemester(semester);
    }

    @Transactional(readOnly = true)
    public Resource loadFile(Long resourceId) {
        SemesterResource resource = getResource(resourceId);
        if (!StringUtils.hasText(resource.getStoragePath())) {
            throw new IllegalArgumentException("Resource does not contain a downloadable file");
        }
        Path fullPath = storageRoot.resolve(resource.getStoragePath()).normalize();
        if (!Files.exists(fullPath)) {
            throw new IllegalArgumentException("Requested file is no longer available");
        }
        try {
            return new UrlResource(fullPath.toUri());
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Failed to load stored resource", ex);
        }
    }

    private FilePayload storeFile(MultipartFile file, Long semesterId) {
        try {
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String storedFilename = UUID.randomUUID() + "_" + originalFilename;
            Path semesterFolder = storageRoot.resolve("semester-" + semesterId);
            Files.createDirectories(semesterFolder);
            Path destination = semesterFolder.resolve(storedFilename);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            String relativePath = storageRoot.relativize(destination).toString().replace("\\", "/");
            return new FilePayload(originalFilename, file.getContentType(), file.getSize(), relativePath);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store uploaded file", ex);
        }
    }

    private void deleteFileIfPresent(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return;
        }
        Path path = storageRoot.resolve(relativePath).normalize();
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Non-blocking cleanup
        }
    }

    private Semester resolveSemester(Long semesterId) {
        if (semesterId == null) {
            throw new IllegalArgumentException("Semester is required");
        }
        return semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Semester not found"));
    }

    private Batch resolveBatch(Long batchId) {
        if (batchId == null) {
            return null;
        }
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found"));
    }

    private record FilePayload(String originalName, String contentType, Long size, String relativePath) {
    }
}
