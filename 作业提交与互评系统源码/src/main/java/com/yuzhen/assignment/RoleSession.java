package com.yuzhen.assignment;

public final class RoleSession {
    public enum Role {
        TEACHER,
        STUDENT
    }

    private final Role role;
    private final StudentProfile student;

    private RoleSession(Role role, StudentProfile student) {
        this.role = role;
        this.student = student;
    }

    public static RoleSession teacher() {
        return new RoleSession(Role.TEACHER, null);
    }

    public static RoleSession student(StudentProfile student) {
        if (student == null) {
            throw new IllegalArgumentException("学生身份不能为空");
        }
        return new RoleSession(Role.STUDENT, student);
    }

    public Role role() {
        return role;
    }

    public StudentProfile student() {
        return student;
    }

    public void requireTeacher() {
        if (role != Role.TEACHER) {
            throw new SecurityException("无权访问教师端管理功能");
        }
    }

    public StudentProfile requireStudent() {
        if (role != Role.STUDENT || student == null) {
            throw new SecurityException("无权以学生身份提交或互评");
        }
        return student;
    }

    public void requireStudent(String studentNo) {
        StudentProfile current = requireStudent();
        if (!current.studentNo.equals(studentNo)) {
            throw new SecurityException("只能操作当前登录学生的数据");
        }
    }
}
