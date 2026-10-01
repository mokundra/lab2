package lab.gui;

import lab.database.Database;
import lab.index.FreeSpaceManager;
import lab.index.GradeIndex;
import lab.index.PrimaryIndex;
import lab.model.Student;
import lab.service.BackupService;
import lab.service.ImportService;
import lab.service.StatisticsService;
import lab.service.StudentService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MainWindow extends JFrame {
    private static final String DATABASE_FILE = "students.db";
    private static final String PRIMARY_INDEX_FILE = "primary.idx";
    private static final String GRADE_INDEX_FILE = "grade.idx";
    private static final String FREE_SPACE_FILE = "free.idx";

    private final Database database = new Database();
    private final PrimaryIndex primaryIndex = new PrimaryIndex();
    private final GradeIndex gradeIndex = new GradeIndex();
    private final FreeSpaceManager freeSpaceManager = new FreeSpaceManager();

    private final StudentService studentService = new StudentService(database, primaryIndex, gradeIndex, freeSpaceManager);

    private final BackupService backupService = new BackupService();
    private final ImportService importService = new ImportService();
    private final StatisticsService statisticsService = new StatisticsService();

    private final DatabaseTableModel tableModel = new DatabaseTableModel();
    private final JTable table = new JTable(tableModel);

    private Path databaseDirectory;

    private void showError(Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
    }

    private void checkDatabaseOpen() {
        if (!database.isOpen()) {
            throw new IllegalStateException("База данных не открыта");
        }
    }

    private void closeAll() throws IOException {
        IOException error = null;

        try {
            freeSpaceManager.close();
        } catch (IOException e) {
            error = e;
        }

        try {
            gradeIndex.close();
        } catch (IOException e) {
            if (error == null) {
                error = e;
            }
        }

        try {
            primaryIndex.close();
        } catch (IOException e) {
            if (error == null) {
                error = e;
            }
        }

        try {
            database.close();
        } catch (IOException e) {
            if (error == null) {
                error = e;
            }
        }

        if (error != null) {
            throw error;
        }
    }

    private void safeCloseAll() {
        try {
            closeAll();
        } catch (IOException ignored) {
        }
    }

    private void deleteDatabaseFiles(Path directory) throws IOException {
        Files.deleteIfExists(directory.resolve(DATABASE_FILE));

        Files.deleteIfExists(directory.resolve(PRIMARY_INDEX_FILE));

        Files.deleteIfExists(directory.resolve(GRADE_INDEX_FILE));

        Files.deleteIfExists(directory.resolve(FREE_SPACE_FILE));
    }

    private void openAll(Path directory) throws IOException {
        try {
            database.open(directory.resolve(DATABASE_FILE));

            primaryIndex.open(directory.resolve(PRIMARY_INDEX_FILE));

            gradeIndex.open(directory.resolve(GRADE_INDEX_FILE));

            freeSpaceManager.open(directory.resolve(FREE_SPACE_FILE));

            databaseDirectory = directory;
        } catch (IOException | RuntimeException e) {
            safeCloseAll();

            databaseDirectory = null;

            throw e;
        }
    }

    private void refreshTable() throws IOException {
        checkDatabaseOpen();

        tableModel.setStudents(studentService.loadAllForDisplay());
    }

    private void createDatabase() {
        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle("Выберите папку для новой базы данных");

        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = chooser.showSaveDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path directory = chooser.getSelectedFile().toPath();

        try {
            if (database.isOpen()) {
                closeAll();
            }

            Files.createDirectories(directory);

            Path databasePath = directory.resolve(DATABASE_FILE);

            Path primaryPath = directory.resolve(PRIMARY_INDEX_FILE);

            Path gradePath = directory.resolve(GRADE_INDEX_FILE);

            Path freePath = directory.resolve(FREE_SPACE_FILE);

            if (Files.exists(databasePath) || Files.exists(primaryPath) || Files.exists(gradePath) || Files.exists(freePath)) {

                throw new IOException("В выбранной папке уже существуют файлы базы данных");
            }

            try {
                database.create(databasePath);
                primaryIndex.create(primaryPath);
                gradeIndex.create(gradePath);
                freeSpaceManager.create(freePath);

            } catch (IOException | RuntimeException e) {
                safeCloseAll();

                deleteDatabaseFiles(directory);

                throw e;
            }

            databaseDirectory = directory;

            refreshTable();

            JOptionPane.showMessageDialog(this, "База данных успешно создана");

        } catch (Exception e) {
            showError(e);
        }
    }

    private void openDatabase() {
        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle("Выберите папку базы данных");

        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = chooser.showOpenDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path directory = chooser.getSelectedFile().toPath();

        try {
            if (database.isOpen()) {
                closeAll();
            }

            openAll(directory);

            refreshTable();

            JOptionPane.showMessageDialog(this, "База данных успешно открыта");
        } catch (Exception e) {
            showError(e);
        }
    }

    private void saveDatabase() {
        try {
            checkDatabaseOpen();

            database.save();

            JOptionPane.showMessageDialog(this, "База данных сохранена");
        } catch (Exception e) {
            showError(e);
        }
    }

    private void clearDatabase() {
        try {
            checkDatabaseOpen();

            int answer = JOptionPane.showConfirmDialog(this, "Удалить все записи из базы данных?", "Очистка базы", JOptionPane.YES_NO_OPTION);

            if (answer != JOptionPane.YES_OPTION) {
                return;
            }

            database.clear();
            primaryIndex.clear();
            gradeIndex.clear();
            freeSpaceManager.clear();

            refreshTable();
        } catch (Exception e) {
            showError(e);
        }
    }

    private void deleteDatabase() {
        try {
            checkDatabaseOpen();

            int answer = JOptionPane.showConfirmDialog(this, "Полностью удалить базу данных?", "Удаление базы", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
            Path directory = databaseDirectory;

            closeAll();

            deleteDatabaseFiles(directory);

            databaseDirectory = null;

            tableModel.setStudents(List.of());

            JOptionPane.showMessageDialog(this, "База данных удалена");
        } catch (Exception e) {
            showError(e);
        }
    }

    private void createBackup() {
        try {
            checkDatabaseOpen();

            database.save();

            JFileChooser chooser = new JFileChooser();

            chooser.setDialogTitle("Сохранить backup");

            chooser.setSelectedFile(new java.io.File("backup.zip"));

            int result = chooser.showSaveDialog(this);

            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }

            Path backupFile = chooser.getSelectedFile().toPath();

            backupService.createBackup(databaseDirectory, backupFile);

            JOptionPane.showMessageDialog(this, "Backup успешно создан");

        } catch (Exception e) {
            showError(e);
        }
    }

    private void restoreBackup() {

        try {
            checkDatabaseOpen();

            JFileChooser chooser = new JFileChooser();

            chooser.setDialogTitle("Выберите backup-файл");

            int result = chooser.showOpenDialog(this);

            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }

            Path backupFile = chooser.getSelectedFile().toPath();

            Path directory = databaseDirectory;

            closeAll();

            try {
                backupService.restoreBackup(backupFile, directory);

                openAll(directory);

                refreshTable();
            } catch (Exception e) {
                safeCloseAll();
                throw e;
            }

            JOptionPane.showMessageDialog(this, "База данных восстановлена");
        } catch (Exception e) {
            showError(e);
        }
    }

    private void exportToExcel() {

        try {
            checkDatabaseOpen();

            JFileChooser chooser = new JFileChooser();

            chooser.setDialogTitle("Экспорт в Excel");

            chooser.setSelectedFile(new java.io.File("students.xlsx"));

            int result = chooser.showSaveDialog(this);

            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }

            Path outputFile = chooser.getSelectedFile().toPath();

            importService.exportToXlsx(database, outputFile);

            JOptionPane.showMessageDialog(this, "Excel-файл успешно создан");
        } catch (Exception e) {
            showError(e);
        }
    }

    private void addStudent(){

        try{
            checkDatabaseOpen();

            StudentDialog dialog = new StudentDialog(this,null);

            dialog.setVisible(true);

            Student student = dialog.getResultStudent();

            if (student==null){
                return;
            }

            studentService.add(student);

            refreshTable();
        }catch (Exception e){
            showError(e);
        }
    }

    private Student getSelectedStudent(){
        int selectedRow = table.getSelectedRow();

        if (selectedRow == -1){
            throw new IllegalStateException( "Выберите запись в таблице");
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);

        return tableModel.getStudentAt(modelRow);
    }

    private void editStudent(){
        try{
            checkDatabaseOpen();

            Student oldStudent = getSelectedStudent();

            StudentDialog dialog = new StudentDialog(this,oldStudent);

            dialog.setVisible(true);

            Student updatedStudent = dialog.getResultStudent();

            if (updatedStudent==null){
                return;
            }

            studentService.update(oldStudent.getId(),updatedStudent);

            refreshTable();
        }catch (Exception e){
            showError(e);
        }
    }

    private void deleteSelectedStudent(){

        try{
            checkDatabaseOpen();

            Student student = getSelectedStudent();

            int answer=JOptionPane.showConfirmDialog(this,
                    "Удалить студента с ID %d?".formatted(student.getId()),
                    "Удаление", JOptionPane.YES_NO_OPTION
            );
            if (answer != JOptionPane.YES_OPTION){
                return;
            }

            studentService.deleteById(student.getId());
            refreshTable();
        }catch (Exception e){
            showError(e);
        }
    }
    private void deleteByGrade(){
        try{
            checkDatabaseOpen();

            JSpinner gradeSpinner = new JSpinner(new SpinnerNumberModel(1,1,11,1));

            int answer = JOptionPane.showConfirmDialog(this,gradeSpinner,"Выберите класс для удаления", JOptionPane.OK_CANCEL_OPTION);

            if (answer != JOptionPane.OK_OPTION){
                return;
            }

            int grade = (Integer) gradeSpinner.getValue();
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Удалить всех учеников %d класса?".formatted(grade),
                    "Подтверждение",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirm != JOptionPane.YES_OPTION){
                return;
            }

            int deleted = studentService.deleteByGrade(grade);

            refreshTable();

            JOptionPane.showMessageDialog(this, "Удалено записей: %d".formatted(deleted));

        }catch (Exception e){
            showError(e);
        }
    }

    private void searchStudents(){
        try{
            checkDatabaseOpen();

            SearchDialog dialog = new SearchDialog(this);

            dialog.setVisible(true);

            if (!dialog.isConfirmed()){
                return;
            }

            if (SearchDialog.SEARCH_BY_ID.equals(dialog.getSearchType())){
                Student student = studentService.findById(dialog.getSearchValue());

                if (student == null){
                    tableModel.setStudents(List.of());

                    JOptionPane.showMessageDialog(this,"Запись не найдена");

                    return;
                }
                tableModel.setStudents(List.of(student));

            }else{
                List<Student> students =studentService.findByGrade((int) dialog.getSearchValue());

                tableModel.setStudents(students);

                if (students.isEmpty()){
                    JOptionPane.showMessageDialog(this,"Записи не найдены");

                }
            }
        }catch (Exception e){
            showError(e);
        }
    }

    private void showAllStudents() {
        try {
            refreshTable();
        } catch (Exception e) {
            showError(e);
        }
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton addButton = new JButton("Добавить");

        JButton editButton = new JButton("Редактировать");

        JButton deleteButton = new JButton("Удалить");

        JButton deleteGradeButton = new JButton("Удалить по классу");

        JButton searchButton = new JButton("Поиск");

        JButton showAllButton = new JButton("Показать все");


        addButton.addActionListener(e -> addStudent());

        editButton.addActionListener(e -> editStudent());

        deleteButton.addActionListener(e -> deleteSelectedStudent());

        deleteGradeButton.addActionListener(e -> deleteByGrade());

        searchButton.addActionListener(e -> searchStudents());

        showAllButton.addActionListener(e -> showAllStudents());

        panel.add(addButton);
        panel.add(editButton);
        panel.add(deleteButton);
        panel.add(deleteGradeButton);
        panel.add(searchButton);
        panel.add(showAllButton);

        return panel;
    }

    private JMenuBar createMenuBar(){
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Файл");

        JMenuItem createItem = new JMenuItem("Создать БД");

        JMenuItem openItem = new JMenuItem("Открыть БД");

        JMenuItem saveItem = new JMenuItem("Сохранить");

        JMenuItem clearItem = new JMenuItem("Очистить БД");

        JMenuItem deleteItem = new JMenuItem("Удалить БД");

        JMenuItem exitItem = new JMenuItem("Выход");


        createItem.addActionListener(e -> createDatabase());

        openItem.addActionListener(e -> openDatabase());

        saveItem.addActionListener(e -> saveDatabase());

        clearItem.addActionListener(e -> clearDatabase());

        deleteItem.addActionListener(e -> deleteDatabase());

        exitItem.addActionListener(e -> closeWindow());


        fileMenu.add(createItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);

        fileMenu.addSeparator();

        fileMenu.add(clearItem);
        fileMenu.add(deleteItem);

        fileMenu.addSeparator();

        fileMenu.add(exitItem);


        JMenu backupMenu = new JMenu("Backup");

        JMenuItem createBackupItem = new JMenuItem("Создать backup");

        JMenuItem restoreBackupItem = new JMenuItem("Восстановить backup");


        createBackupItem.addActionListener(e -> createBackup());

        restoreBackupItem.addActionListener(e -> restoreBackup());


        backupMenu.add(createBackupItem);
        backupMenu.add(restoreBackupItem);

        JMenu exportMenu = new JMenu("Экспорт");

        JMenuItem excelItem = new JMenuItem("Экспорт в XLSX");

        excelItem.addActionListener(e -> exportToExcel());

        exportMenu.add(excelItem);

        menuBar.add(fileMenu);
        menuBar.add(backupMenu);
        menuBar.add(exportMenu);

        return menuBar;
    }

    private void closeWindow() {
        safeCloseAll();
        dispose();
    }

    private void configureWindow(){
        setTitle("Файловая база данных студентов");

        setLayout(new BorderLayout(10, 10));

        setJMenuBar(createMenuBar());

        table.setAutoCreateRowSorter(true);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(table);

        add(createButtonPanel(),BorderLayout.NORTH);

        add(scrollPane,BorderLayout.CENTER);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        closeWindow();
                    }
                }
        );

        setSize(1200,600);
        setLocationRelativeTo(null);
    }

    public MainWindow() {
        configureWindow();
    }

}
