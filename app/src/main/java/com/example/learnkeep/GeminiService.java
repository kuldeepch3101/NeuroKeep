package com.example.learnkeep;

import android.os.Handler;
import android.os.Looper;

import com.example.learnkeep.api.ApiClient;
import com.example.learnkeep.api.ApiService;
import com.example.learnkeep.api.GeminiChatRequest;
import com.example.learnkeep.api.GeminiChatResponse;
import com.example.learnkeep.api.GeminiMcqRequest;
import com.example.learnkeep.api.GeminiMcqResponse;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;


public class GeminiService {
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainThread = new Handler(Looper.getMainLooper());

    public interface McqCallback {
        void onSuccess(McqQuestion[] questions);
        void onError(String error);
    }

    public interface ChatCallback {
        void onSuccess(String reply);
        void onError(String error);
    }

    //MCQ Generation
    public static void generateMcqTest(
            android.content.Context context,
            String  topicTitle,
            String  notes,
            int     confidence,
            McqCallback callback) {

        executor.execute(() -> {
            try {
                ApiService api = ApiClient.getClient(context)
                        .create(ApiService.class);

                Call<GeminiMcqResponse> call = api.generateMcq(
                        new GeminiMcqRequest(topicTitle, notes, confidence));

                Response<GeminiMcqResponse> response = call.execute();

                if (!response.isSuccessful() || response.body() == null) {
                    String err = "Server error " + response.code();
                    mainThread.post(() -> callback.onError(err));
                    return;
                }

                GeminiMcqResponse body = response.body();

                if (!body.success || body.questions == null || body.questions.isEmpty()) {
                    String err = body.message != null ? body.message : "No questions returned";
                    mainThread.post(() -> callback.onError(err));
                    return;
                }

                // Convert List<McqQuestion> to McqQuestion[]
                McqQuestion[] arr = body.questions.toArray(new McqQuestion[0]);
                mainThread.post(() -> callback.onSuccess(arr));

            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Network error";
                mainThread.post(() -> callback.onError(msg));
            }
        });
    }
    //AI Chat
    public static void chat(
            android.content.Context context,
            String userMessage,
            List<KnowledgeEntity> allTopics,
            ChatCallback callback) {

        executor.execute(() -> {
            try {
                ApiService api = ApiClient.getClient(context)
                        .create(ApiService.class);
                Call<GeminiChatResponse> call = api.chat(
                        new GeminiChatRequest(userMessage, allTopics));
                Response<GeminiChatResponse> response = call.execute();
                if (!response.isSuccessful() || response.body() == null) {
                    String err = "Server error " + response.code();
                    mainThread.post(() -> callback.onError(err));
                    return;
                }
                GeminiChatResponse body = response.body();
                if (!body.success || body.reply == null) {
                    String err = body.message != null ? body.message : "No reply received";
                    mainThread.post(() -> callback.onError(err));
                    return;
                }
                mainThread.post(() -> callback.onSuccess(body.reply));
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Network error";
                mainThread.post(() -> callback.onError(msg));
            }
        });
    }
}
