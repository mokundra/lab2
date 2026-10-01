package lab.index;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

public class GradeIndex {
    private static final int MAGIC = 0x47524458;

    private static final int MIN_GRADE = 1;
    private static final int MAX_GRADE = 11;
    private static final int BUCKET_COUNT = 11;

    private static final int HEADER_SIZE = 32;
    private static final int BUCKET_SIZE = 8;
    private static final int NODE_SIZE = 24;

    private static final byte OCCUPIED = 1;
    private static final byte DELETED = 2;

    private static final long NULL_NODE = -1;

    private Path filePath;
    private RandomAccessFile file;
    private long nodeCount;
    private long activeEntries;
    private long freeListHead;

    @FunctionalInterface
    public interface RecordHandler {
        void handle(long recordNumber) throws IOException;//что-то сделать с каждым элементом
    }

    private void checkOpen() {
        if (file == null) {
            throw new IllegalStateException("Индекс классов не открыт");
        }
    }

    private void checkGrade(int grade) {
        if (grade < MIN_GRADE || grade > MAX_GRADE) {
            throw new IllegalArgumentException("Класс должен быть от 1 до 11");
        }
    }

    private long getBucketOffset(int grade) {
        checkGrade(grade);

        return HEADER_SIZE + (long) (grade - MIN_GRADE) * BUCKET_SIZE;
    }

    private long getNodesStart() {
        return HEADER_SIZE + (long) BUCKET_COUNT * BUCKET_SIZE;
    }

    private long getNodeOffset(long nodeNumber) {

        if (nodeNumber < 0 || nodeNumber >= nodeCount) {
            throw new IllegalArgumentException("Некорректный номер узла");
        }

        return getNodesStart() + nodeNumber * NODE_SIZE;
    }

    private void writeHeader() throws IOException {
        file.seek(0);

        file.writeInt(MAGIC);
        file.writeLong(nodeCount);
        file.writeLong(activeEntries);
        file.writeLong(freeListHead);

        while (file.getFilePointer() < HEADER_SIZE) {
            file.writeByte(0);
        }
    }

    private long readBucketHead(int grade) throws IOException {

        file.seek(getBucketOffset(grade));
        return file.readLong();
    }

    private void writeBucketHead(int grade, long nodeNumber) throws IOException {
        file.seek(getBucketOffset(grade));

        file.writeLong(nodeNumber);
    }

    private void InitializeBuckets() throws IOException {
        for (int grade = MIN_GRADE; grade <= MAX_GRADE; grade++) {
            writeBucketHead(grade, NULL_NODE);
        }
    }

    private byte readNodeState(long nodeNumber) throws IOException {
        file.seek(getNodeOffset(nodeNumber));

        return file.readByte();
    }

    private long readRecordNumber(long nodeNumber) throws IOException {

        file.seek(getNodeOffset(nodeNumber) + 8);

        return file.readLong();
    }

    private long readNextNode(long nodeNumber) throws IOException {
        file.seek(getNodeOffset(nodeNumber) + 16);

        return file.readLong();
    }

    private void writeNode(long nodeNumber, byte state, long recordNumber, long nextNode) throws IOException {
        file.seek(getNodeOffset(nodeNumber));

        file.writeByte(state);

        for (int i = 0; i < 7; i++) {
            file.writeByte(0);
        }

        file.writeLong(recordNumber);
        file.writeLong(nextNode);
    }

    private long allocateNode() throws IOException {
        if (freeListHead != NULL_NODE) {
            long nodeNumber = freeListHead;

            freeListHead = readNextNode(nodeNumber);

            return nodeNumber;
        }

        long nodeNumber = nodeCount;

        nodeCount++;

        return nodeNumber;
    }

    public void create(Path path) throws IOException {
        if (file != null) {
            throw new IllegalStateException("Сначала закройте текущий индекс классов");
        }

        if (Files.exists(path)) {
            throw new IOException("Файл индекса классов уже существует");
        }

        Path parent = path.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        file = new RandomAccessFile(path.toFile(), "rw");

        filePath = path;

        nodeCount = 0;
        activeEntries = 0;
        freeListHead = NULL_NODE;

        writeHeader();
        InitializeBuckets();

        file.getFD().sync();
    }

