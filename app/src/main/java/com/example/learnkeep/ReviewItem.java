package com.example.learnkeep;
public class ReviewItem {
    public String title,day,tags;
    public int priority;
    public ReviewItem(String title, String day, String tags,int priority) {
        this.title = title;
        this.day = day;
        this.tags = tags;
        this.priority = priority;
    }
}
