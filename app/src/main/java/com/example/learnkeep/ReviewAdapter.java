package com.example.learnkeep;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {
    List<ReviewItem> list;
    public ReviewAdapter(List<ReviewItem> list) {
        this.list = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle,txtDay;
        ImageView iconSubject;
        public ViewHolder(View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTopicTitle);
            txtDay = itemView.findViewById(R.id.txtReviewDay);
            iconSubject = itemView.findViewById(R.id.iconSubject);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ReviewItem item = list.get(position);
        holder.txtTitle.setText(item.title);
        holder.txtDay.setText(item.day);
        int icon = TopicIconHelper.getIconFromTags(item.tags);
        holder.iconSubject.setImageResource(icon);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}