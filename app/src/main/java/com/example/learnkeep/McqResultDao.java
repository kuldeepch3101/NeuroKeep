package com.example.learnkeep;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface McqResultDao {
    @Insert
    void insert(McqResultEntity result);

    @Query("SELECT * FROM mcq_results WHERE topicId = :topicId ORDER BY takenAt DESC")
    List<McqResultEntity> getByTopic(int topicId);

    @Query("SELECT * FROM mcq_results WHERE topicId = :topicId ORDER BY takenAt DESC LIMIT 1")
    McqResultEntity getLatestByTopic(int topicId);

    @Query("SELECT * FROM mcq_results ORDER BY takenAt DESC")
    List<McqResultEntity> getAll();

    @Query("SELECT AVG(CAST(score AS FLOAT)/total) FROM mcq_results")
    float getOverallAccuracy();

    @Query("SELECT COUNT(*) FROM mcq_results")
    int getTotalTests();
}
