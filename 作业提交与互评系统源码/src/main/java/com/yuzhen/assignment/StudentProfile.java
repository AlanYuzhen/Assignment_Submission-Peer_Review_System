package com.yuzhen.assignment;

public class StudentProfile {
    public final String studentNo;
    public final String name;

    public StudentProfile(String studentNo, String name) {
        this.studentNo = studentNo;
        this.name = name;
    }

    @Override
    public String toString() {
        return studentNo + " - " + name;
    }
}
