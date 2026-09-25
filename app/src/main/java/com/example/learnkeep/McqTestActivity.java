package com.example.learnkeep;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class McqTestActivity extends AppCompatActivity {

    // ── intent extras ─────────────────────────────────────────────────
    public static final String EXTRA_TOPIC_ID = "topic_id";
    public static final String EXTRA_TRIGGER  = "trigger";

    // ── state ─────────────────────────────────────────────────────────
    private KnowledgeEntity entity;
    private McqQuestion[] questions;
    private int currentIndex= 0;
    private int correctCount= 0;
    private int selectedConfidence;
    private int[] userAnswers;  // stores chosen index per question
    private String triggerType= "MANUAL";

    // ── confidence‑step views ─────────────────────────────────────────
    private LinearLayout layoutConfidence;
    private LinearLayout confidenceContainer;
    private TextView txtConfidenceValue;
    private Button btnStartTest;

    // ── loading view ──────────────────────────────────────────────────
    private LinearLayout layoutLoading;
    private TextView txtLoadingStatus;

    // ── question views ────────────────────────────────────────────────
    private LinearLayout layoutQuestion;
    private TextView txtTopicTitle;
    private TextView txtProgress;
    private TextView txtQuestion;
    private LinearLayout layoutOptions;
    private Button btnNext;
    private TextView txtExplanation;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mcq_test);

        // Extras
        int topicId = getIntent().getIntExtra(EXTRA_TOPIC_ID, -1);
        triggerType = getIntent().getStringExtra(EXTRA_TRIGGER);
        if (triggerType == null) triggerType = "MANUAL";

        if (topicId == -1) { finish(); return; }

        entity = AppDatabase.getInstance(this).knowledgeDao().getById(topicId);
        if (entity == null) { finish(); return; }

        // If topic is completed, skip test
        if (entity.isCompleted) {
            Toast.makeText(this, "Topic already completed! No reminders needed.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Parse current confidence
        try { selectedConfidence = Integer.parseInt(entity.confidence); }
        catch (Exception e) { selectedConfidence = 5; }

        // Bind views
        layoutConfidence = findViewById(R.id.layoutConfidence);
        confidenceContainer = findViewById(R.id.confidenceContainer);
        txtConfidenceValue  = findViewById(R.id.txtConfidenceValue);
        btnStartTest = findViewById(R.id.btnStartTest);

        layoutLoading = findViewById(R.id.layoutLoading);
        txtLoadingStatus = findViewById(R.id.txtLoadingStatus);

        layoutQuestion = findViewById(R.id.layoutQuestion);
        txtTopicTitle = findViewById(R.id.txtTopicTitle);
        txtProgress = findViewById(R.id.txtProgress);
        txtQuestion = findViewById(R.id.txtQuestion);
        layoutOptions = findViewById(R.id.layoutOptions);
        btnNext = findViewById(R.id.btnNext);
        txtExplanation = findViewById(R.id.txtExplanation);
        progressBar = findViewById(R.id.progressBar);

        txtTopicTitle.setText(entity.title);

        // Step 1 — show confidence selector
        showConfidenceStep();

        btnStartTest.setOnClickListener(v -> {
            entity.confidence = String.valueOf(selectedConfidence);
            AppDatabase.getInstance(this).knowledgeDao().update(entity);
            generateTest();
        });

        btnNext.setOnClickListener(v -> nextQuestion());
    }

    private void showConfidenceStep() {
        layoutConfidence.setVisibility(View.VISIBLE);
        layoutLoading.setVisibility(View.GONE);
        layoutQuestion.setVisibility(View.GONE);
        buildConfidenceSelector(selectedConfidence);
    }

    private void buildConfidenceSelector(int preselected) {
        confidenceContainer.removeAllViews();
        for (int i = 1; i <= 10; i++) {
            TextView tv = new TextView(this);
            tv.setText(String.valueOf(i));
            tv.setTextColor(Color.WHITE);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextSize(14f);
            int size = dp(40);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(dp(5), dp(5), dp(5), dp(5));
            tv.setLayoutParams(lp);
            tv.setBackground(confidenceDrawable(i, i == preselected));
            int val = i;
            tv.setOnClickListener(v -> {
                selectedConfidence = val;
                buildConfidenceSelector(val);
            });
            confidenceContainer.addView(tv);
        }
        String label;
        if (selectedConfidence <= 3)      label = "Low — I'm shaky on this";
        else if (selectedConfidence <= 6) label = "Medium — I know some";
        else if (selectedConfidence <= 8) label = "Good — I know most";
        else                              label = "High — I've mastered it";
        txtConfidenceValue.setText(selectedConfidence + "/10  •  " + label);
    }

    private GradientDrawable confidenceDrawable(int v, boolean selected) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        if (v <= 3)      d.setColor(Color.parseColor("#EF4444"));
        else if (v <= 6) d.setColor(Color.parseColor("#F59E0B"));
        else if (v <= 8) d.setColor(Color.parseColor("#84CC16"));
        else             d.setColor(Color.parseColor("#22C55E"));
        d.setStroke(selected ? 5 : 2,
                selected ? Color.parseColor("#1E1B4B") : Color.parseColor("#E5E7EB"));
        return d;
    }

    private void generateTest() {
        layoutConfidence.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.VISIBLE);
        layoutQuestion.setVisibility(View.GONE);
        txtLoadingStatus.setText("🤖 Gemini is crafting your quiz…");

        GeminiService.generateMcqTest(
                McqTestActivity.this,
                entity.title,
                entity.notes,
                selectedConfidence,
                new GeminiService.McqCallback() {
                    @Override public void onSuccess(McqQuestion[] qs) {
                        questions   = qs;
                        userAnswers = new int[qs.length];
                        java.util.Arrays.fill(userAnswers, -1);
                        currentIndex = 0;
                        correctCount = 0;
                        showQuestion();
                    }
                    @Override public void onError(String error) {
                        layoutLoading.setVisibility(View.GONE);
                        layoutConfidence.setVisibility(View.VISIBLE);
                        Toast.makeText(McqTestActivity.this,
                                "Failed to generate quiz: " + error, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }


    private void showQuestion() {
        layoutLoading.setVisibility(View.GONE);
        layoutConfidence.setVisibility(View.GONE);
        layoutQuestion.setVisibility(View.VISIBLE);

        McqQuestion q = questions[currentIndex];
        txtProgress.setText("Question " + (currentIndex + 1) + " of " + questions.length);
        progressBar.setMax(questions.length);
        progressBar.setProgress(currentIndex + 1);
        txtQuestion.setText(q.question);
        txtExplanation.setVisibility(View.GONE);
        btnNext.setVisibility(View.GONE);

        layoutOptions.removeAllViews();

        for (int i = 0; i < q.options.length; i++) {
            final int idx = i;
            TextView opt = new TextView(this);
            opt.setText(q.options[i]);
            opt.setTextSize(15f);
            opt.setPadding(dp(16), dp(14), dp(16), dp(14));
            opt.setBackground(optionDrawable("#FFFFFF", "#D1D5DB"));
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(10));
            opt.setLayoutParams(lp);

            opt.setOnClickListener(v -> onOptionSelected(idx, q));
            layoutOptions.addView(opt);
        }
    }

    private void onOptionSelected(int chosen, McqQuestion q) {
        if (userAnswers[currentIndex] != -1) return; // already answered

        userAnswers[currentIndex] = chosen;
        boolean correct = (chosen == q.correctIndex);
        if (correct) correctCount++;

        // Colour all options
        for (int i = 0; i < layoutOptions.getChildCount(); i++) {
            TextView tv = (TextView) layoutOptions.getChildAt(i);
            if (i == q.correctIndex) {
                tv.setBackground(optionDrawable("#D1FAE5", "#22C55E"));
                tv.setTextColor(Color.parseColor("#065F46"));
            } else if (i == chosen && !correct) {
                tv.setBackground(optionDrawable("#FEE2E2", "#EF4444"));
                tv.setTextColor(Color.parseColor("#991B1B"));
            }
            tv.setEnabled(false);
        }

        // Show explanation
        if (!q.explanation.isEmpty()) {
            txtExplanation.setText("💡 " + q.explanation);
            txtExplanation.setVisibility(View.VISIBLE);
        }

        btnNext.setText(currentIndex == questions.length - 1 ? "See Results" : "Next →");
        btnNext.setVisibility(View.VISIBLE);
    }

    private void nextQuestion() {
        currentIndex++;
        if (currentIndex < questions.length) {
            showQuestion();
        } else {
            finishTest();
        }
    }

    private void finishTest() {
        int total = questions.length;

        // Compute new confidence from score (score/total mapped to 1-10)
        float ratio = (float) correctCount / total;
        int newConfidence;
        if      (ratio >= 0.9f) newConfidence = 10;
        else if (ratio >= 0.8f) newConfidence = 9;
        else if (ratio >= 0.7f) newConfidence = 8;
        else if (ratio >= 0.6f) newConfidence = 7;
        else if (ratio >= 0.5f) newConfidence = 6;
        else if (ratio >= 0.4f) newConfidence = 5;
        else if (ratio >= 0.3f) newConfidence = 4;
        else if (ratio >= 0.2f) newConfidence = 3;
        else if (ratio >= 0.1f) newConfidence = 2;
        else                    newConfidence = 1;

        // Persist McqResultEntity in separate table
        McqResultEntity result = new McqResultEntity();
        result.topicId          = entity.id;
        result.topicTitle       = entity.title;
        result.score            = correctCount;
        result.total            = total;
        result.confidenceBefore = selectedConfidence;
        result.confidenceAfter  = newConfidence;
        result.takenAt          = System.currentTimeMillis();
        result.triggerType      = triggerType;

        AppDatabase db = AppDatabase.getInstance(this);
        db.mcqResultDao().insert(result);
        // After db.mcqResultDao().insert(result);
        SyncManager.syncToCloud(this, new SyncManager.SyncCallback() {
            @Override public void onSuccess(String msg) { Log.d("SYNC", "MCQ backed up"); }
            @Override public void onFailure(String err) { Log.e("SYNC", "MCQ backup failed: " + err); }
        });

        // Update KnowledgeEntity with latest score & new confidence
        entity.confidence    = String.valueOf(newConfidence);
        entity.lastTestScore = correctCount;
        entity.lastTestTotal = total;
        entity.lastTestAt    = System.currentTimeMillis();
        entity.reviewCount   = entity.reviewCount + 1;
        db.knowledgeDao().update(entity);

        // Schedule next reminder (only if not completed)
        if (!entity.isCompleted) {
            ReminderScheduler.scheduleReminder(this, entity.id, entity.title, newConfidence);
        }

        // Launch result screen
        Intent intent = new Intent(this, McqResultActivity.class);
        intent.putExtra("topic_id",     entity.id);
        intent.putExtra("score",        correctCount);
        intent.putExtra("total",        total);
        intent.putExtra("conf_before",  selectedConfidence);
        intent.putExtra("conf_after",   newConfidence);
        intent.putExtra("trigger",      triggerType);
        // Pass questions + user answers for review
        intent.putExtra("questions_json", buildQuestionsJson());
        intent.putIntegerArrayListExtra("user_answers", toArrayList(userAnswers));
        startActivity(intent);
        finish();
    }

    // ── helpers ───────────────────────────────────────────────────────

    private String buildQuestionsJson() {
        try {
            org.json.JSONArray arr = new org.json.JSONArray();
            for (McqQuestion q : questions) {
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("question", q.question);
                o.put("correctIndex", q.correctIndex);
                o.put("explanation", q.explanation);
                org.json.JSONArray opts = new org.json.JSONArray();
                for (String s : q.options) opts.put(s);
                o.put("options", opts);
                arr.put(o);
            }
            return arr.toString();
        } catch (Exception e) { return "[]"; }
    }

    private ArrayList<Integer> toArrayList(int[] arr) {
        ArrayList<Integer> list = new ArrayList<>();
        for (int v : arr) list.add(v);
        return list;
    }

    private GradientDrawable optionDrawable(String bg, String stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(12));
        d.setColor(Color.parseColor(bg));
        d.setStroke(dp(1), Color.parseColor(stroke));
        return d;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
