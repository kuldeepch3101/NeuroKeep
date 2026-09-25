package com.example.learnkeep;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import java.util.*;

public class PracticeFragment extends Fragment {

    public PracticeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle s) {
        View view = inflater.inflate(R.layout.fragment_practice, container, false);

        RecyclerView rv = view.findViewById(R.id.recyclerPractice);
        TextView txtEmpty = view.findViewById(R.id.txtEmpty);
        TextView txtTotal = view.findViewById(R.id.txtTotalTopics);
        TextView txtDone = view.findViewById(R.id.txtCompletedCount);
        TextView txtTests = view.findViewById(R.id.txtTestsTaken);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        new Thread(() -> {
            List<KnowledgeEntity> all = AppDatabase.getInstance(requireContext())
                    .knowledgeDao().getAll();
            int completed = 0, testsTaken = 0;
            for (KnowledgeEntity e : all) {
                if (e.isCompleted) completed++;
                if (e.lastTestScore >= 0) testsTaken++;
            }

            List<KnowledgeEntity> active = new ArrayList<>();
            for (KnowledgeEntity e : all) if (!e.isCompleted) active.add(e);

            int finalCompleted = completed, finalTests = testsTaken;
            requireActivity().runOnUiThread(() -> {
                txtTotal.setText(String.valueOf(all.size()));
                txtDone.setText(String.valueOf(finalCompleted));
                txtTests.setText(String.valueOf(finalTests));

                if (active.isEmpty()) {
                    txtEmpty.setVisibility(View.VISIBLE);
                    rv.setVisibility(View.GONE);
                } else {
                    txtEmpty.setVisibility(View.GONE);
                    rv.setVisibility(View.VISIBLE);
                    rv.setAdapter(new PracticeAdapter(active));
                }
            });
        }).start();
        return view;
    }

    //Inner adapter

    static class PracticeAdapter extends RecyclerView.Adapter<PracticeAdapter.VH> {
        final List<KnowledgeEntity> list;
        PracticeAdapter(List<KnowledgeEntity> l) { list = l; }

        @Override
        public VH onCreateViewHolder(ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_practice_topic, p, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            KnowledgeEntity e = list.get(pos);
            h.txtTitle.setText(e.title);

            int conf = 5;
            try { conf = Integer.parseInt(e.confidence); } catch (Exception ignored) {}
            h.txtConf.setText("Confidence: " + conf + "/10");
            if (e.lastTestScore >= 0)
                h.txtLastScore.setText("Last: " + e.lastTestScore + "/" + e.lastTestTotal + " correct");
            else
                h.txtLastScore.setText("No quiz taken yet");

            int finalConf = conf;
            h.btnQuiz.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), McqTestActivity.class);
                intent.putExtra(McqTestActivity.EXTRA_TOPIC_ID, e.id);
                intent.putExtra(McqTestActivity.EXTRA_TRIGGER, "MANUAL");
                v.getContext().startActivity(intent);
            });
        }

        @Override public int getItemCount() { return list.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView txtTitle, txtConf, txtLastScore;
            Button   btnQuiz;
            VH(View v) {
                super(v);
                txtTitle = v.findViewById(R.id.txtPracticeTitle);
                txtConf = v.findViewById(R.id.txtPracticeConf);
                txtLastScore = v.findViewById(R.id.txtPracticeLastScore);
                btnQuiz = v.findViewById(R.id.btnPracticeQuiz);
            }
        }
    }
}
