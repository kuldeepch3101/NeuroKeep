package com.example.learnkeep.api;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface ApiService {

    @POST("login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("send-signup-otp")
    Call<Map<String, Object>> sendSignupOtp(@Body Map<String, String> body);

    @POST("signup")
    Call<LoginResponse> signupWithOtp(@Body OtpSignupRequest request);

    @POST("forgot-password-otp")
    Call<Map<String, Object>> sendForgotOtp(@Body Map<String, String> body);

    @POST("reset-password")
    Call<Map<String, Object>> resetPassword(@Body Map<String, String> body);

    @PUT("update-profile-pic")
    Call<Map<String, Object>> updateProfilePic(@Body Map<String, String> body);

    @GET("profile")
    Call<ProfileResponse> getProfile();

    @POST("gemini/mcq")
    Call<GeminiMcqResponse> generateMcq(@Body GeminiMcqRequest request);

    @POST("gemini/chat")
    Call<GeminiChatResponse> chat(@Body GeminiChatRequest request);

    // Sync UP (device → MongoDB)
    @POST("sync/knowledge")
    Call<SyncKnowledgeResponse> syncKnowledge(@Body SyncKnowledgeRequest request);

    @POST("sync/mcq-results")
    Call<SyncMcqResponse> syncMcqResults(@Body SyncMcqRequest request);

    // Restore DOWN (MongoDB → device)
    @GET("sync/knowledge")
    Call<SyncKnowledgeResponse> restoreKnowledge();

    @GET("sync/mcq-results")
    Call<SyncMcqResponse> restoreMcqResults();
}
