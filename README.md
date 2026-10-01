# Laboratory Work No. 2 - File-Based Student Database

A desktop application for managing students of an online school. The project implements a **single-table database using binary files**, without a database management system or SQL. Records and indexes are stored on disk and accessed with Java `RandomAccessFile`.

The application has a graphical interface built with **Java Swing**.

## Features

- Create, open, save, clear and delete a database.
- Add students and edit existing records.
- Search for a student by unique ID or find all students in a particular grade.
- Delete a selected student by ID or delete all students in a grade.
- Reuse the space occupied by deleted records.
- Create and restore a ZIP backup of the database and its indexes.
- Export active student records to an Excel `.xlsx` file.
- Measure the execution time of basic operations using `StatisticsService`.

Each student has a unique ID, full name, email, grade (1–11), subject, course, tariff and registration date.

## Technologies

- Java 17
- Java Swing for the graphical interface
- `RandomAccessFile` and `java.nio.file` for persistent storage
- Apache POI 5.4.1 for Excel export
- Maven for dependency management and building

## Project structure

```text
.
├── pom.xml
├── README.md
├── .gitignore
└── src/
    └── main/
        └── java/
            └── lab/
                ├── Main.java
                ├── database/
                │   ├── Database.java
                │   ├── DatabaseHeader.java
                │   └── RecordManager.java
                ├── index/
                │   ├── PrimaryIndex.java
                │   ├── HashFunction.java
                │   ├── GradeIndex.java
                │   └── FreeSpaceManager.java
                ├── model/
                │   └── Student.java
                ├── service/
                │   ├── StudentService.java
                │   ├── BackupService.java
                │   ├── ImportService.java
                │   └── StatisticsService.java
                └── gui/
                    ├── MainWindow.java
                    ├── StudentDialog.java
                    ├── SearchDialog.java
                    └── DatabaseTableModel.java
```

## How to run

**Requirements:** JDK 17 or newer and Maven, or IntelliJ IDEA with Maven support.

1. Clone the repository and open its root directory in IntelliJ IDEA.
2. Set the project SDK to **JDK 17** and allow Maven to download the dependencies from `pom.xml`.
3. Run `lab.Main`. The main application window will appear.

Alternatively, from the repository root run:

```bash
mvn clean compile
mvn exec:java
```

On the first launch, choose **File -> Create DB** and select an empty directory for the database. Later, use **File -> Open DB** and select the same directory to access the saved records. Use the buttons in the main window to add, edit, search for and delete students.

The ZIP backup and Excel export commands are available from their respective menus. Restoring a backup replaces the four current database files; keep a separate backup before restoring.

## Database file format

The application maintains four files in the directory selected when creating the database:

| File | Purpose |
| --- | --- |
| `students.db` | Main binary data file, containing the database header and student records. |
| `primary.idx` | Primary hash index mapping a unique student ID to its physical record number. |
| `grade.idx` | Secondary index for grades 1–11, implemented using buckets and linked lists. |
| `free.idx` | Stack of record numbers available for reuse after deletion. |

The main database file has a **64-byte header** and **424-byte fixed-length records**. The physical offset of a record is calculated as:

```text
offset = 64 + recordNumber * 424
```

This makes it possible to read or update an individual record directly without loading the entire database into memory. Deleting a student marks the record as inactive, updates both indexes and adds the record number to the free-space stack. A subsequent insertion can reuse that slot.

### Indexing

- **Primary index:** an on-disk hash table with double hashing for collision resolution. It expands and rehashes when the load factor exceeds 0.7.
- **Grade index:** 11 buckets, one for each grade. Each bucket points to a linked list of physical record numbers. Removed index nodes can also be reused.
- **Free-space management:** a file-backed stack of deleted record numbers.

Operations are performed directly on the database and index files. In-memory lists are used only when collecting search results or displaying records in the table.

### Time complexity

Let `n` be the number of records and `k` the number of students in a grade:

| Operation | Complexity |
| --- | --- |
| Search by ID | O(1) expected with the hash index |
| Add a student | O(1) expected, amortized; rehashing takes O(n) |
| Search by grade | O(k) |
| Delete or update a student | O(k) in the worst case for the relevant grade because the secondary index is traversed |
| Display all students | O(n) |

These are algorithmic estimates, not guaranteed disk-access times. Actual performance also depends on the file system and storage device.

## Notes

- Keep `students.db`, `primary.idx`, `grade.idx` and `free.idx` together; the index files correspond to the main data file.
- Generated database files, backups, Excel exports and IDE/build artifacts are excluded from version control by `.gitignore`.
