package lab.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupService {
    private static final String DATABASE_FILE = "students.db";
    private static final String PRIMARY_INDEX_FILE = "primary.idx";
    private static final String GRADE_INDEX_FILE = "grade.idx";
    private static final String FREE_SPACE_FILE = "free.idx";

    private void checkFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("Файл %s не существует".formatted(path.getFileName()));
        }
    }

    private void addFileToZip(ZipOutputStream zip, Path file, String entryName) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);

        zip.putNextEntry(entry);

        Files.copy(file, zip);

        zip.closeEntry();
    }

    public void createBackup(Path databaseDirectory, Path backupFile) throws IOException {

        Path databaseFile = databaseDirectory.resolve(DATABASE_FILE);

        Path primaryIndexFile = databaseDirectory.resolve(PRIMARY_INDEX_FILE);

        Path gradeIndexFile = databaseDirectory.resolve(GRADE_INDEX_FILE);

        Path freeSpaceFile = databaseDirectory.resolve(FREE_SPACE_FILE);

        checkFile(databaseFile);
        checkFile(primaryIndexFile);
        checkFile(gradeIndexFile);
        checkFile(freeSpaceFile);

        Path parent = backupFile.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (OutputStream output = Files.newOutputStream(backupFile);

             ZipOutputStream zip = new ZipOutputStream(output)) {

            addFileToZip(zip, databaseFile, DATABASE_FILE);

            addFileToZip(zip, primaryIndexFile, PRIMARY_INDEX_FILE);

            addFileToZip(zip, gradeIndexFile, GRADE_INDEX_FILE);

            addFileToZip(zip, freeSpaceFile, FREE_SPACE_FILE);
        }
    }

    public void restoreBackup(Path backupFile, Path databaseDirectory) throws IOException {
        checkFile(backupFile);

        Files.createDirectories(databaseDirectory);

        Set<String> restoredFiles = new HashSet<>();

        try (InputStream input = Files.newInputStream(backupFile);
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();

                if (!name.equals(DATABASE_FILE) && !name.equals(PRIMARY_INDEX_FILE)
                        && !name.equals(GRADE_INDEX_FILE) && !name.equals(FREE_SPACE_FILE)) {
                    throw new IOException("Backup содержит неизвестный файл: " + name);
                }

                Path target = databaseDirectory.resolve(name);

                Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);

                restoredFiles.add(name);

                zip.closeEntry();
            }
        }

        if (!restoredFiles.contains(DATABASE_FILE) || !restoredFiles.contains(PRIMARY_INDEX_FILE)
                || !restoredFiles.contains(GRADE_INDEX_FILE) || !restoredFiles.contains(FREE_SPACE_FILE)) {
            throw new IOException("Backup повреждён или содержит не все файлы базы данных");
        }
    }

}
