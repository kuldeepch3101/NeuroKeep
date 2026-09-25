package com.example.learnkeep.api;

import com.example.learnkeep.KnowledgeEntity;
import java.util.ArrayList;
import java.util.List;
public class GeminiChatRequest {
    public String message;
    public List<TopicPayload> topics;

    public GeminiChatRequest(String message, List<KnowledgeEntity> entities) {
        this.message = message;
        this.topics = new ArrayList<>();
        if (entities != null) {
            for (KnowledgeEntity e : entities) {
                this.topics.add(new TopicPayload(e));
            }
        }
    }
    public static class TopicPayload {
        public String title;
        public String confidence;
        public String tags;
        public String notes;
        public int lastTestScore;
        public int lastTestTotal;
        public boolean isCompleted;

        public TopicPayload(KnowledgeEntity e) {
            this.title         = e.title;
            this.confidence    = e.confidence;
            this.tags          = e.tags;
            this.notes         = e.notes != null && e.notes.length() > 200 ? e.notes.substring(0, 200) : e.notes;
            this.lastTestScore = e.lastTestScore;
            this.lastTestTotal = e.lastTestTotal;
            this.isCompleted   = e.isCompleted;
        }
    }
}
