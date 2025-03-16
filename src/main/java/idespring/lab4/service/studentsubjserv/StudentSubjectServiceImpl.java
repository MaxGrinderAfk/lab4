package idespring.lab4.service.studentsubjserv;

import idespring.lab4.exceptions.EntityNotFoundException;
import idespring.lab4.model.Student;
import idespring.lab4.model.Subject;
import idespring.lab4.repository.studentrepo.StudentRepository;
import idespring.lab4.repository.subjectrepo.SubjectRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class StudentSubjectServiceImpl implements StudentSubjectService {
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private static final String STUDENTERR = "Student not found";
    private static final String SUBJECTERR = "Subject not found";
    private static final Logger logger = LoggerFactory.getLogger(StudentSubjectServiceImpl.class);

    @Autowired
    public StudentSubjectServiceImpl(StudentRepository studentRepository,
                                     SubjectRepository subjectRepository) {
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
    }

    @Override
    @CacheEvict(value = {"studentSubjects", "students"}, allEntries = true)
    @Transactional
    public void addSubjectToStudent(Long studentId, Long subjectId) {
        logger.info("Adding subject {} to student {}", subjectId, studentId);

        studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException(STUDENTERR));

        subjectRepository.findById(subjectId)
                .orElseThrow(() -> new EntityNotFoundException(SUBJECTERR));

        studentRepository.addSubject(studentId, subjectId);
        logger.info("Subject {} added to student {}", subjectId, studentId);
    }

    @Override
    @CacheEvict(value = {"studentSubjects", "students"}, allEntries = true)
    @Transactional
    public void removeSubjectFromStudent(Long studentId, Long subjectId) {
        logger.info("Removing subject {} from student {}", subjectId, studentId);

        studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException(STUDENTERR));

        subjectRepository.findById(subjectId)
                .orElseThrow(() -> new EntityNotFoundException(SUBJECTERR));

        studentRepository.removeSubject(studentId, subjectId);
        logger.info("Subject {} removed from student {}", subjectId, studentId);
    }

    @Override
    @Cacheable(value = "studentSubjects", key = "'subjects-' + #studentId",
            unless = "#result == null or #result.isEmpty()")
    public List<Subject> getSubjectsByStudent(Long studentId) {
        long start = System.nanoTime();
        logger.info("Fetching subjects for student {}", studentId);

        List<Subject> subjects = subjectRepository.findByStudentId(studentId);

        long end = System.nanoTime();
        logger.info("Execution time for getSubjectsByStudent: {} ms", (end - start) / 1_000_000);
        return subjects;
    }

    @Override
    @Cacheable(value = "studentSubjects", key = "'students-' + #subjectId",
            unless = "#result == null or #result.isEmpty()")
    public Set<Student> getStudentsBySubject(Long subjectId) {
        long start = System.nanoTime();
        logger.info("Fetching students for subject {}", subjectId);

        Subject subject = subjectRepository.findByIdWithStudents(subjectId)
                .orElseThrow(() -> new EntityNotFoundException(SUBJECTERR));

        long end = System.nanoTime();
        logger.info("Execution time for getStudentsBySubject: {} ms", (end - start) / 1_000_000);
        return subject.getStudents();
    }

    @Override
    @Cacheable(value = "studentSubjects", key = "'student-with-subjects-' + #studentId",
            unless = "#result == null or #result.getSubjects().isEmpty()")
    public Student findStudentWithSubjects(Long studentId) {
        long start = System.nanoTime();
        logger.info("Fetching student with subjects for ID: {}", studentId);

        Student student = studentRepository.findByIdWithSubjects(studentId)
                .orElseThrow(() -> new EntityNotFoundException(STUDENTERR));

        long end = System.nanoTime();
        logger.info("Execution time for findStudentWithSubjects: {} ms", (end - start) / 1_000_000);
        return student;
    }

    @Override
    @Cacheable(value = "studentSubjects", key = "'subject-with-students-' + #subjectId",
            unless = "#result == null or #result.getStudents().isEmpty()")
    public Subject findSubjectWithStudents(Long subjectId) {
        long start = System.nanoTime();
        logger.info("Fetching subject with students for ID: {}", subjectId);

        Subject subject = subjectRepository.findByIdWithStudents(subjectId)
                .orElseThrow(() -> new EntityNotFoundException(SUBJECTERR));

        long end = System.nanoTime();
        logger.info("Execution time for findSubjectWithStudents: {} ms", (end - start) / 1_000_000);
        return subject;
    }
}