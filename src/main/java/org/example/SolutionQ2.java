package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SolutionQ2 {

    public List<String> studentsInDepartment(List<Student> students, String departmentName) {
        return students.stream()
                .filter(s -> s.getCourses().stream()
                        .anyMatch(c -> departmentName.equals(c.getDepartment().getName())))
                .map(Student::getName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public String topStudentByCredits(List<Student> students) {
        return students.stream()
                .max((s1, s2) -> Integer.compare(
                        s1.getCourses().stream().mapToInt(Course::getCredits).sum(),
                        s2.getCourses().stream().mapToInt(Course::getCredits).sum()
                ))
                .map(Student::getName)
                .get();
    }
    public static Map<String, List<String>> getCoursesByDepartment(List<Student> students, int year) {

        return students.stream()
                .flatMap(student -> student.getCourses().stream())
                .filter(course -> course.getYear() >= year)
                .collect(Collectors.groupingBy(
                        course -> course.getDepartment().getName(),
                        Collectors.collectingAndThen(
                                Collectors.mapping(
                                        Course::getTitle,
                                        Collectors.toSet()
                                ),
                                ArrayList::new
                        )
                ));
    }
     }