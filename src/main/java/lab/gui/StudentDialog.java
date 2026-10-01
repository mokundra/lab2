package lab.gui;

import lab.model.Student;

import javax.swing.*;
import java.awt.*;

public class StudentDialog extends JDialog {

    private final JTextField idField = new JTextField(20);
    private final JTextField fullNameField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);

    private final JSpinner gradeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 11, 1));

    private final JTextField subjectField = new JTextField(20);
    private final JTextField courseField = new JTextField(20);
    private final JTextField tariffField = new JTextField(20);

    private Student resultStudent;

    private long registrationDate;

    private void addField(JPanel panel, GridBagConstraints constraints,
                          int row, String labelText, Component component) {

        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;

        panel.add(new JLabel(labelText), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;

        panel.add(component, constraints);
    }


    private JPanel createFormPanel() {

        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.insets = new Insets(5, 5, 5, 5);

        constraints.fill = GridBagConstraints.HORIZONTAL;

        addField(panel, constraints, 0, "ID:", idField);

        addField(panel, constraints, 1, "ФИО:", fullNameField);

        addField(panel, constraints, 2, "Email:", emailField);

        addField(panel, constraints, 3, "Класс:", gradeSpinner);

        addField(panel, constraints, 4, "Предмет:", subjectField);

        addField(panel, constraints, 5, "Курс:", courseField);

        addField(panel, constraints, 6, "Тариф:", tariffField);

        return panel;
    }


    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton saveButton = new JButton("Сохранить");

        JButton cancelButton = new JButton("Отмена");

        saveButton.addActionListener(e -> saveStudent());

        cancelButton.addActionListener(e -> dispose());

        panel.add(saveButton);
        panel.add(cancelButton);

        return panel;
    }


    private String getRequiredText(JTextField field, String fieldName) {
        String value = field.getText().trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException("%s не может быть пустым".formatted(fieldName));
        }

        return value;
    }


    private long readId() {
        String text = idField.getText().trim();

        if (text.isEmpty()) {
            throw new IllegalArgumentException("ID не может быть пустым");
        }

        long id;

        try {
            id = Long.parseLong(text);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID должен быть целым числом");
        }

        if (id <= 0) {
            throw new IllegalArgumentException("ID должен быть положительным");
        }

        return id;
    }


    private void saveStudent() {
        try {
            long id = readId();

            String fullName = getRequiredText(fullNameField, "ФИО");

            String email = getRequiredText(emailField, "Email");

            int grade = (Integer) gradeSpinner.getValue();

            String subject = getRequiredText(subjectField, "Предмет");

            String course = getRequiredText(courseField, "Курс");

            String tariff = getRequiredText(tariffField, "Тариф");

            resultStudent = new Student(id, fullName, email, grade, subject, course, tariff, registrationDate);
            dispose();

        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void fillFields(Student student) {
        idField.setText(String.valueOf(student.getId()));

        fullNameField.setText(student.getFullName());

        emailField.setText(student.getEmail());

        gradeSpinner.setValue(student.getGrade());

        subjectField.setText(student.getSubject());

        courseField.setText(student.getCourse());

        tariffField.setText(student.getTariff());
    }


    private void configureWindow() {

        setLayout(new BorderLayout(10, 10));

        add(createFormPanel(), BorderLayout.CENTER);

        add(createButtonPanel(), BorderLayout.SOUTH);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        pack();

        setResizable(false);

        setLocationRelativeTo(getOwner());
    }


    public StudentDialog(Window owner, Student student) {
        super(owner, student == null ? "Добавление студента" : "Редактирование студента", ModalityType.APPLICATION_MODAL);

        if (student == null) {
            registrationDate = System.currentTimeMillis();

        } else {
            registrationDate = student.getRegistrationDate();

            fillFields(student);
        }
        configureWindow();
    }

    public Student getResultStudent() {
        return resultStudent;
    }
}