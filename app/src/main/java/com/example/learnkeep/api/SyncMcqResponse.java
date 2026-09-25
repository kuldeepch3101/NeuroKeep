package com.example.learnkeep.api;

import com.example.learnkeep.McqResultEntity;
import java.util.List;

public class SyncMcqResponse {
    public boolean success;
    public List<McqResultEntity> results;
    public int synced;
}