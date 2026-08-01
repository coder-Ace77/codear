package com.codear.engine.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String username;
    private String name;
    private String email;
    private String password;
    private String role;
    private Integer dailyStreak;
    private Integer problemSolvedEasy;
    private Integer problemSolvedMedium;
    private Integer problemSolvedHard;
    private Integer problemSolvedTotal;

}
