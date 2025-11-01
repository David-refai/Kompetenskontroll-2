package org.example.entities;

import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Domain entity representing a Candidate profile.
 * =
 * Contains personal and professional information such as:
 *  - name, age, industry, and years of experience.
 * =
 * Used across the repository and service layers.
 */
public class Candidate {

    /** Unique identifier (auto-assigned by the repository if not set). */
    @Getter
    @Setter
    private long id;

    /** Candidate’s full name. */
    @Setter
    private String name;

    /** Candidate’s age in years. */
    @Setter
    private int age;

    /** The professional industry or field (e.g., IT, Education, Healthcare). */
    @Setter
    private String industry;

    /** Number of years of professional experience. */
    @Setter
    private int yearsOfExperience;

    private String dateTimeOfRegister;

    /**
     * Custom constructor (without ID).
     * Used when creating a new candidate before the repository assigns an ID.
     *
     * @param name candidate name
     * @param age candidate age
     * @param industry candidate industry
     * @param yearsOfExperience total years of experience
     */
    public Candidate(String name, int age, String industry, int yearsOfExperience) {
        this.name = name;
        this.age = age;
        this.industry = industry;
        this.yearsOfExperience = yearsOfExperience;
    }

    public Candidate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.dateTimeOfRegister = LocalDateTime.now().format(formatter);

    }



    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getIndustry() {
        return industry;
    }

    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public String getDateTimeOfRegister() {
        return dateTimeOfRegister;
    }

    public void setDateTimeOfRegister(String dateTimeOfRegister) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.dateTimeOfRegister = LocalDateTime.now().format(formatter);
    }

    @Override
    public String toString() {
        return "Candidate{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", industry='" + industry + '\'' +
                ", yearsOfExperience=" + yearsOfExperience +
                ", dateTimeOfRegister='" + dateTimeOfRegister + '\'' +
                '}';
    }


}
