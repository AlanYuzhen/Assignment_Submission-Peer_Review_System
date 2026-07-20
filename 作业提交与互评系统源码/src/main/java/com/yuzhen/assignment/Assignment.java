package com.yuzhen.assignment;

import java.time.LocalDate;

public class Assignment {
    public int id;
    public String title;
    public String description;
    public LocalDate dueDate;
    public String sampleInput;
    public String expectedOutput;
    public boolean programming;
    public boolean peerReviewOpen;

    public boolean isOpen(LocalDate today) {
        return !dueDate.isBefore(today);
    }
}
