package com.example.learnkeep;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

public class McqResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mcq_result);

        Intent in = getIntent();
        int topicId  = in.getIntExtra("topic_id", -1);
        int score = in.getIntExtra("score",    0);
        int total = in.getIntExtra("total",    5);
        int confBefore = in.getIntExtra("conf_before", 5);
        int confAfter = in.getIntExtra("conf_after",  5);
        String trigger = in.getStringExtra("trigger");

        // Bind views
        TextView txtScore = findViewById(R.id.txtScore);
        TextView txtScoreLabel = findViewById(R.id.txtScoreLabel);
        TextView txtConfChange = findViewById(R.id.txtConfChange);
        TextView txtNextReminder = findViewById(R.id.txtNextReminder);
        LinearLayout layoutReview = findViewById(R.id.layoutReview);
        Button btnComplete = findViewById(R.id.btnMarkComplete);
        Button btnBack = findViewById(R.id.btnBack);

        // Score
        txtScore.setText(score + "/" + total);
        float pct = (float) score / total;
        String emoji;
        String scoreColor;
        if(pct >= 0.8f){
            emoji = "🎉 Excellent!";   scoreColor = "#22C55E";
        } else if (pct >= 0.6f){
            emoji = "👍 Good job!";    scoreColor = "#84CC16";
        } else if (pct >= 0.4f){
            emoji = "📖 Keep going!";  scoreColor = "#F59E0B";
        } else {
            emoji = "💪 Need more practice!"; scoreColor = "#EF4444";
        }
        txtScoreLabel.setText(emoji);

        try {
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(Color.parseColor(scoreColor + "22"));
            circle.setStroke(8, Color.parseColor(scoreColor));
            findViewById(R.id.scoreCircle).setBackground(circle);
        } catch (Exception ignored) {}

        // Confidence change
        String arrow = confAfter > confBefore ? "⬆" : confAfter < confBefore ? "⬇" : "→";
        String confColor = confAfter > confBefore ? "#22C55E" : confAfter < confBefore ? "#EF4444" : "#6B7280";
        txtConfChange.setText("Confidence: " + confBefore + " " + arrow + " " + confAfter + "/10");
        txtConfChange.setTextColor(Color.parseColor(confColor));

        // Next reminder schedule
        int days;
        if      (confAfter <= 3) days = 1;
        else if (confAfter <= 6) days = 3;
        else if (confAfter <= 8) days = 7;
        else                     days = 14;

        // Check if topic is completed
        KnowledgeEntity entity = null;
        if (topicId != -1) {
            entity = AppDatabase.getInstance(this).knowledgeDao().getById(topicId);
        }
        if (entity != null && entity.isCompleted) {
            txtNextReminder.setText("✅ Topic marked complete — no more reminders!");
        } else {
            txtNextReminder.setText("⏰ Next reminder in " + days + " day" + (days > 1 ? "s" : ""));
        }

        // Build review cards
        buildReview(layoutReview, in);
        // Mark complete button
        KnowledgeEntity finalEntity = entity;
        btnComplete.setOnClickListener(v -> {
            if (finalEntity == null) return;
            finalEntity.isCompleted = true;
            AppDatabase.getInstance(this).knowledgeDao().update(finalEntity);
            ReminderScheduler.cancelReminder(this, finalEntity.id);
            txtNextReminder.setText("✅ Topic marked complete — no more reminders!");
            btnComplete.setEnabled(false);
            btnComplete.setText("✅ Marked Complete");
            Toast.makeText(this, "Topic completed! No more reminders.", Toast.LENGTH_SHORT).show();
        });

        if (finalEntity != null && finalEntity.isCompleted) {
            btnComplete.setEnabled(false);
            btnComplete.setText("✅ Already Completed");
        }

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });
    }

    private void buildReview(LinearLayout container, Intent in) {
        String json = in.getStringExtra("questions_json");
        ArrayList<Integer> answers = in.getIntegerArrayListExtra("user_answers");
        if (json == null || answers == null) return;

        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject q  = arr.getJSONObject(i);
                int correct   = q.getInt("correctIndex");
                int chosen    = i < answers.size() ? answers.get(i) : -1;
                JSONArray opts = q.getJSONArray("options");
                boolean right = chosen == correct;

                // Card wrapper
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(16), dp(14), dp(16), dp(14));
                card.setBackground(roundedRect(right ? "#F0FDF4" : "#FFF1F2",
                        right ? "#22C55E" : "#EF4444"));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, dp(12));
                card.setLayoutParams(lp);

                // Q number + result
                TextView tvHead = new TextView(this);
                tvHead.setText("Q" + (i+1) + "  " + (right ? "✅ Correct" : "❌ Wrong"));
                tvHead.setTextColor(Color.parseColor(right ? "#065F46" : "#991B1B"));
                tvHead.setTextSize(13f);
                card.addView(tvHead);

                // Question text
                TextView tvQ = new TextView(this);
                tvQ.setText(q.getString("question"));
                tvQ.setTextSize(15f);
                tvQ.setTextColor(Color.parseColor("#111827"));
                tvQ.setPadding(0, dp(6), 0, dp(4));
                card.addView(tvQ);

                // Options
                for (int j = 0; j < opts.length(); j++) {
                    TextView tvOpt = new TextView(this);
                    String prefix = (j == correct) ? "✔ " : (j == chosen && !right) ? "✘ " : "○ ";
                    tvOpt.setText(prefix + opts.getString(j));
                    int color;
                    if (j == correct) color = Color.parseColor("#065F46");
                    else if (j == chosen && !right) color = Color.parseColor("#991B1B");
                    else color = Color.parseColor("#6B7280");
                    tvOpt.setTextColor(color);
                    tvOpt.setTextSize(14f);
                    tvOpt.setPadding(dp(8), dp(3), 0, dp(3));
                    card.addView(tvOpt);
                }

                // Explanation
                String expl = q.optString("explanation", "");
                if (!expl.isEmpty()) {
                    TextView tvEx = new TextView(this);
                    tvEx.setText("💡 " + expl);
                    tvEx.setTextSize(13f);
                    tvEx.setTextColor(Color.parseColor("#374151"));
                    tvEx.setPadding(0, dp(8), 0, 0);
                    card.addView(tvEx);
                }

                container.addView(card);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private GradientDrawable roundedRect(String bg, String stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(14));
        d.setColor(Color.parseColor(bg));
        d.setStroke(dp(1), Color.parseColor(stroke));
        return d;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
