package vn.hoidanit.jobservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import vn.hoidanit.jobservice.domain.Job;
import vn.hoidanit.jobservice.domain.Skill;
import vn.hoidanit.jobservice.repository.JobRepository;
import vn.hoidanit.jobservice.repository.SkillRepository;
import vn.hoidanit.jobservice.util.constant.LevelEnum;
import vn.hoidanit.jobservice.util.constant.LocationEnum;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Database Seeder for Job Service
 * Creates sample skills and jobs for development and testing ONLY
 *
 * This seeder runs ONLY in dev/test profiles, NOT in production!
 * Production environments should start with empty skills/jobs tables.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder {

    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;

    /**
     * Seed sample skills and jobs for development/testing
     * Only runs in 'dev', 'test', and 'docker' profiles
     */
    @Bean
    @Profile({ "dev", "test", "docker" })
    public CommandLineRunner seedData() {
        return args -> {
            log.info("JOB SERVICE - Starting database seeding...");

            // Check and create skills if not exists
            if (skillRepository.count() > 0) {
                log.info("Skills already exist, skipping skill seeding");
            } else {
                createSkills();
            }

            // Check and create jobs if not exists
            if (jobRepository.count() > 0) {
                log.info("Jobs already exist, skipping job seeding");
            } else {
                createJobs();
            }

            log.info("JOB SERVICE - Database seeding completed successfully!");
        };
    }

    private void createSkills() {
        log.info("--- Creating Skills ---");

        String[] skillNames = {
                "Java", "Spring Boot", "MySQL", "PostgreSQL", "MongoDB",
                "Redis", "Docker", "Kubernetes", "AWS", "React",
                "Angular", "Vue.js", "Node.js", "Python", "JavaScript",
                "TypeScript", "Microservices", "REST API", "GraphQL", "Git"
        };

        for (String skillName : skillNames) {
            try {
                Skill skill = new Skill();
                skill.setName(skillName);
                skill.setCreatedBy("system");
                skill.setCreatedAt(Instant.now());
                skillRepository.save(skill);
                log.info("Created skill: {}", skillName);
            } catch (Exception e) {
                log.error("Failed to create skill: {} | Error: {}", skillName, e.getMessage());
            }
        }
    }

    private void createJobs() {
        log.info("--- Creating Jobs ---");

        // Job 1: Backend Developer
        createJobIfNotExists(
                "Backend Developer",
                LocationEnum.HANOI,
                2000.00,
                5,
                LevelEnum.JUNIOR,
                "Looking for talented Java Spring Boot developers to join our growing team. You will work on building scalable microservices and RESTful APIs.",
                1L,
                Arrays.asList("Java", "Spring Boot", "MySQL", "Docker"));

        // Job 2: Frontend Developer
        createJobIfNotExists(
                "Frontend Developer",
                LocationEnum.HOCHIMINH,
                1800.00,
                3,
                LevelEnum.MIDDLE,
                "Seeking React developer with strong JavaScript skills. You will create modern, responsive web applications.",
                2L,
                Arrays.asList("React", "JavaScript", "TypeScript", "REST API"));

        // Job 3: Full Stack Developer
        createJobIfNotExists(
                "Full Stack Developer",
                LocationEnum.DANANG,
                2500.00,
                2,
                LevelEnum.SENIOR,
                "Full stack developer with expertise in both Java backend and React frontend. Lead technical decisions and mentor junior developers.",
                3L,
                Arrays.asList("Java", "Spring Boot", "React", "MySQL", "Docker", "Microservices"));

        // Job 4: DevOps Engineer
        createJobIfNotExists(
                "DevOps Engineer",
                LocationEnum.HANOI,
                2800.00,
                2,
                LevelEnum.SENIOR,
                "Experience with Docker, Kubernetes, and AWS required. Build and maintain CI/CD pipelines and cloud infrastructure.",
                1L,
                Arrays.asList("Docker", "Kubernetes", "AWS", "Git", "MySQL"));

        // Job 5: Mobile Developer
        createJobIfNotExists(
                "Mobile Developer",
                LocationEnum.HOCHIMINH,
                2200.00,
                4,
                LevelEnum.MIDDLE,
                "iOS/Android development using modern frameworks. Create engaging mobile experiences for millions of users.",
                4L,
                Arrays.asList("React", "JavaScript", "TypeScript", "REST API"));

        // Job 6: Python Developer
        createJobIfNotExists(
                "Python Developer",
                LocationEnum.HANOI,
                2100.00,
                3,
                LevelEnum.MIDDLE,
                "Python developer for data processing and API development. Work with Django/Flask and integrate with various databases.",
                5L,
                Arrays.asList("Python", "PostgreSQL", "MongoDB", "REST API", "Docker"));

        // Job 7: Cloud Architect
        createJobIfNotExists(
                "Cloud Architect",
                LocationEnum.HOCHIMINH,
                3500.00,
                1,
                LevelEnum.SENIOR,
                "Design and implement cloud-native solutions on AWS. Lead migration projects and optimize cloud infrastructure costs.",
                1L,
                Arrays.asList("AWS", "Docker", "Kubernetes", "Microservices", "PostgreSQL"));

        // Job 8: Angular Developer
        createJobIfNotExists(
                "Angular Developer",
                LocationEnum.DANANG,
                1900.00,
                3,
                LevelEnum.MIDDLE,
                "Build enterprise applications using Angular framework. Strong understanding of TypeScript and RxJS required.",
                3L,
                Arrays.asList("Angular", "TypeScript", "JavaScript", "REST API"));
    }

    private void createJobIfNotExists(String name, LocationEnum location, double salary, int quantity,
            LevelEnum level, String description, Long companyId,
            List<String> skillNames) {
        try {
            // Check if job already exists by name
            boolean exists = jobRepository.findAll().stream()
                    .anyMatch(j -> j.getName().equalsIgnoreCase(name));

            if (!exists) {
                Job job = new Job();
                job.setName(name);
                job.setLocation(location); // Now LocationEnum
                job.setSalary(salary);
                job.setQuantity(quantity);
                job.setLevel(level);
                job.setDescription(description);
                job.setCompanyId(companyId);
                job.setActive(true);
                job.setStartDate(Instant.now());
                job.setEndDate(Instant.now().plus(90, ChronoUnit.DAYS)); // 3 months from now
                job.setCreatedBy("system");
                job.setCreatedAt(Instant.now());

                // Find and set skills
                List<Skill> skills = new ArrayList<>();
                for (String skillName : skillNames) {
                    skillRepository.findAll().stream()
                            .filter(s -> s.getName().equalsIgnoreCase(skillName))
                            .findFirst()
                            .ifPresent(skills::add);
                }
                job.setSkills(skills);

                jobRepository.save(job);
                log.info("Created job: {} | Location: {} | Level: {} | Skills: {}",
                        name, location.name(), level, skillNames.size());
            } else {
                log.info("Job already exists: {}", name);
            }
        } catch (Exception e) {
            log.error("Failed to create job: {} | Error: {}", name, e.getMessage(), e);
        }
    }
}
