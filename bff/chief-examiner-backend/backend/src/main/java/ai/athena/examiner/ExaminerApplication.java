package ai.athena.examiner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ExaminerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExaminerApplication.class, args);
    }
}
