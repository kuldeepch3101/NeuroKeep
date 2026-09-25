package com.example.learnkeep;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class AiChatBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView recyclerView;
    private EditText etMessage;
    private ImageButton btnSend;
    private ProgressBar progressBar;
    private static List<String> messages = new ArrayList<>();
    private static List<Boolean> isUserList = new ArrayList<>();
    private int typingPosition = -1;
    private Handler typingHandler = new Handler();
    private int dotCount = 0;
    private boolean isTypingAnimating = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_ai_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView = view.findViewById(R.id.chatRecyclerView);
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btnSend);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        recyclerView.setAdapter(new RecyclerView.Adapter<>() {

            @Override
            public int getItemCount() {
                return messages.size();
            }

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                LinearLayout container = new LinearLayout(parent.getContext());
                container.setLayoutParams(new RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        RecyclerView.LayoutParams.WRAP_CONTENT
                ));
                container.setPadding(dp(6), dp(4), dp(6), dp(4));

                TextView tv = new TextView(parent.getContext());
                tv.setPadding(dp(16), dp(12), dp(16), dp(12));
                tv.setTextSize(14f);
                tv.setMaxWidth(dp(260));

                container.addView(tv);

                return new RecyclerView.ViewHolder(container) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

                LinearLayout container = (LinearLayout) holder.itemView;
                TextView tv = (TextView) container.getChildAt(0);

                String message = messages.get(position);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

                params.setMargins(dp(4), dp(2), dp(4), dp(2));

                if (isUserList.get(position)) {
                    container.setGravity(android.view.Gravity.END);
                    tv.setBackgroundResource(R.drawable.bg_chat_user);
                    tv.setTextColor(0xFFFFFFFF);
                    params.setMarginStart(dp(60));
                } else {
                    container.setGravity(android.view.Gravity.START);
                    tv.setBackgroundResource(R.drawable.bg_chat_ai);
                    tv.setTextColor(0xFF1E1924);
                    params.setMarginEnd(dp(60));
                }

                tv.setLayoutParams(params);

                // Typing animation
                if (position == typingPosition && isTypingAnimating) {
                    tv.setText("AI is typing" + getDots());
                } else {
                    tv.setText(message);
                }
            }
        });

        if (messages.isEmpty()) {
            messages.add("👋 Hi! I'm your LearnKeep AI. Ask me anything!");
            isUserList.add(false);
        }

        btnSend.setOnClickListener(v -> sendMessage());

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void sendMessage() {
        String msg = etMessage.getText().toString().trim();
        if (msg.isEmpty()) return;

        etMessage.setText("");

        messages.add(msg);
        isUserList.add(true);

        recyclerView.getAdapter().notifyItemInserted(messages.size() - 1);
        recyclerView.scrollToPosition(messages.size() - 1);

        setLoading(true);

        // Typing bubble
        messages.add("");
        isUserList.add(false);
        typingPosition = messages.size() - 1;

        isTypingAnimating = true;
        startTypingAnimation();

        recyclerView.getAdapter().notifyItemInserted(typingPosition);

        new Thread(() -> {
            List<KnowledgeEntity> topics = AppDatabase
                    .getInstance(requireContext())
                    .knowledgeDao()
                    .getAll();

            GeminiService.chat(requireContext(), msg, topics, new GeminiService.ChatCallback() {
                @Override
                public void onSuccess(String reply) {
                    stopTypingAnimation();
                    setLoading(false);
                    replaceTypingMessage(reply);
                }

                @Override
                public void onError(String error) {
                    stopTypingAnimation();
                    setLoading(false);
                    replaceTypingMessage("⚠️ Error connecting to AI");
                }
            });
        }).start();
    }

    private void startTypingAnimation() {
        typingHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isTypingAnimating) return;

                dotCount = (dotCount + 1) % 4;

                if (typingPosition != -1) {
                    recyclerView.getAdapter().notifyItemChanged(typingPosition);
                }

                typingHandler.postDelayed(this, 500);
            }
        }, 500);
    }

    private void stopTypingAnimation() {
        isTypingAnimating = false;
        dotCount = 0;
    }

    private String getDots() {
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < dotCount; i++) dots.append(".");
        return dots.toString();
    }

    private void replaceTypingMessage(String text) {
        requireActivity().runOnUiThread(() -> {
            messages.set(typingPosition, text);
            recyclerView.getAdapter().notifyItemChanged(typingPosition);
            typingPosition = -1;
            recyclerView.scrollToPosition(messages.size() - 1);
        });
    }

    private void setLoading(boolean loading) {
        requireActivity().runOnUiThread(() ->
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}