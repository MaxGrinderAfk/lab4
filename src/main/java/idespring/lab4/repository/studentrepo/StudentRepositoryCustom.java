package idespring.lab4.repository.studentrepo;

import idespring.lab4.model.Student;
import java.util.List;

public interface StudentRepositoryCustom {
    List<Student> findByAgeAndSortByName(int age, String sort);

    List<Student> sortByName(String sort);
}
