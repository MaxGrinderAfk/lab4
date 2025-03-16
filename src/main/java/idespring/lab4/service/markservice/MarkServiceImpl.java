package idespring.lab4.service.markservice;

import idespring.lab4.exceptions.EntityNotFoundException;
import idespring.lab4.exceptions.SubjectNotAssignedException;
import idespring.lab4.model.Mark;
import idespring.lab4.model.Student;
import idespring.lab4.model.Subject;
import idespring.lab4.repository.markrepo.MarkRepository;
import idespring.lab4.repository.studentrepo.StudentRepository;
import idespring.lab4.repository.subjectrepo.SubjectRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarkServiceImpl implements MarkService {
    private final MarkRepository markRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private static final Logger logger = LoggerFactory.getLogger(MarkServiceImpl.class);

    @Autowired
    public MarkServiceImpl(MarkRepository markRepository,
                           StudentRepository studentRepository,
                           SubjectRepository subjectRepository) {
        this.markRepository = markRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
    }

    @Override
    @Cacheable(value = "marks", key = "'marks-' + (#studentId != null ? #studentId : 'all') + '-' "
            + "+ (#subjectId != null ? #subjectId : 'all')",
            unless = "#result == null or #result.isEmpty()")
    public List<Mark> readMarks(Long studentId, Long subjectId) {
        long start = System.nanoTime();
        logger.info("Fetching marks for student: {}, subject: {}", studentId, subjectId);

        List<Mark> marks;
        if (studentId != null && subjectId != null) {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Student not found with id: " + studentId));
            Subject subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Subject not found with id: " + subjectId));
            marks = markRepository.findByStudentAndSubject(student, subject);
        } else if (studentId != null) {
            marks = markRepository.findByStudentId(studentId);
        } else if (subjectId != null) {
            marks = markRepository.findBySubjectId(subjectId);
        } else {
            marks = markRepository.findAll();
        }

        long end = System.nanoTime();
        logger.info("Execution time for readMarks: {} ms", (end - start) / 1_000_000);
        return marks;
    }

    @Override
    @Cacheable(value = "marks", key = "'value-' + #value",
            unless = "#result == null or #result.isEmpty()")
    public List<Mark> findByValue(int value) {
        logger.info("Fetching marks with value: {}", value);
        return markRepository.findByValue(value);
    }

    @Override
    @Cacheable(value = "marks", key = "'avg-student-' + #studentId", unless = "#result == null")
    public Double getAverageMarkByStudentId(Long studentId) {
        logger.info("Fetching average mark for student: {}", studentId);
        return markRepository.getAverageMarkByStudentId(studentId);
    }

    @Override
    @CacheEvict(value = "marks", allEntries = true)
    @Transactional
    public void deleteMarkSpecific(Long studentId, String subjectName, int markValue, Long id) {
        logger.info("Deleting specific mark for student: {}, subject: {}, value: {}, id: {}",
                studentId, subjectName, markValue, id);
        int deletedCount = markRepository.deleteMarkByStudentIdSubjectNameValueAndOptionalId(
                studentId, subjectName, markValue, id);
        if (deletedCount == 0) {
            throw new EntityNotFoundException("Mark not found with the given criteria.");
        }
    }

    @Override
    @Cacheable(value = "marks", key = "'avg-subject-' + #subjectId", unless = "#result == null")
    public Double getAverageMarkBySubjectId(Long subjectId) {
        logger.info("Fetching average mark for subject: {}", subjectId);
        return markRepository.getAverageMarkBySubjectId(subjectId);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "marks", key = "'avg-student-' + #mark.student.id"),
            @CacheEvict(value = "marks", key = "'avg-subject-' + #mark.subject.id"),
            @CacheEvict(value = "marks", allEntries = true)
    })
    @Transactional
    public Mark addMark(Mark mark) {
        long start = System.nanoTime();
        logger.info("Adding mark for student: {}, subject: {}, value: {}",
                mark.getStudent().getId(), mark.getSubject().getId(), mark.getValue());

        Student student = studentRepository.findById(mark.getStudent().getId())
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: "
                        + mark.getStudent().getId()));
        Subject subject = subjectRepository.findById(mark.getSubject().getId())
                .orElseThrow(() -> new EntityNotFoundException("Subject not found with id: "
                        + mark.getSubject().getId()));

        boolean hasSubject = student.getSubjects().stream()
                .anyMatch(s -> s.getId().equals(subject.getId()));

        if (!hasSubject) {
            throw new SubjectNotAssignedException("Student with ID " + student.getId()
                    + " does not have subject with ID " + subject.getId());
        }

        Mark savedMark = markRepository.save(mark);

        long end = System.nanoTime();
        logger.info("Execution time for addMark: {} ms", (end - start) / 1_000_000);
        return savedMark;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "marks", key = "#id"),
            @CacheEvict(value = "marks", allEntries = true)
    })
    public void deleteMark(Long id) {
        logger.info("Deleting mark with id: {}", id);
        markRepository.deleteById(id);
    }
}