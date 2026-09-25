package com.example.learnkeep.api;

import com.example.learnkeep.KnowledgeEntity;
import java.util.List;

public class SyncKnowledgeResponse {
    public boolean success;
    public List<KnowledgeEntity> topics;
    public int synced;
}