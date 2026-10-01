package lab.database;

import java.io.IOException;
import java.io.RandomAccessFile;

public class DatabaseHeader {
    public static final int MAGIC = 0x4C414232;
    public static final int HEADER_SIZE = 64;
    public static final int RECORD_SIZE = 424;

    private long totalRecords;
    private long activeRecords;
    private long createdAt;
    private long modifiedAt;

    public DatabaseHeader() {
        this.totalRecords = 0;
        this.activeRecords = 0;

        long currentTime = System.currentTimeMillis();

        this.createdAt = currentTime;
        this.modifiedAt = currentTime;
    }

    public DatabaseHeader(long totalRecords,
                          long activeRecords,
                          long createdAt,
                          long modifiedAt) {
        this.totalRecords = totalRecords;
        this.activeRecords = activeRecords;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    public void write(RandomAccessFile file) throws IOException {
        file.seek(0);

        file.writeInt(MAGIC);
        file.writeInt(HEADER_SIZE);
        file.writeInt(RECORD_SIZE);

        file.writeLong(totalRecords);
        file.writeLong(activeRecords);
        file.writeLong(createdAt);
        file.writeLong(modifiedAt);

        while (file.getFilePointer() < HEADER_SIZE) {
            file.writeByte(0);
        }
    }

    public static DatabaseHeader read(RandomAccessFile file) throws IOException {
        file.seek(0);

        int magic = file.readInt();
        if (magic != MAGIC) {
            throw new IOException("Некорректный формат файла базы данных");
        }

        int headerSize = file.readInt();
        int recordSize = file.readInt();

        if (headerSize != HEADER_SIZE || recordSize != RECORD_SIZE) {
            throw new IOException("Некорректная структура базы данных");
        }

        long totalRecords = file.readLong();
        long activeRecords = file.readLong();
        long createdAt = file.readLong();
        long modifiedAt = file.readLong();

        return new DatabaseHeader(totalRecords, activeRecords, createdAt, modifiedAt);
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public long getActiveRecords() {
        return activeRecords;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getModifiedAt() {
        return modifiedAt;
    }

    public void incrementTotalRecords() {
        totalRecords++;
        modifiedAt = System.currentTimeMillis();
    }

    public void incrementActiveRecords() {
        activeRecords++;
        modifiedAt = System.currentTimeMillis();
    }

    public void decrementActiveRecords() {
        if (activeRecords <= 0) {
            throw new IllegalStateException(
                    "Количество активных записей не может быть меньше нуля"
            );
        }
        activeRecords--;
        modifiedAt = System.currentTimeMillis();
    }

    public void updateModifiedTime() {
        modifiedAt = System.currentTimeMillis();
    }
}


