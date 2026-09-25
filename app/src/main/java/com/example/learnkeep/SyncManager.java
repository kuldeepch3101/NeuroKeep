package com.example.learnkeep;

import android.content.Context;
import android.util.Log;

import com.example.learnkeep.api.ApiClient;
import com.example.learnkeep.api.ApiService;
import com.example.learnkeep.api.SyncKnowledgeRequest;
import com.example.learnkeep.api.SyncKnowledgeResponse;
import com.example.learnkeep.api.SyncMcqRequest;
import com.example.learnkeep.api.SyncMcqResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SyncManager {

    public interface SyncCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    // ── Push local Room data UP to MongoDB ─────────────────────────
    public static void syncToCloud(Context context, SyncCallback callback) {
        AppDatabase db = AppDatabase.getInstance(context);
        ApiService api = ApiClient.getClient(context).create(ApiService.class);

        new Thread(() -> {
            List<KnowledgeEntity> topics = db.knowledgeDao().getAll();
            List<McqResultEntity> results = db.mcqResultDao().getAll();

            // Sync topics first
            api.syncKnowledge(new SyncKnowledgeRequest(topics))
                    .enqueue(new Callback<SyncKnowledgeResponse>() {
                        @Override
                        public void onResponse(Call<SyncKnowledgeResponse> call, Response<SyncKnowledgeResponse> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().success) {
                                // Then sync MCQ results
                                api.syncMcqResults(new SyncMcqRequest(results))
                                        .enqueue(new Callback<SyncMcqResponse>() {
                                            @Override
                                            public void onResponse(Call<SyncMcqResponse> call, Response<SyncMcqResponse> res) {
                                                if (callback != null) callback.onSuccess("Sync complete");
                                            }
                                            @Override
                                            public void onFailure(Call<SyncMcqResponse> call, Throwable t) {
                                                if (callback != null) callback.onFailure(t.getMessage());
                                            }
                                        });
                            } else {
                                if (callback != null) callback.onFailure("Topic sync failed");
                            }
                        }
                        @Override
                        public void onFailure(Call<SyncKnowledgeResponse> call, Throwable t) {
                            if (callback != null) callback.onFailure(t.getMessage());
                        }
                    });
        }).start();
    }

    // ── Pull cloud data DOWN and insert into local Room DB ─────────
    public static void restoreFromCloud(Context context, SyncCallback callback) {
        AppDatabase db = AppDatabase.getInstance(context);
        ApiService api = ApiClient.getClient(context).create(ApiService.class);

        api.restoreKnowledge().enqueue(new Callback<SyncKnowledgeResponse>() {
            @Override
            public void onResponse(Call<SyncKnowledgeResponse> call, Response<SyncKnowledgeResponse> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().success) {
                    if (callback != null) callback.onFailure("Restore failed");
                    return;
                }

                new Thread(() -> {
                    // Clear existing local data first, then insert cloud data
                    List<KnowledgeEntity> cloudTopics = response.body().topics;
                    if (cloudTopics != null) {
                        for (KnowledgeEntity topic : cloudTopics) {
                            topic.id = 0; // reset so Room auto-generates a new local ID
                            db.knowledgeDao().insert(topic);
                        }
                    }

                    // Now restore MCQ results
                    api.restoreMcqResults().enqueue(new Callback<SyncMcqResponse>() {
                        @Override
                        public void onResponse(Call<SyncMcqResponse> call, Response<SyncMcqResponse> res) {
                            if (res.isSuccessful() && res.body() != null) {
                                new Thread(() -> {
                                    List<McqResultEntity> cloudResults = res.body().results;
                                    // Build a map of oldLocalId → new Room ID
                                    android.util.SparseIntArray idMap = new android.util.SparseIntArray();
                                    List<KnowledgeEntity> allLocal = db.knowledgeDao().getAll();

                                    for (KnowledgeEntity local : allLocal) {
                                        for (KnowledgeEntity cloud : cloudTopics) {
                                            if (local.createdAt == cloud.createdAt) {
                                                idMap.put((int) cloud.id, local.id);
                                                break;
                                            }
                                        }
                                    }
                                    if (cloudResults != null) {
                                        for (McqResultEntity result : cloudResults) {
                                            result.id = 0;
                                            int newTopicId = idMap.get(result.topicId, -1);
                                            if (newTopicId != -1) result.topicId = newTopicId;
                                            db.mcqResultDao().insert(result);
                                        }
                                    }
                                    if (callback != null) callback.onSuccess("Restore complete");
                                }).start();
                            }
                        }
                        @Override
                        public void onFailure(Call<SyncMcqResponse> call, Throwable t) {
                            if (callback != null) callback.onFailure(t.getMessage());
                        }
                    });
                }).start();
            }
            @Override
            public void onFailure(Call<SyncKnowledgeResponse> call, Throwable t) {
                if (callback != null) callback.onFailure(t.getMessage());
            }
        });
    }
}