package lab.database;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

public class Database {
    private Path filePath;
    private RandomAccessFile file;

    private DatabaseHeader header;
    private RecordManager recordManager;

    private void checkOpen() {
        if (file == null) {
            throw new IllegalStateException("База данных не открыта");
        }
    }

    public void save() throws IOException {

        checkOpen();

        header.write(file);

        file.getFD().sync();//Возьми системное представление этого файла и принудительно синхронизируй его с диском
    }

    public void create(Path path) throws IOException {
        if (file != null) {
            throw new IllegalStateException("Сначала закройте текущую базу данных");
        }

        if (Files.exists(path)) {
            throw new IOException("Файл базы данных уже существует");
        }

        Path parent = path.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        file = new RandomAccessFile(path.toFile(), "rw");

        filePath = path;

        header = new DatabaseHeader();

        header.write(file);

        recordManager = new RecordManager(file);

        save();

    }

    public void clear() throws IOException {
        checkOpen();

        file.setLength(0);

        header = new DatabaseHeader();

        header.write(file);

        recordManager = new RecordManager(file);

        save();
    }

    public void close() throws IOException {
        if (file == null) {
            return;
        }

        save();

        file.close();

        file = null;
        header = null;
        recordManager = null;
    }

    public void open(Path path) throws IOException {
        if (file != null) {
            throw new IllegalStateException("Сначала закройте текущую базу данных");
        }

        if (!Files.exists(path)) {
            throw new IOException("Файл базы данных не существует");
        }

        RandomAccessFile openedFile = new RandomAccessFile(path.toFile(), "rw");

        try {
            if (openedFile.length() < DatabaseHeader.HEADER_SIZE) {
                throw new IOException("Файл слишком мал и не является корректной базой данных");
            }

            DatabaseHeader openedHeader = DatabaseHeader.read(openedFile);
            file = openedFile;
            filePath = path;
            header = openedHeader;
            recordManager = new RecordManager(file);
        } catch (IOException e) {
            openedFile.close();
            throw e;
        }
    }

    public void delete() throws IOException {
        checkOpen();

        Path path = filePath;

        close();

        Files.deleteIfExists(path);

        filePath = null;
    }

    public boolean isOpen() {
        return file != null;
    }

    public Path getFilePath() {
        return filePath;
    }

    public DatabaseHeader getHeader() {
        checkOpen();
        return header;
    }

    public RecordManager getRecordManager() {
        checkOpen();
        return recordManager;
    }
}
