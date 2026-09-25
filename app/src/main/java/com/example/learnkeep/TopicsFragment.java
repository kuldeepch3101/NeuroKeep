package com.example.learnkeep;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import java.util.*;

public class TopicsFragment extends Fragment {

    private RecyclerView recyclerTopics;
    private TopicAdapter adapter;
    private List<KnowledgeEntity> fullList = new ArrayList<>();

    public TopicsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle s) {
        View view = inflater.inflate(R.layout.fragment_topics, container, false);

        recyclerTopics = view.findViewById(R.id.recyclerTopics);
        recyclerTopics.setLayoutManager(new LinearLayoutManager(getContext()));

        EditText  searchBox = view.findViewById(R.id.searchBox);
        ImageView btnClear  = view.findViewById(R.id.btnClearSearch);

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                filterTopics(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        btnClear.setOnClickListener(v -> searchBox.setText(""));

        loadTopics();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTopics();
    }

    private void loadTopics() {
        new Thread(() -> {
            List<KnowledgeEntity> list =
                    AppDatabase.getInstance(requireContext()).knowledgeDao().getAll();
            requireActivity().runOnUiThread(() -> {
                fullList.clear();
                if (list != null) fullList.addAll(list);
                if (adapter == null) {
                    adapter = new TopicAdapter(fullList);
                    recyclerTopics.setAdapter(adapter);
                } else {
                    adapter.notifyDataSetChanged();
                }
            });
        }).start();
    }

    private void filterTopics(String query) {
        if (adapter == null) return;
        List<KnowledgeEntity> filtered = new ArrayList<>();
        for (KnowledgeEntity e : fullList) {
            if (e.title.toLowerCase().contains(query.toLowerCase())
                    || (e.tags != null && e.tags.toLowerCase().contains(query.toLowerCase())))
                filtered.add(e);
        }
        adapter = new TopicAdapter(filtered);
        recyclerTopics.setAdapter(adapter);
    }
}