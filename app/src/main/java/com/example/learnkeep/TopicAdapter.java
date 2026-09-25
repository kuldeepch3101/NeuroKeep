package com.example.learnkeep;

import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TopicAdapter extends RecyclerView.Adapter<TopicAdapter.ViewHolder> {

    private final List<KnowledgeEntity> list;

    public TopicAdapter(List<KnowledgeEntity> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_topic, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        KnowledgeEntity item = list.get(position);

        h.txtTitle.setText(item.title);
        h.imgTopic.setImageResource(TopicIconHelper.getIconFromTags(item.tags));

        String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(new Date(item.createdAt));
        h.txtDate.setText("Added " + date);

        // ── Confidence badge ─────────────────────────────────────────
        final int conf;
        int parsedConf = 5;
        try { parsedConf = Integer.parseInt(item.confidence); } catch (Exception ignored) {}
        conf = parsedConf;

        String confLabel;
        int confColor;
        if      (conf <= 3) { confLabel = "Low "  + conf + "/10"; confColor = 0xFFEF4444; }
        else if (conf <= 6) { confLabel = "Mid "  + conf + "/10"; confColor = 0xFFF59E0B; }
        else if (conf <= 8) { confLabel = "Good " + conf + "/10"; confColor = 0xFF84CC16; }
        else                { confLabel = "High " + conf + "/10"; confColor = 0xFF22C55E; }
        h.txtConfBadge.setText(confLabel);
        h.txtConfBadge.setTextColor(confColor);

        // ── Last MCQ score ───────────────────────────────────────────
        if (item.lastTestScore >= 0) {
            h.txtLastScore.setVisibility(View.VISIBLE);
            h.txtLastScore.setText("Last quiz: " + item.lastTestScore + "/" + item.lastTestTotal);
        } else {
            h.txtLastScore.setVisibility(View.GONE);
        }

        // ── Completed state ──────────────────────────────────────────
        if (item.isCompleted) {
            h.txtTitle.setPaintFlags(h.txtTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            h.txtTitle.setTextColor(0xFF9CA3AF);
            h.badgeCompleted.setVisibility(View.VISIBLE);
        } else {
            h.txtTitle.setPaintFlags(h.txtTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            h.txtTitle.setTextColor(0xFF111827);
            h.badgeCompleted.setVisibility(View.GONE);
        }

        // ── Toggle complete checkbox ─────────────────────────────────
        h.checkComplete.setOnCheckedChangeListener(null); // clear first to avoid trigger loop
        h.checkComplete.setChecked(item.isCompleted);
        h.checkComplete.setOnCheckedChangeListener((btn, checked) -> {
            item.isCompleted = checked;
            AppDatabase.getInstance(h.itemView.getContext()).knowledgeDao().update(item);
            if (checked) {
                // conf is effectively final — safe in lambda
                ReminderScheduler.cancelReminder(h.itemView.getContext(), item.id);
                Toast.makeText(h.itemView.getContext(),
                        "✅ \"" + item.title + "\" marked complete. No more reminders!",
                        Toast.LENGTH_SHORT).show();
            } else {
                ReminderScheduler.scheduleReminder(
                        h.itemView.getContext(), item.id, item.title, conf);
            }
            notifyItemChanged(position);
        });

        // ── YouTube link ─────────────────────────────────────────────
        if (item.youtubeLinks != null && !item.youtubeLinks.isEmpty()) {
            h.txtYoutubePreview.setVisibility(View.VISIBLE);
            h.txtYoutubePreview.setOnClickListener(v -> {
                String url = item.youtubeLinks;
                if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://" + url;
                v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            });
        } else {
            h.txtYoutubePreview.setVisibility(View.GONE);
        }

        // ── Item click → TopicDetails ─────────────────────────────────
        h.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), TopicDetailsActivity.class);
            intent.putExtra("topic_id", item.id);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView  txtTitle, txtDate, txtYoutubePreview, txtConfBadge, txtLastScore, badgeCompleted;
        ImageView imgTopic;
        CheckBox  checkComplete;

        ViewHolder(View v) {
            super(v);
            txtTitle = v.findViewById(R.id.txtTitle);
            txtDate = v.findViewById(R.id.txtDate);
            imgTopic = v.findViewById(R.id.imgTopic);
            txtYoutubePreview = v.findViewById(R.id.txtYoutubePreview);
            txtConfBadge = v.findViewById(R.id.txtConfBadge);
            txtLastScore = v.findViewById(R.id.txtLastScore);
            checkComplete = v.findViewById(R.id.checkComplete);
            badgeCompleted = v.findViewById(R.id.badgeCompleted);
        }
    }
}
