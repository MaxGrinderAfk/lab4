package idespring.lab4.service.studentsubjserv;

import idespring.lab4.model.Student;
import idespring.lab4.model.Subject;
import java.util.List;
import java.util.Set;

public interface StudentSubjectService {
    void addSubjectToStudent(Long studentId, Long subjectId);

    void removeSubjectFromStudent(Long studentId, Long subjectId);

    List<Subject> getSubjectsByStudent(Long studentId);

    Set<Student> getStudentsBySubject(Long subjectId);

    Student findStudentWithSubjects(Long studentId);

    Subject findSubjectWithStudents(Long subjectId);
}