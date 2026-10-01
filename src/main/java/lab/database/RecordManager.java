package lab.database;

import lab.model.Student;

import java.io.IOException;
import java.io.RandomAccessFile;

public class RecordManager {
    private static final int FULL_NAME_LENGTH = 50;
    private static final int EMAIL_LENGTH = 60;
    private static final int SUBJECT_LENGTH = 30;
    private static final int COURSE_LENGTH = 40;
    private static final int TARIFF_LENGTH = 20;
    private static final int RESERVED_BYTES = 3;
    private final RandomAccessFile file;

    public RecordManager(RandomAccessFile file) {
        this.file = file;
    }

    public long getRecordOffset(long recordNumber) {
        if (recordNumber < 0) {
            throw new IllegalArgumentException("Номер записи не может быть отрицательным");
        }

        return DatabaseHeader.HEADER_SIZE + recordNumber * DatabaseHeader.RECORD_SIZE;
    }

    public void setActive(long recordNumber, boolean active) throws IOException {

        long offset = getRecordOffset(recordNumber);

        if (offset + DatabaseHeader.RECORD_SIZE > file.length()) {
            throw new IOException("Запись с таким номером не существует");
        }

        file.seek(offset);

        file.writeBoolean(active);
    }

    private void writeFixedString(String value, int maxLength) throws IOException {
        for (int i = 0; i < maxLength; i++) {
            if (i < value.length()) {
                file.writeChar(value.charAt(i));
            } else {
                file.writeChar('\0');
            }
        }
    }

    private String readFixedString(int maxLength) throws IOException {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < maxLength; i++) {

            char symbol = file.readChar();

            if (symbol != '\0') {
                result.append(symbol);
            }
        }
        return result.toString();
    }

    private void checkString(String value, int maxLength, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " не может быть null");
        }

        if (value.length() > maxLength) {
            throw new IllegalArgumentException("%s: максимальная длина : %d символов".formatted(fieldName, maxLength));
        }
    }

    private void validateStudent(Student student) {
        if (student == null) {
            throw new IllegalArgumentException("Student не может быть null");
        }

        checkString(
                student.getFullName(),
                FULL_NAME_LENGTH,
                "ФИО"
        );

        checkString(
                student.getEmail(),
                EMAIL_LENGTH,
                "Email"
        );

        checkString(
                student.getSubject(),
                SUBJECT_LENGTH,
                "Предмет"
        );

        checkString(
                student.getCourse(),
                COURSE_LENGTH,
                "Курс"
        );

        checkString(
                student.getTariff(),
                TARIFF_LENGTH,
                "Тариф"
        );
    }


    public void write(long recordNumber, Student student) throws IOException {

        validateStudent(student);

        long offset = getRecordOffset(recordNumber);

        file.seek(offset);

        file.writeBoolean(student.isActive());
        file.writeLong(student.getId());

        writeFixedString(student.getFullName(), FULL_NAME_LENGTH);
        writeFixedString(student.getEmail(), EMAIL_LENGTH);

        file.writeInt(student.getGrade());

        writeFixedString(student.getSubject(), SUBJECT_LENGTH);
        writeFixedString(student.getCourse(), COURSE_LENGTH);
        writeFixedString(student.getTariff(), TARIFF_LENGTH);

        file.writeLong(student.getRegistrationDate());

        for (int i = 0; i < RESERVED_BYTES; i++) {
            file.writeByte(0);
        }

    }

    public Student read(long recordNumber) throws IOException {
        long offset = getRecordOffset(recordNumber);

        if (offset + DatabaseHeader.RECORD_SIZE > file.length()) {
            throw new IOException("Запись с таким номером не существует");
        }

        file.seek(offset);

        boolean active = file.readBoolean();
        long id = file.readLong();

        String fullName = readFixedString(FULL_NAME_LENGTH);
        String email = readFixedString(EMAIL_LENGTH);

        int grade = file.readInt();

        String subject = readFixedString(SUBJECT_LENGTH);
        String course = readFixedString(COURSE_LENGTH);
        String tariff = readFixedString(TARIFF_LENGTH);

        long registrationDate = file.readLong();

        file.skipBytes(RESERVED_BYTES);

        return new Student(active, id, fullName, email, grade, subject, course, tariff, registrationDate);
    }

}