    public void open(Path path) throws IOException {
        if (file != null) {
            throw new IllegalStateException("Сначала закройте текущий индекс классов");
        }

        if (!Files.exists(path)) {
            throw new IOException("Файл индекса классов не существует");
        }

        RandomAccessFile openedFile = new RandomAccessFile(path.toFile(), "rw");

        long minLength = HEADER_SIZE + (long) BUCKET_COUNT * BUCKET_SIZE;

        if (openedFile.length() < minLength) {
            openedFile.close();
            throw new IOException("Некорректный файл индекса классов");
        }

        int magic = openedFile.readInt();

        if (magic != MAGIC) {
            openedFile.close();
            throw new IOException("Некорректный формат индекса классов");
        }

        long openedNodeCount = openedFile.readLong();

        long openedActiveEntries = openedFile.readLong();

        long openedFreeListHead = openedFile.readLong();

        if (openedNodeCount < 0 || openedActiveEntries < 0 || openedActiveEntries > openedNodeCount) {
            openedFile.close();
            throw new IOException("Повреждён заголовок индекса классов");
        }

        long expectedLength = minLength + openedNodeCount * NODE_SIZE;

        if (openedFile.length() < expectedLength) {
            openedFile.close();

            throw new IOException(
                    "Файл индекса классов повреждён"
            );
        }
        file = openedFile;
        filePath = path;

        nodeCount = openedNodeCount;
        activeEntries = openedActiveEntries;
        freeListHead = openedFreeListHead;


    }

    public void add(int grade, long recordNumber) throws IOException {
        checkOpen();
        checkGrade(grade);

        if (recordNumber < 0) {
            throw new IllegalArgumentException("Номер записи не может быть отрицательным");
        }

        long currentHead = readBucketHead(grade);

        long nodeNumber = allocateNode();

        writeNode(nodeNumber, OCCUPIED, recordNumber, currentHead);

        writeBucketHead(grade, nodeNumber);

        activeEntries++;

        writeHeader();
    }

    public boolean contains(int grade, long recordNumber) throws IOException {
        checkOpen();
        checkGrade(grade);

        long current = readBucketHead(grade);

        long steps = 0;

        while (current != NULL_NODE) {
            if (steps++ >= nodeCount) {
                throw new IOException( "Повреждён индекс классов");
            }

            if (readNodeState(current) != OCCUPIED) {
                throw new IOException("Повреждена цепочка индекса классов");
            }

            if (readRecordNumber(current) == recordNumber) {
                return true;
            }

            current=readNextNode(current);
        }
        return false;
    }

    public void forEach(int grade, RecordHandler handler) throws IOException{
        checkOpen();
        checkGrade(grade);

        if (handler == null) {
            throw new IllegalArgumentException("Обработчик не может быть null");
        }

        long current = readBucketHead(grade);

        long steps = 0;

        while (current != NULL_NODE){
            if (steps++ >= nodeCount){
                throw new IOException("Повреждён индекс классов");
            }

            if (readNodeState(current) != OCCUPIED) {
                throw new IOException("Повреждена цепочка индекса классов");
            }

            long recordNumber = readRecordNumber(current);

            handler.handle(recordNumber);

            current = readNextNode(current);
        }
    }

    public boolean remove(int grade, long recordNumber) throws IOException{
        checkOpen();
        checkGrade(grade);

        long current = readBucketHead(grade);

        long previous = NULL_NODE;

        long steps = 0;

        while(current != NULL_NODE){
            if (steps++ >= nodeCount) {
                throw new IOException("Повреждён индекс классов");
            }

            if (readNodeState(current) != OCCUPIED) {
                throw new IOException("Повреждена цепочка индекса классов");
            }

            long next = readNextNode(current);

            if (readRecordNumber(current) == recordNumber){
                if (previous==NULL_NODE){
                    writeBucketHead(grade,next);
                }
                else{
                    long previousRecord = readRecordNumber(previous);
                    writeNode(previous, OCCUPIED, previousRecord, next);
                }
                writeNode(current,DELETED,-1,freeListHead);

                freeListHead = current;

                activeEntries--;
                writeHeader();
                return true;
            }
            previous=current;
            current=next;
        }
        return false;
    }

    public void clear() throws IOException {
        checkOpen();
        file.setLength(0);
        nodeCount = 0;
        activeEntries = 0;
        freeListHead = NULL_NODE;
        writeHeader();
        InitializeBuckets();
        file.getFD().sync();
    }

    public void close() throws IOException {

        if (file == null) {
            return;
        }

        writeHeader();
        file.getFD().sync();
        file.close();
        file = null;
        filePath = null;
        nodeCount = 0;
        activeEntries = 0;
        freeListHead = NULL_NODE;
    }

    public long getActiveEntries() {
        checkOpen();
        return activeEntries;
    }

    public long getFirstRecordNumber(int grade) throws IOException {
        checkOpen();
        checkGrade(grade);

        long nodeNumber = readBucketHead(grade);

        if (nodeNumber == NULL_NODE) {
            return -1;
        }

        if (readNodeState(nodeNumber) != OCCUPIED) {
            throw new IOException("Повреждена цепочка индекса классов");
        }
        return readRecordNumber(nodeNumber);
    }

}
