package com.yuzhen.assignment;

import java.time.LocalDateTime;

public class Submission {
    public int id;
    public int assignmentId;
    public String studentNo;
    public String studentName;
    public String fileName;
    public String filePath;
    public String textContent;
    public LocalDateTime submittedAt;
    public String judgeStatus;
    public String judgeMessage;
    public double autoScore;
    public boolean plagiarismFlag;
}
