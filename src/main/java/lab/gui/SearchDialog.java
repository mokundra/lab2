package lab.gui;

import javax.swing.*;
import java.awt.*;

public class SearchDialog extends JDialog {

    public static final String SEARCH_BY_ID = "ID";
    public static final String SEARCH_BY_GRADE = "Класс";

    private final JComboBox<String> searchTypeBox =
            new JComboBox<>(new String[]{SEARCH_BY_ID, SEARCH_BY_GRADE});

    private final JTextField valueField = new JTextField(15);

    private boolean confirmed = false;

    private String searchType;
    private long searchValue;


    private JPanel createFormPanel() {

        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(new JLabel("Искать по:"), constraints);

        constraints.gridx = 1;

        panel.add(searchTypeBox, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;

        panel.add(new JLabel("Значение:"), constraints);

        constraints.gridx = 1;

        panel.add(valueField, constraints);

        return panel;
    }


    private JPanel createButtonPanel() {

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton searchButton = new JButton("Найти");

        JButton cancelButton = new JButton("Отмена");

        searchButton.addActionListener(e -> confirmSearch());

        cancelButton.addActionListener(e -> dispose());

        panel.add(searchButton);
        panel.add(cancelButton);

        return panel;
    }


    private void confirmSearch() {

        try {
            String text = valueField.getText().trim();

            if (text.isEmpty()) {
                throw new IllegalArgumentException("Введите значение для поиска");
            }

            searchType = (String) searchTypeBox.getSelectedItem();

            if (SEARCH_BY_ID.equals(searchType)) {
                long id;

                try {
                    id = Long.parseLong(text);

                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("ID должен быть целым числом");
                }

                if (id <= 0) {
                    throw new IllegalArgumentException("ID должен быть положительным");
                }

                searchValue = id;

            } else {

                int grade;

                try {
                    grade = Integer.parseInt(text);

                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Класс должен быть целым числом");
                }

                if (grade < 1 || grade > 11) {
                    throw new IllegalArgumentException("Класс должен быть от 1 до 11");
                }

                searchValue = grade;
            }

            confirmed = true;

            dispose();

        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
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

    public SearchDialog(Window owner) {
        super(owner, "Поиск", ModalityType.APPLICATION_MODAL);
        configureWindow();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getSearchType() {
        return searchType;
    }

    public long getSearchValue() {
        return searchValue;
    }
}