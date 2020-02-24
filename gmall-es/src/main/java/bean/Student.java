package bean;

public class Student {
    private String stu_id;
    private String name;

    public Student() {
    }

    public Student(String stu_id, String name) {
        this.stu_id = stu_id;
        this.name = name;
    }

    public String getStu_id() {
        return stu_id;
    }

    public String getName() {
        return name;
    }

    public void setStu_id(String stu_id) {
        this.stu_id = stu_id;
    }

    public void setName(String name) {
        this.name = name;
    }
}
