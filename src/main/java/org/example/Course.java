package org.example;

public class Course {
    private String title;
    private int credits;
    private int year;
    private Department department;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
}