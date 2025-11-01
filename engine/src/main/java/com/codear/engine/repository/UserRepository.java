package com.codear.engine.repository;

import org.springframework.stereotype.Repository;
import com.codear.engine.entity.User;

// This import is not used for @Param, use the one below
// import io.lettuce.core.dynamic.annotation.Param; 

// Correct import for @Param
import org.springframework.data.repository.query.Param; 

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserRepository extends JpaRepository<User,Long>{

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.dailyStreak = :dailyStreak WHERE u.id = :userId")
    void updateStreak(@Param("userId") Long userId, @Param("dailyStreak") Integer streak);
    
    @Modifying
    @Transactional 
    @Query("UPDATE User u SET u.dailyStreak = u.dailyStreak + 1 WHERE u.id = :userId")
    void incrementStreak(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET " +
           "u.problemSolvedTotal = u.problemSolvedTotal + 1, " +
           "u.problemSolvedEasy = CASE WHEN :difficulty = 'Easy' THEN u.problemSolvedEasy + 1 ELSE u.problemSolvedEasy END, " +
           "u.problemSolvedMedium = CASE WHEN :difficulty = 'Medium' THEN u.problemSolvedMedium + 1 ELSE u.problemSolvedMedium END, " +
           "u.problemSolvedHard = CASE WHEN :difficulty = 'Hard' THEN u.problemSolvedHard + 1 ELSE u.problemSolvedHard END " +
           "WHERE u.id = :userId")
    void incrementProblemCount(@Param("userId") Long userId, @Param("difficulty") String difficulty);

}