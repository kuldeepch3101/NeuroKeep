package com.example.learnkeep;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface KnowledgeDao {

    @Insert
    void insert(KnowledgeEntity entity);

    @Update
    void update(KnowledgeEntity entity);

    @Delete
    void delete(KnowledgeEntity entity);

    @Query("SELECT * FROM knowledge ORDER BY createdAt DESC")
    List<KnowledgeEntity> getAll();

    @Query("SELECT * FROM knowledge WHERE id = :id")
    KnowledgeEntity getById(int id);
}
