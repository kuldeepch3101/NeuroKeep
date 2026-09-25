package com.example.learnkeep;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.io.*;
import java.util.*;

public class TopicDetailsActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 201;

    private KnowledgeEntity entity;

    // Views
    private TextView     txtConfidenceValue, txtAttachmentStatus, txtYoutubeLink, txtLastScore;
    private EditText     etTitle, etNotes;
    private ChipGroup    chipGroupTags;
    private RecyclerView recyclerAttachments;
    private LinearLayout confidenceContainer;
    private int          selectedConfidence = 5;
    private final List<String> attachmentPaths = new ArrayList<>();
    private AttachmentAdapter  attachmentAdapter;
    private File         cameraPhotoFile;
    private ImageView    imgTopicIcon;
    private LinearLayout tagInputLayout;
    private boolean      hasTag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_details);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // ── Bind all views ─────────────────────────────────────────────
        etTitle = findViewById(R.id.etTitle);
        etNotes = findViewById(R.id.etNotes);
        txtYoutubeLink = findViewById(R.id.txtYoutubeLink);
        txtAttachmentStatus = findViewById(R.id.txtAttachmentStatus);
        chipGroupTags = findViewById(R.id.chipGroupTags);
        tagInputLayout  = findViewById(R.id.tagInputLayout);
        recyclerAttachments = findViewById(R.id.recyclerAttachments);
        confidenceContainer = findViewById(R.id.confidenceContainer);
        txtConfidenceValue  = findViewById(R.id.txtConfidenceValue);
        imgTopicIcon  = findViewById(R.id.imgTopicIcon);
        txtLastScore  = findViewById(R.id.txtLastScore);

        etNotes.setMovementMethod(new android.text.method.ScrollingMovementMethod());
        etNotes.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        // ── RecyclerView ───────────────────────────────────────────────
        recyclerAttachments.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        attachmentAdapter = new AttachmentAdapter(this, attachmentPaths, updated -> {
            txtAttachmentStatus.setText(updated.size() + " attachment(s)");
            entity.attachmentPaths = String.join(",", updated);
        });
        recyclerAttachments.setAdapter(attachmentAdapter);

        // ── Load topic ─────────────────────────────────────────────────
        int topicId = getIntent().getIntExtra("topic_id", -1);
        if (topicId == -1) { finish(); return; }

        entity = AppDatabase.getInstance(this).knowledgeDao().getById(topicId);
        if (entity == null) { finish(); return; }

        // ── Populate ───────────────────────────────────────────────────
        imgTopicIcon.setImageResource(TopicIconHelper.getIconFromTags(entity.tags));
        etTitle.setText(entity.title);
        etNotes.setText(entity.notes);
        txtYoutubeLink.setText(entity.youtubeLinks);

        // Last quiz score — always visible, text changes based on data
        if (entity.lastTestScore >= 0) {
            txtLastScore.setText("Last quiz: " + entity.lastTestScore
                    + "/" + entity.lastTestTotal + " correct");
        } else {
            txtLastScore.setText("No quiz taken yet — tap below to start AI quiz");
        }

        // YouTube click
        txtYoutubeLink.setOnClickListener(v -> {
            String url = entity.youtubeLinks;
            if (url != null && !url.isEmpty()) {
                if (!url.startsWith("http")) url = "https://" + url;
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });
        txtYoutubeLink.setOnLongClickListener(v -> {
            String url = entity.youtubeLinks;
            if (url != null && !url.isEmpty()) {
                android.content.ClipboardManager cb =
                        (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                cb.setPrimaryClip(android.content.ClipData.newPlainText("YouTube Link", url));
                Toast.makeText(this, "Link copied", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        // Confidence
        try { selectedConfidence = Integer.parseInt(entity.confidence); }
        catch (Exception e) { selectedConfidence = 5; }
        setupConfidenceSelector(selectedConfidence);

        // Tags
        if (entity.tags != null) {
            for (String tag : entity.tags.split(",")) {
                if (!tag.trim().isEmpty()) addTagChip(tag.replace("#", "").trim());
            }
        }

        EditText etTag = findViewById(R.id.etTag);
        findViewById(R.id.btnAddTag).setOnClickListener(v -> {
            String tag = etTag.getText().toString().trim();
            if (!tag.isEmpty()) { addTagChip(tag); etTag.setText(""); }
        });

        // Attachments
        if (entity.attachmentPaths != null && !entity.attachmentPaths.isEmpty()) {
            for (String path : entity.attachmentPaths.split(",")) {
                File f = new File(path);
                if (f.exists()) attachmentPaths.add(path);
            }
        }
        updateAttachmentUI();

        // ── Button listeners ───────────────────────────────────────────
        findViewById(R.id.btnCamera).setOnClickListener(v -> openCamera());
        findViewById(R.id.btnImage).setOnClickListener(v -> pickImage());
        findViewById(R.id.btnFile).setOnClickListener(v -> pickFile());
        findViewById(R.id.btnUpdate).setOnClickListener(v -> updateTopic());
        findViewById(R.id.btnDelete).setOnClickListener(v -> deleteTopic());

        // ✅ btnTakeQuiz — now exists in layout
        findViewById(R.id.btnTakeQuiz).setOnClickListener(v -> {
            saveEntityFields(); // persist any edits first
            Intent intent = new Intent(this, McqTestActivity.class);
            intent.putExtra(McqTestActivity.EXTRA_TOPIC_ID, entity.id);
            intent.putExtra(McqTestActivity.EXTRA_TRIGGER, "MANUAL");
            startActivity(intent);
        });
    }

    // ── Persist current field values to entity & DB ─────────────────────
    private void saveEntityFields() {
        entity.title           = etTitle.getText().toString().trim();
        entity.notes           = etNotes.getText().toString().trim();
        entity.youtubeLinks    = txtYoutubeLink.getText().toString().trim();
        entity.confidence      = String.valueOf(selectedConfidence);
        entity.tags            = collectTags();
        entity.attachmentPaths = String.join(",", attachmentPaths);
        AppDatabase.getInstance(this).knowledgeDao().update(entity);
    }

    // ── Confidence selector ─────────────────────────────────────────────
    private void setupConfidenceSelector(int pre) {
        confidenceContainer.removeAllViews();
        for (int i = 1; i <= 10; i++) {
            TextView tv = new TextView(this);
            tv.setText(String.valueOf(i));
            tv.setTextColor(Color.WHITE);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextSize(14f);
            int size = (int) (32 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(size, size);
            p.setMargins(6, 0, 6, 0);
            tv.setLayoutParams(p);
            tv.setBackground(confDrawable(i, i == pre));
            int val = i;
            tv.setOnClickListener(v -> { selectedConfidence = val; setupConfidenceSelector(val); });
            confidenceContainer.addView(tv);
        }
        txtConfidenceValue.setText("Selected: " + pre);
    }

    private GradientDrawable confDrawable(int v, boolean sel) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        if      (v <= 3) d.setColor(Color.parseColor("#EF4444"));
        else if (v <= 6) d.setColor(Color.parseColor("#F59E0B"));
        else if (v <= 8) d.setColor(Color.parseColor("#84CC16"));
        else             d.setColor(Color.parseColor("#22C55E"));
        d.setStroke(sel ? 4 : 2, sel ? Color.BLACK : Color.parseColor("#E5E7EB"));
        return d;
    }

    // ── Tags ────────────────────────────────────────────────────────────
    private void addTagChip(String text) {
        if (hasTag) return;
        Chip chip = new Chip(this);
        chip.setText("#" + text);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            chipGroupTags.removeView(chip);
            hasTag = false;
            if (tagInputLayout != null) tagInputLayout.setVisibility(View.VISIBLE);
        });
        chipGroupTags.addView(chip);
        hasTag = true;
        if (tagInputLayout != null) tagInputLayout.setVisibility(View.GONE);
    }

    private String collectTags() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chipGroupTags.getChildCount(); i++)
            sb.append(((Chip) chipGroupTags.getChildAt(i)).getText()).append(",");
        return sb.toString();
    }

    // ── Image picker ────────────────────────────────────────────────────
    private final ActivityResultLauncher<Intent> imagePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    r -> { if (r.getResultCode() == RESULT_OK && r.getData() != null)
                        saveToInternal(r.getData().getData()); });
    private void pickImage() {
        imagePicker.launch(new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
    }

    // ── File picker ─────────────────────────────────────────────────────
    private final ActivityResultLauncher<Intent> filePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    r -> { if (r.getResultCode() == RESULT_OK && r.getData() != null)
                        saveToInternal(r.getData().getData()); });
    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("*/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        filePicker.launch(i);
    }

    // ── Camera ──────────────────────────────────────────────────────────
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    r -> { if (r.getResultCode() == RESULT_OK
                            && cameraPhotoFile != null && cameraPhotoFile.exists()) {
                        attachmentPaths.add(cameraPhotoFile.getAbsolutePath());
                        updateAttachmentUI();
                    }});
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        else launchCamera();
    }
    private void launchCamera() {
        try {
            File dir = new File(getFilesDir(), "attachments");
            if (!dir.exists()) dir.mkdirs();
            cameraPhotoFile = new File(dir, System.currentTimeMillis() + ".jpg");
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", cameraPhotoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            cameraLauncher.launch(intent);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Internal file save ──────────────────────────────────────────────
    private void saveToInternal(Uri uri) {
        try {
            File dir = new File(getFilesDir(), "attachments");
            if (!dir.exists()) dir.mkdirs();
            String type = getContentResolver().getType(uri);
            String ext  = "dat";
            if ("image/jpeg".equals(type))       ext = "jpg";
            else if ("image/png".equals(type))   ext = "png";
            else if ("application/pdf".equals(type)) ext = "pdf";
            File file = new File(dir, System.currentTimeMillis() + "." + ext);
            InputStream  in  = getContentResolver().openInputStream(uri);
            FileOutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[4096]; int len;
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
            in.close(); out.close();
            attachmentPaths.add(file.getAbsolutePath());
            updateAttachmentUI();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateAttachmentUI() {
        txtAttachmentStatus.setText(attachmentPaths.size() + " attachment(s)");
        attachmentAdapter.notifyDataSetChanged();
    }

    // ── Update / Delete ─────────────────────────────────────────────────
    private void updateTopic() {
        saveEntityFields();
        ReminderScheduler.scheduleReminder(this, entity.id, entity.title, selectedConfidence);
        Toast.makeText(this, "Topic updated ✓", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void deleteTopic() {
        for (String path : attachmentPaths) { File f = new File(path); if (f.exists()) f.delete(); }
        ReminderScheduler.cancelReminder(this, entity.id);
        AppDatabase.getInstance(this).knowledgeDao().delete(entity);
        finish();
    }
}
