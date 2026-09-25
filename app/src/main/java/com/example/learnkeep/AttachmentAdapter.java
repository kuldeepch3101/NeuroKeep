package com.example.learnkeep;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class AttachmentAdapter
        extends RecyclerView.Adapter<AttachmentAdapter.ViewHolder> {

    private final Context context;
    private final List<String> paths;
    private final AttachmentListener listener;

    // 🔗 CALLBACK INTERFACE
    public interface AttachmentListener {
        void onAttachmentListChanged(List<String> updatedPaths);
    }

    public AttachmentAdapter(Context context,
                             List<String> paths,
                             AttachmentListener listener) {
        this.context = context;
        this.paths = paths;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_attachment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder, int position) {

        String path = paths.get(position);
        File file = new File(path);

        // IMAGE PREVIEW
        if (isImage(path) && file.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            holder.imageView.setImageBitmap(bitmap);
        } else {
            holder.imageView.setImageResource(
                    android.R.drawable.ic_menu_save
            );
        }

        // CLICK PREVIEW
        holder.imageView.setOnClickListener(v -> {
            if (isImage(path)) {
                Intent intent =
                        new Intent(context, FullscreenImageActivity.class);
                intent.putExtra("path", path);
                context.startActivity(intent);
            } else if (path.endsWith(".pdf")) {
                openPdf(file);
            }
        });

        // ❌ REMOVE ATTACHMENT
        holder.btnRemove.setOnClickListener(v -> {
            File removed = new File(path);
            if (removed.exists()) removed.delete();

            paths.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, paths.size());

            listener.onAttachmentListChanged(paths);
        });
    }

    private void openPdf(File file) {
        Uri uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".provider",
                file
        );

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(
                Intent.createChooser(intent, "Open PDF")
        );
    }

    @Override
    public int getItemCount() {
        return paths.size();
    }

    private boolean isImage(String path) {
        return path.endsWith(".jpg")
                || path.endsWith(".jpeg")
                || path.endsWith(".png");
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton btnRemove;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgAttachment);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}
