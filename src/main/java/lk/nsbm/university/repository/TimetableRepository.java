package lk.nsbm.university.repository;

import lk.nsbm.university.entity.Batch;
import lk.nsbm.university.entity.Semester;
import lk.nsbm.university.entity.Timetable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    
    Page<Timetable> findAll(Pageable pageable);
    
    List<Timetable> findByBatchAndSemesterOrderByDayOfWeekAscTimeSlotAsc(Batch batch, Semester semester);
    
    Page<Timetable> findByBatchAndSemester(Batch batch, Semester semester, Pageable pageable);

    List<Timetable> findBySemesterOrderByDayOfWeekAscTimeSlotAsc(Semester semester);

    void deleteBySemester(Semester semester);
}
