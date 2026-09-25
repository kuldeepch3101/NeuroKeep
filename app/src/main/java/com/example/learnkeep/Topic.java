package com.example.learnkeep;

public class Topic {

    private String title;
    private String date;
    private String confidence;

    public Topic(String title, String date, String confidence) {
        this.title = title;
        this.date = date;
        this.confidence = confidence;
    }

    public String getTitle() {
        return title;
    }

    public String getDate() {
        return date;
    }

    public String getConfidence() {
        return confidence;
    }
}
