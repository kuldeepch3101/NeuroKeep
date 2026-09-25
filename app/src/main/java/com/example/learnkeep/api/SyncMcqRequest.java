package com.example.learnkeep.api;

import com.example.learnkeep.McqResultEntity;
import java.util.List;

public class SyncMcqRequest {
    public List<McqResultEntity> results;
    public SyncMcqRequest(List<McqResultEntity> results) {
        this.results = results;
    }
}