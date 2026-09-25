package com.example.learnkeep.api;
public class GeminiMcqRequest {
    public String title;
    public String notes;
    public int confidence;

    public GeminiMcqRequest(String title,String notes,int confidence) {
        this.title = title;
        this.notes = notes;
        this.confidence = confidence;
    }
}
