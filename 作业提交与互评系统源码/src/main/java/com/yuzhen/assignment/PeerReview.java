package com.yuzhen.assignment;

import java.time.LocalDateTime;

public class PeerReview {
    public int id;
    public int assignmentId;
    public int submissionId;
    public String reviewerNo;
    public int score;
    public String comment;
    public LocalDateTime createdAt;
    public boolean disputed;
    public boolean resolvedByTeacher;
}
