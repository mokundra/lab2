package lab.gui;

import lab.model.Student;

import javax.swing.table.AbstractTableModel;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DatabaseTableModel extends AbstractTableModel {
    private static final String[] COLUMN_NAMES = {
            "ID",
            "ФИО",
            "Email",
            "Класс",
            "Предмет",
            "Курс",
            "Тариф",
            "Дата регистрации"
    };

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private List<Student> students = new ArrayList<>();

    private String formatDate(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DATE_FORMAT);
    }

    public void setStudents(List<Student> students) {
        if (students == null) {
            this.students = new ArrayList<>();
        } else {
            this.students = students;
        }

        fireTableDataChanged();
    }

    public Student getStudentAt(int rowIndex) {
        if (rowIndex < 0 || rowIndex > students.size()) {
            throw new IllegalArgumentException("Некорректный номер строки");
        }

        return students.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return students.size();
    }


    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }


    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        Student student = students.get(rowIndex);

        return switch (columnIndex) {
            case 0 -> student.getId();
            case 1 -> student.getFullName();
            case 2 -> student.getEmail();
            case 3 -> student.getGrade();
            case 4 -> student.getSubject();
            case 5 -> student.getCourse();
            case 6 -> student.getTariff();
            case 7 -> formatDate(student.getRegistrationDate());
            default -> null;
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
}
