package idespring.lab4.service.studservice;

import idespring.lab4.exceptions.EntityNotFoundException;
import idespring.lab4.model.Mark;
import idespring.lab4.model.Student;
import idespring.lab4.model.Subject;
import idespring.lab4.repository.studentrepo.StudentRepository;
import jakarta.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

@Service
public class StudentServiceImpl implements StudentServ {
    private final StudentRepository studentRepository;
    private static final String NOTFOUND = "Student not found with id: ";
    private static final Logger logger = LoggerFactory.getLogger(StudentServiceImpl.class);

    @Autowired
    public StudentServiceImpl(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    @Cacheable(value = "students", key = "#age + '-' + #sort + '-' + #id",
            unless = "#result == null or #result.isEmpty()")
    public List<Student> readStudents(Integer age, String sort, Long id) {
        long start = System.nanoTime();
        logger.info("Fetching students from database with age: {}, sort: {}, id: {}",
                age, sort, id);

        List<Student> students;
        if (id != null) {
            students = Collections.singletonList(
                    studentRepository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException(NOTFOUND + id))
            );
        } else if (age != null && sort != null) {
            students = studentRepository.findByAgeAndSortByName(age, sort);
        } else if (age != null) {
            students = studentRepository.findByAge(age).stream().toList();
        } else if (sort != null) {
            students = studentRepository.sortByName(sort);
        } else {
            students = studentRepository.findAll();
        }

        long end = System.nanoTime();
        logger.info("Execution time for readStudents: {} ms", (end - start) / 1_000_000);
        return students;
    }

    @Override
    @Cacheable(value = "students", key = "'group-' + #groupId",
            unless = "#result == null or #result.isEmpty()")
    public List<Student> findByGroupId(Long groupId) {
        logger.info("Fetching students from group ID: {}", groupId);
        return studentRepository.findByGroupId(groupId).stream().toList();
    }

    @Override
    @Cacheable(value = "students", key = "#id", unless = "#result == null")
    public Student findById(Long id) {
        long start = System.nanoTime();
        logger.info("Fetching student from database with id: {}", id);

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(NOTFOUND + id));

        long end = System.nanoTime();
        logger.info("Execution time for findById: {} ms", (end - start) / 1_000_000);
        return student;
    }

    @Override
    @CachePut(value = "students", key = "#result.id")
    @CacheEvict(value = {"studentSubjects", "marks"}, allEntries = true)
    public Student addStudent(Student student) {
        final long start = System.nanoTime();
        logger.info("Saving student: {}", student.getName());

        Set<Long> subjectIds = student.getSubjects().stream()
                .map(Subject::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Mark mark : student.getMarks()) {
            mark.setStudent(student);
        }

        student.setSubjects(new HashSet<>());
        Student savedStudent = studentRepository.save(student);

        for (Long subjectId : subjectIds) {
            studentRepository.addSubject(savedStudent.getId(), subjectId);
        }

        long end = System.nanoTime();
        logger.info("Execution time for addStudent: {} ms", (end - start) / 1_000_000);
        return savedStudent;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "students", key = "#id"),
            @CacheEvict(value = "students", key = "'group-' + #id")
    })
    public void updateStudent(String name, int age, long id) {
        logger.info("Updating student with id: {}", id);
        studentRepository.update(name, age, id);
        logger.info("Student with id {} updated", id);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "students", key = "#id"),
            @CacheEvict(value = {"marks", "studentSubjects"}, allEntries = true)
    })
    @Transactional
    public void deleteStudent(long id) {
        logger.info("Deleting student with id: {}", id);
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(NOTFOUND + id));
        student.getSubjects().clear();
        studentRepository.saveAndFlush(student);
        studentRepository.delete(student);
        logger.info("Student with id {} deleted", id);
    }
}
