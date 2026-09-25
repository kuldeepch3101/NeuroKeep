package com.example.learnkeep;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "knowledge")
public class KnowledgeEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String title;
    public String notes;
    public String youtubeLinks;
    public String confidence;   // "1"–"10"
    public String tags;
    public String attachmentPaths; // comma-separated file paths
    public long createdAt;
    public int  reviewCount;
    public int  lastTestScore = -1;
    public int  lastTestTotal = 0;
    public long lastTestAt    = 0;
    public boolean isCompleted = false;
}
