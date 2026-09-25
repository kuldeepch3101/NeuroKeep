package com.example.learnkeep.api;

import com.example.learnkeep.KnowledgeEntity;
import java.util.List;

public class SyncKnowledgeRequest {
    public List<KnowledgeEntity> topics;
    public SyncKnowledgeRequest(List<KnowledgeEntity> topics) {
        this.topics = topics;
    }
}