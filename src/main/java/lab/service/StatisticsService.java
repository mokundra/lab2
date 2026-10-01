package lab.service;

import lab.model.Student;

import java.io.IOException;

public class StatisticsService {

    @FunctionalInterface
    private interface Operation {
        void execute() throws IOException;
    }

    private void checkService(StudentService studentService) {
        if (studentService == null) {
            throw new IllegalArgumentException("StudentService не может быть null");
        }
    }

    private long measure(Operation operation) throws IOException {
        long start = System.nanoTime();

        operation.execute();

        long end = System.nanoTime();

        return end - start;
    }

    public long measureAdd(StudentService studentService, Student student) throws IOException {
        checkService(studentService);

        if (student == null) {
            throw new IllegalArgumentException("Student не может быть null");
        }

        return measure(() -> studentService.add(student));
    }

    public long measureFindById(StudentService studentService, long id) throws IOException {
        checkService(studentService);

        return measure(() -> studentService.findById(id));
    }

    public long measureFindByGrade(StudentService studentService, int grade) throws IOException {
        checkService(studentService);

        return measure(() -> studentService.findByGrade(grade));
    }

    public long measureDeleteById(StudentService studentService, long id) throws IOException {
        checkService(studentService);

        return measure(() -> studentService.deleteById(id));
    }

    public long measureDeleteByGrade(StudentService studentService, int grade) throws IOException {
        checkService(studentService);

        return measure(() -> studentService.deleteByGrade(grade));
    }

    public double nanosecondsToMilliseconds(long nanoseconds) {
        if (nanoseconds < 0) {
            throw new IllegalArgumentException("Время не может быть отрицательным");
        }

        return nanoseconds / 1000000.0;
    }


}

