package lab.model;

public class Student {
    private boolean active;
    private long id;
    private String fullName;
    private String email;
    private int grade;
    private String subject;
    private String course;
    private String tariff;
    private long registrationDate;

    //конструктор для новых
    public Student(long id, String fullName, String email, int grade, String subject, String course, String tariff, long registrationDate) {
        this.active = true;
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.grade = grade;
        this.subject = subject;
        this.course = course;
        this.tariff = tariff;
        this.registrationDate = registrationDate;
    }

    //конструктор для уже существующих
    public Student(boolean active, long id, String fullName, String email, int grade, String subject, String course, String tariff, long registrationDate) {
        this.active = active;
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.grade = grade;
        this.subject = subject;
        this.course = course;
        this.tariff = tariff;
        this.registrationDate = registrationDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getGrade() {
        return grade;
    }

    public void setGrade(int grade) {
        this.grade = grade;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getTariff() {
        return tariff;
    }

    public void setTariff(String tariff) {
        this.tariff = tariff;
    }

    public long getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(long registrationDate) {
        this.registrationDate = registrationDate;
    }

    @Override
    public String toString() {
        return "Student{" +
                "active=" + active +
                ", id=" + id +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", grade=" + grade +
                ", subject='" + subject + '\'' +
                ", course='" + course + '\'' +
                ", tariff='" + tariff + '\'' +
                ", registrationDate=" + registrationDate +
                '}';
    }


}
