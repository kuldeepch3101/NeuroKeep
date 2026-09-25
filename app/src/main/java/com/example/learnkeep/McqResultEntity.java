package com.example.learnkeep;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "mcq_results")
public class McqResultEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int topicId;          // FK → KnowledgeEntity.id
    public String topicTitle;    // denormalised for quick display

    public int score;            // number of correct answers (0-5)
    public int total;            // total questions (always 5 currently)
    public int confidenceBefore; // confidence when test was triggered
    public int confidenceAfter;  // new confidence computed from score
    public long takenAt;         // epoch ms
    // "REMINDER" or "MANUAL"
    public String triggerType;
}
