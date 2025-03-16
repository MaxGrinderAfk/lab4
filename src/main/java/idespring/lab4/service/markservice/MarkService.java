package idespring.lab4.service.markservice;

import idespring.lab4.model.Mark;
import java.util.List;

public interface MarkService {
    List<Mark> readMarks(Long studentId, Long subjectId);

    List<Mark> findByValue(int value);

    void deleteMarkSpecific(Long studentId, String subjectName, int markValue, Long id);

    Double getAverageMarkByStudentId(Long studentId);

    Double getAverageMarkBySubjectId(Long subjectId);

    Mark addMark(Mark mark);

    void deleteMark(Long id);
}