package com.example.learnkeep;

import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.*;

import java.util.*;

public class StatsFragment extends Fragment {

    TextView txtStreak, txtRetention, txtTopicsCount, txtReviewsDue;
    TextView txtStrongSubjects, txtWeakSubjects;
    TextView txtAvgScore, txtTestsTaken;
    LineChart chart;
    RecyclerView recyclerReview;
    ArrayList<ReviewItem> reviewList = new ArrayList<>();

    public StatsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle s) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        txtStreak = view.findViewById(R.id.txtStreak);
        txtRetention = view.findViewById(R.id.txtRetention);
        txtTopicsCount = view.findViewById(R.id.txtTopicsCount);
        txtReviewsDue = view.findViewById(R.id.txtReviewsDue);
        txtStrongSubjects = view.findViewById(R.id.txtStrongSubjects);
        txtWeakSubjects = view.findViewById(R.id.txtWeakSubjects);
        chart = view.findViewById(R.id.chart);
        recyclerReview = view.findViewById(R.id.recyclerReview);
        recyclerReview.setLayoutManager(new LinearLayoutManager(getContext()));

        // Optional new views — only bind if present in layout
        txtAvgScore  = view.findViewById(R.id.txtAvgScore);
        txtTestsTaken = view.findViewById(R.id.txtTestsTaken);

        chart.setNoDataText("No study data yet");
        loadStats();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStats();
    }

    private void loadStats() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<KnowledgeEntity> topics = db.knowledgeDao().getAll();

            int totalTests  = db.mcqResultDao().getTotalTests();
            float accuracy  = db.mcqResultDao().getOverallAccuracy(); // 0.0–1.0
            int accPct      = Math.round(accuracy * 100);

            requireActivity().runOnUiThread(() -> {
                // Topics count (exclude completed)
                int active = 0;
                for (KnowledgeEntity t : topics) if (!t.isCompleted) active++;
                txtTopicsCount.setText(active + "/" + topics.size());

                // Reviews due (conf ≤ 3 AND not completed)
                int reviewsDue = 0;
                for (KnowledgeEntity t : topics) {
                    try {
                        if (!t.isCompleted && Integer.parseInt(t.confidence) <= 3) reviewsDue++;
                    } catch (Exception ignored) {}
                }
                txtReviewsDue.setText(String.valueOf(reviewsDue));

                // MCQ stats
                if (txtTestsTaken != null) txtTestsTaken.setText(String.valueOf(totalTests));
                if (txtAvgScore   != null) txtAvgScore.setText(accPct + "%");

                // Review schedule list
                reviewList.clear();
                long now = System.currentTimeMillis();
                for (KnowledgeEntity t : topics) {
                    if (t.isCompleted) continue;
                    int conf = 5;
                    try { conf = Integer.parseInt(t.confidence); } catch (Exception ignored) {}
                    int days;
                    if      (conf <= 3) days = 1;
                    else if (conf <= 6) days = 3;
                    else if (conf <= 8) days = 7;
                    else                days = 14;
                    long reviewTime = t.createdAt + (days * 86_400_000L);
                    long diff = reviewTime - now;
                    String day; int priority;
                    if (diff < 0)              { day = "Overdue";   priority = 0; }
                    else if (diff <= 86400000) { day = "Today";     priority = 1; }
                    else if (diff <= 172800000){ day = "Tomorrow";  priority = 2; }
                    else                       { day = "Upcoming";  priority = 3; }
                    reviewList.add(new ReviewItem(t.title, day, t.tags, priority));
                }
                reviewList.sort((a, b) -> a.priority - b.priority);
                recyclerReview.setAdapter(new ReviewAdapter(reviewList));

                calculateRetention(topics);
                calculateStreak(topics);
                analyzeSubjects(topics);
                loadChart(topics);
            });
        }).start();
    }

    private void calculateRetention(List<KnowledgeEntity> topics) {
        if (topics == null || topics.isEmpty()) { txtRetention.setText("0%"); return; }
        int sum = 0, count = 0;
        for (KnowledgeEntity t : topics) {
            try { sum += Integer.parseInt(t.confidence); count++; } catch (Exception ignored) {}
        }
        int avg = count > 0 ? sum / count : 0;
        txtRetention.setText((avg * 10) + "%");
    }

    private void calculateStreak(List<KnowledgeEntity> topics) {
        int streak = Math.min(topics.size(), 7);
        txtStreak.setText(streak + " Days");
    }

    private void analyzeSubjects(List<KnowledgeEntity> topics) {
        Map<String, Integer> scoreMap = new HashMap<>(), countMap = new HashMap<>();
        for (KnowledgeEntity t : topics) {
            if (t.tags == null) continue;
            int conf = 0;
            try { conf = Integer.parseInt(t.confidence); } catch (Exception ignored) {}
            scoreMap.put(t.tags, scoreMap.getOrDefault(t.tags, 0) + conf);
            countMap.put(t.tags, countMap.getOrDefault(t.tags, 0) + 1);
        }
        StringBuilder strong = new StringBuilder(), weak = new StringBuilder();
        for (String tag : scoreMap.keySet()) {
            int avg = scoreMap.get(tag) / countMap.get(tag);
            if (avg >= 7) strong.append(tag).append("\n");
            if (avg <= 3) weak.append(tag).append("\n");
        }
        txtStrongSubjects.setText(strong.length() == 0 ? "None" : strong.toString().trim());
        txtWeakSubjects.setText(weak.length() == 0 ? "None" : weak.toString().trim());
    }

    private void loadChart(List<KnowledgeEntity> topics) {
        List<Entry> entries = new ArrayList<>();
        int idx = 0;
        for (KnowledgeEntity t : topics) {
            try { entries.add(new Entry(idx++, Float.parseFloat(t.confidence))); }
            catch (Exception ignored) {}
        }
        LineDataSet ds = new LineDataSet(entries, "Confidence");
        ds.setLineWidth(3f);
        ds.setCircleRadius(4f);
        ds.setColor(Color.parseColor("#7F13EC"));
        ds.setCircleColor(Color.parseColor("#7F13EC"));
        ds.setValueTextSize(10f);
        ds.setFillColor(Color.parseColor("#F5EAFA"));
        ds.setDrawFilled(true);
        chart.setData(new LineData(ds));
        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.animateX(1200);
        chart.invalidate();
    }
}
