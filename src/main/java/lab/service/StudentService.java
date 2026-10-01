package lab.service;

import lab.database.Database;
import lab.database.DatabaseHeader;
import lab.database.RecordManager;
import lab.index.FreeSpaceManager;
import lab.index.GradeIndex;
import lab.index.PrimaryIndex;
import lab.model.Student;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StudentService {

    private final Database database;
    private final PrimaryIndex primaryIndex;
    private final GradeIndex gradeIndex;
    private final FreeSpaceManager freeSpaceManager;

    public StudentService(Database database, PrimaryIndex primaryIndex,
                          GradeIndex gradeIndex, FreeSpaceManager freeSpaceManager) {

        if (database == null || primaryIndex == null || gradeIndex == null || freeSpaceManager == null) {
            throw new IllegalArgumentException("Компоненты базы данных не могут быть null");
        }

        this.database = database;
        this.primaryIndex = primaryIndex;
        this.gradeIndex = gradeIndex;
        this.freeSpaceManager = freeSpaceManager;

    }

    private void checkDatabase() {
        if (!database.isOpen()) {
            throw new IllegalStateException("База данных не открыта");
        }
    }

    private void checkStudent(Student student) {
        if (student == null) {
            throw new IllegalArgumentException("Student не может быть null");
        }

        if (student.getId() <= 0) {
            throw new IllegalArgumentException("ID должен быть положительным");
        }

        if (student.getGrade() < 1 || student.getGrade() > 11) {
            throw new IllegalArgumentException("Класс должен быть от 1 до 11");
        }
    }

    private Student readAndCheck(long recordNumber, long expectedId) throws IOException {
        RecordManager recordManager = database.getRecordManager();

        Student student = recordManager.read(recordNumber);

        if (!student.isActive()) {
            throw new IOException("Первичный индекс указывает на удалённую запись");
        }

        if (student.getId() != expectedId) {
            throw new IOException("Первичный индекс повреждён");
        }
        return student;
    }

    private void rollbackAdd(long recordNumber, long id, int grade, boolean primaryAdded,
                             boolean gradeAdded, boolean reused) {
        try {
            if (gradeAdded) {
                gradeIndex.remove(grade, recordNumber);
            }
        } catch (Exception ignored) {
        }

        try {
            if (primaryAdded) {
                primaryIndex.remove(id);
            }
        } catch (Exception ignored) {
        }

        try {
            database.getRecordManager().setActive(recordNumber, false);
        } catch (Exception ignored) {
        }

        if (reused) {
            try {
                freeSpaceManager.push(recordNumber);
            } catch (Exception ignored) {
            }
        }
    }

    public long add(Student student) throws IOException {
        checkDatabase();
        checkStudent(student);

        if (primaryIndex.contains(student.getId())) {
            throw new IllegalArgumentException("Запись с ID %d уже существует".formatted(student.getId()));
        }

        DatabaseHeader header = database.getHeader();
        RecordManager recordManager = database.getRecordManager();
        long recordNumber = freeSpaceManager.pop();

        boolean reused = recordNumber != -1;

        if (!reused) {
            recordNumber = header.getTotalRecords();
        }

        student.setActive(true);

        boolean primaryAdded = false;
        boolean gradeAdded = false;

        try {
            recordManager.write(recordNumber, student);

            primaryIndex.put(student.getId(), recordNumber);

            primaryAdded = true;

            gradeIndex.add(student.getGrade(), recordNumber);

            gradeAdded = true;
        } catch (IOException | RuntimeException e) {
            rollbackAdd(recordNumber, student.getId(), student.getGrade(), primaryAdded, gradeAdded, reused);
            throw e;
        }

        if (!reused) {
            header.incrementTotalRecords();
        }

        header.incrementActiveRecords();

        database.save();

        return recordNumber;
    }

    public Student findById(long id) throws IOException {
        checkDatabase();

        long recordNumber = primaryIndex.get(id);

        if (recordNumber == -1) {
            return null;
        }

        return readAndCheck(recordNumber, id);
    }

    public List<Student> findByGrade(int grade) throws IOException {
        checkDatabase();

        if (grade < 1 || grade > 11) {
            throw new IllegalArgumentException("Класс должен быть от 1 до 11");
        }

        List<Student> res = new ArrayList<>();

        RecordManager recordManager = database.getRecordManager();

        gradeIndex.forEach(grade,
                recordNumber -> {
                    Student student = recordManager.read(recordNumber);

                    if (!student.isActive()) {
                        throw new IOException("Индекс класса указывает на удалённую запись");
                    }

                    if (student.getGrade() != grade) {
                        throw new IOException("Индекс классов повреждён");
                    }
                    res.add(student);
                }
        );
        return res;
    }

    public boolean deleteById(long id) throws IOException {
        checkDatabase();

        long recordNumber = primaryIndex.get(id);

        if (recordNumber == -1) {
            return false;
        }

        Student student = readAndCheck(recordNumber, id);

        if (!gradeIndex.contains(student.getGrade(), recordNumber)) {
            throw new IOException("Индекс классов повреждён");
        }

        // Сначала резервируем свободное место
        freeSpaceManager.push(recordNumber);

        boolean gradeRemoved = false;
        boolean primaryRemoved = false;
        boolean headerChanged = false;

        try {
            if (!gradeIndex.remove(student.getGrade(), recordNumber)) {throw new IOException("Не удалось удалить запись из индекса классов");}

            gradeRemoved = true;

            if (!primaryIndex.remove(id)) {throw new IOException("Не удалось удалить запись из первичного индекса");}

            primaryRemoved = true;

            database.getRecordManager().setActive(recordNumber, false);

            database.getHeader().decrementActiveRecords();
            headerChanged = true;

            database.save();

            return true;

        } catch (IOException | RuntimeException e) {

            try {
                database.getRecordManager().setActive(recordNumber, true);
            } catch (Exception ex) {
                e.addSuppressed(ex);
            }

            if (primaryRemoved) {
                try {
                    primaryIndex.put(id, recordNumber);
                } catch (Exception ex) {
                    e.addSuppressed(ex);
                }
            }

            if (gradeRemoved) {
                try {
                    gradeIndex.add(student.getGrade(), recordNumber);
                } catch (Exception ex) {
                    e.addSuppressed(ex);
                }
            }

            if (headerChanged) {
                database.getHeader().incrementActiveRecords();
            }

            try {
                freeSpaceManager.pop();
            } catch (Exception ex) {
                e.addSuppressed(ex);
            }

            throw e;
        }
    }

    public int deleteByGrade(int grade) throws IOException {
        checkDatabase();

        if (grade < 1 || grade > 11) {
            throw new IllegalArgumentException("Класс должен быть от 1 до 11");
        }

        int deleted = 0;

        RecordManager recordManager = database.getRecordManager();

        while (true) {
            long recordNumber = gradeIndex.getFirstRecordNumber(grade);

            if (recordNumber == -1) {
                break;
            }
            Student student = recordManager.read(recordNumber);

            if (!student.isActive() || student.getGrade() != grade) {
                throw new IOException("Индекс классов повреждён");
            }

            if (!deleteById(student.getId())) {
                throw new IOException("Не удалось удалить запись");
            }
            deleted++;
        }
        return deleted;
    }

    public void update(long currentId, Student updatedStudent) throws IOException {

        checkDatabase();
        checkStudent(updatedStudent);

        long recordNumber = primaryIndex.get(currentId);

        if (recordNumber == -1) {
            throw new IllegalArgumentException("Запись с ID %d не найдена".formatted(currentId));
        }

        Student oldStudent = readAndCheck(recordNumber, currentId);

        boolean idChanged = currentId != updatedStudent.getId();

        boolean gradeChanged = oldStudent.getGrade() != updatedStudent.getGrade();

        if (idChanged && primaryIndex.contains(updatedStudent.getId())) {
            throw new IllegalArgumentException("Запись с ID %d уже существует".formatted(updatedStudent.getId()));
        }

        if (!gradeIndex.contains(oldStudent.getGrade(), recordNumber)) {
            throw new IOException("Индекс классов повреждён");
        }

        updatedStudent.setActive(true);

        boolean newPrimaryAdded = false;
        boolean newGradeAdded = false;
        boolean recordUpdated = false;

        boolean oldPrimaryRemoved = false;
        boolean oldGradeRemoved = false;

        try {

            if (idChanged) {
                primaryIndex.put(updatedStudent.getId(), recordNumber);
                newPrimaryAdded = true;
            }

            if (gradeChanged) {
                gradeIndex.add(updatedStudent.getGrade(), recordNumber);
                newGradeAdded = true;
            }

            database.getRecordManager().write(recordNumber, updatedStudent);
            recordUpdated = true;

            if (idChanged) {

                if (!primaryIndex.remove(currentId)) {
                    throw new IOException("Не удалось удалить старый ID из индекса");
                }
                oldPrimaryRemoved = true;
            }

            if (gradeChanged) {
                if (!gradeIndex.remove(oldStudent.getGrade(), recordNumber)) {
                    throw new IOException("Не удалось удалить старый класс из индекса");
                }
                oldGradeRemoved = true;
            }

        } catch (IOException | RuntimeException e) {
            if (recordUpdated) {
                try {
                    database.getRecordManager().write(recordNumber, oldStudent);
                } catch (Exception ignored) {
                }
            }
            if (oldPrimaryRemoved) {
                try {
                    primaryIndex.put(currentId, recordNumber);
                } catch (Exception ignored) {
                }
            }
            if (oldGradeRemoved) {
                try {
                    gradeIndex.add(oldStudent.getGrade(), recordNumber);
                } catch (Exception ignored) {
                }
            }
            if (newGradeAdded) {
                try {
                    gradeIndex.remove(updatedStudent.getGrade(), recordNumber);
                } catch (Exception ignored) {
                }
            }
            if (newPrimaryAdded) {
                try {
                    primaryIndex.remove(updatedStudent.getId());
                } catch (Exception ignored) {
                }
            }
            throw e;
        }
        database.getHeader().updateModifiedTime();
        database.save();
    }

    public List<Student> loadAllForDisplay() throws IOException {
        checkDatabase();

        List<Student> students = new ArrayList<>();

        DatabaseHeader header = database.getHeader();

        RecordManager recordManager = database.getRecordManager();

        for (long recordNumber = 0; recordNumber < header.getTotalRecords(); recordNumber++) {
            Student student = recordManager.read(recordNumber);
            if (student.isActive()) {
                students.add(student);
            }
        }
        return students;
    }
}
