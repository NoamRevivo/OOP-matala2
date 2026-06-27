package org.example;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main
{

    public static void main(String[] args)
    {

        Department csDept = new Department();
        csDept.setName("מדעי המחשב");

        Department mathDept = new Department();
        mathDept.setName("מתמטיקה");

        Course javaCourse = new Course();
        javaCourse.setTitle("תכנות מונחה עצמים בשפת JAVA");
        javaCourse.setCredits(4);
        javaCourse.setYear(2025);
        javaCourse.setDepartment(csDept);

        Course algoCourse = new Course();
        algoCourse.setTitle("אלגוריתמים");
        algoCourse.setCredits(5);
        algoCourse.setYear(2026);
        algoCourse.setDepartment(csDept);

        Course algebraCourse = new Course();
        algebraCourse.setTitle("אלגברה לינארית");
        algebraCourse.setCredits(3);
        algebraCourse.setYear(2025);
        algebraCourse.setDepartment(mathDept);

        Student s1 = new Student();
        s1.setName("נועם");
        s1.setCourses(Arrays.asList(javaCourse, algoCourse));

        Student s2 = new Student();
        s2.setName("קלודין");
        s2.setCourses(Arrays.asList(algebraCourse));

        Student s3 = new Student();
        s3.setName("שלמה");
        s3.setCourses(Arrays.asList(javaCourse, algebraCourse));

        List<Student> students = Arrays.asList(s1, s2, s3);


        System.out.println("סטודנטים במדעי המחשב: " + studentsInDepartment(students, "מדעי המחשב"));
        System.out.println("הסטודנט/ית עם מרב נקודות הזכות: " + topStudentByCredits(students));
        System.out.println("קורסים משנת 2026 ומעלה: " + courseTitlesByDepartmentFromYear(students, 2026));
    }

    // ==========================================
    // סעיף א': רשימת סטודנטים ייחודית וממוינת
    // ==========================================
    public static List<String> studentsInDepartment(List<Student> students, String departmentName) {
        return students.stream()
                .filter(student -> student.getCourses().stream()
                        .anyMatch(course -> course.getDepartment().getName().equals(departmentName)))
                .map(Student::getName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // ==========================================
    // סעיף ב': שמו של הסטודנט עם סכום נקודות הזכות הגבוה ביותר
    // ==========================================
    public static String topStudentByCredits(List<Student> students) {
        return students.stream()
                .max((s1, s2) -> {
                    int sum1 = s1.getCourses().stream().mapToInt(Course::getCredits).sum();
                    int sum2 = s2.getCourses().stream().mapToInt(Course::getCredits).sum();
                    return Integer.compare(sum1, sum2);
                })
                .map(Student::getName)
                .orElse(null);
    }

    // ==========================================
    // סעיף ג': מפה של שם מחלקה לרשימת שמות קורסים ייחודיים
    // ==========================================
    public static Map<String, List<String>> courseTitlesByDepartmentFromYear(List<Student> students, int fromYear) {
        return students.stream()
                .flatMap(student -> student.getCourses().stream())
                .filter(course -> course.getYear() >= fromYear)
                .distinct()
                .collect(Collectors.groupingBy(
                        course -> course.getDepartment().getName(),
                        Collectors.mapping(Course::getTitle, Collectors.toList())
                ));
    }
}