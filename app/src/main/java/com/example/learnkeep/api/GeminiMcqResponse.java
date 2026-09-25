package com.example.learnkeep.api;

import com.example.learnkeep.McqQuestion;
import java.util.List;
public class GeminiMcqResponse {
    public boolean success;
    public List<McqQuestion> questions;
    public String message;   // error message if success=false
}
