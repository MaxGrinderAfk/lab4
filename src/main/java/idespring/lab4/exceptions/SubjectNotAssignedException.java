package idespring.lab4.exceptions;

public class SubjectNotAssignedException extends RuntimeException {
    public SubjectNotAssignedException(String message) {
        super(message);
    }
}
