package lab.service;

import lab.database.Database;
import lab.database.DatabaseHeader;
import lab.database.RecordManager;
import lab.model.Student;


import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ImportService {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private void checkDatabase(Database database) {
        if (database == null) {
            throw new IllegalArgumentException("Database не может быть null");
        }

        if (!database.isOpen()) {
            throw new IllegalStateException("База данных не открыта");
        }
    }

    private String formatDate(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DATE_FORMAT);
        //берем число от 1970 1 января, переводим в время как в системе и в нашемму формату
    }

    private void createHeader(Row row) {
        row.createCell(0).setCellValue("ID");
        row.createCell(1).setCellValue("ФИО");
        row.createCell(2).setCellValue("Email");
        row.createCell(3).setCellValue("Класс");
        row.createCell(4).setCellValue("Предмет");
        row.createCell(5).setCellValue("Курс");
        row.createCell(6).setCellValue("Тариф");
        row.createCell(7).setCellValue("Дата регистрации");
    }

    private void writeStudent(Row row, Student student) {
        row.createCell(0).setCellValue(student.getId());
        row.createCell(1).setCellValue(student.getFullName());
        row.createCell(2).setCellValue(student.getEmail());
        row.createCell(3).setCellValue(student.getGrade());
        row.createCell(4).setCellValue(student.getSubject());
        row.createCell(5).setCellValue(student.getCourse());
        row.createCell(6).setCellValue(student.getTariff());
        row.createCell(7).setCellValue(formatDate(student.getRegistrationDate()));
    }

    public void exportToXlsx(Database database, Path outputFile) throws IOException {
        checkDatabase(database);

        Path parent = outputFile.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        DatabaseHeader header = database.getHeader();

        RecordManager recordManager = database.getRecordManager();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Students");

            int rowNumber = 0;

            Row headerRow = sheet.createRow(rowNumber++);

            createHeader(headerRow);

            for (long recordNumber = 0; recordNumber < header.getTotalRecords(); recordNumber++) {
                Student student = recordManager.read(recordNumber);

                if (!student.isActive()) {
                    continue;
                }

                Row row = sheet.createRow(rowNumber++);

                writeStudent(row, student);
            }

            for (int column = 0; column < 8; column++) {
                sheet.autoSizeColumn(column);
            }

            try (OutputStream output = Files.newOutputStream(outputFile)) {
                workbook.write(output);
            }
        }
    }
}
