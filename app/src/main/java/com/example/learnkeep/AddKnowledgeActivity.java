package com.example.learnkeep;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.*;

import androidx.activity.OnBackPressedCallback;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AddKnowledgeActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 101;
    private TextView txtAttachmentStatus;
    private EditText etTag;
    private ChipGroup chipGroupTags;
    private RecyclerView recyclerAttachments;
    private LinearLayout confidenceContainer;
    private TextView txtConfidenceValue;
    private int selectedConfidence = 5;
    private final List<String>attachmentPaths = new ArrayList<>();
    private AttachmentAdapter attachmentAdapter;
    private File cameraPhotoFile;
    private ImageView imgTopicIcon;
    private LinearLayout tagInputLayout;
    private boolean hasTag = false;
    private LinearLayout layoutSavingOverlay;
    private TextView txtSavingStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_knowledge);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        txtAttachmentStatus = findViewById(R.id.txtAttachmentStatus);
        etTag = findViewById(R.id.etTag);
        chipGroupTags = findViewById(R.id.chipGroupTags);
        tagInputLayout = findViewById(R.id.tagInputLayout);
        recyclerAttachments = findViewById(R.id.recyclerAttachments);
        confidenceContainer = findViewById(R.id.confidenceContainer);
        txtConfidenceValue  = findViewById(R.id.txtConfidenceValue);
        imgTopicIcon = findViewById(R.id.imgTopicIcon);
        layoutSavingOverlay = findViewById(R.id.layoutSavingOverlay);
        txtSavingStatus = findViewById(R.id.txtSavingStatus);

        updateAutoIcon();
        EditText etNotes = findViewById(R.id.etNotes);
        etNotes.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
        etNotes.setMovementMethod(new android.text.method.ScrollingMovementMethod());
        setupConfidenceSelector(selectedConfidence);

        recyclerAttachments.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        attachmentAdapter = new AttachmentAdapter(this, attachmentPaths, updatedPaths -> txtAttachmentStatus.setText(updatedPaths.size() + " attachment(s) added"));
        recyclerAttachments.setAdapter(attachmentAdapter);

        findViewById(R.id.btnAddTag).setOnClickListener(v -> addTag());
        findViewById(R.id.btnImage).setOnClickListener(v -> pickImage());
        findViewById(R.id.btnFile).setOnClickListener(v -> pickFile());
        findViewById(R.id.btnCamera).setOnClickListener(v -> openCamera());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveAndLaunchMcq());

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override public void handleOnBackPressed() {
                        deleteAllUnsavedAttachments();
                        finish();
                    }
                });
    }

    //Confidence selector
    private void setupConfidenceSelector(int preselected) {
        confidenceContainer.removeAllViews();
        for (int i = 1; i <= 10; i++) {
            TextView tv = new TextView(this);
            tv.setText(String.valueOf(i));
            tv.setTextColor(Color.WHITE);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextSize(14f);
            int size = (int)(32 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(size, size);
            p.setMargins(6, 0, 6, 0);
            tv.setLayoutParams(p);
            tv.setBackground(createConfidenceDrawable(i, i == preselected));
            int value = i;
            tv.setOnClickListener(v -> { selectedConfidence = value; setupConfidenceSelector(value); });
            confidenceContainer.addView(tv);
        }
        txtConfidenceValue.setText("Selected: " + preselected);
    }

    private GradientDrawable createConfidenceDrawable(int value, boolean selected) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        if (value <= 3)      d.setColor(Color.parseColor("#EF4444"));
        else if (value <= 6) d.setColor(Color.parseColor("#F59E0B"));
        else if (value <= 8) d.setColor(Color.parseColor("#84CC16"));
        else                 d.setColor(Color.parseColor("#22C55E"));
        d.setStroke(selected ? 4 : 2, selected ? Color.BLACK : Color.parseColor("#E5E7EB"));
        return d;
    }

    //Tags
    private void addTag() {
        if (hasTag) return;
        String tag = etTag.getText().toString().trim();
        if (tag.isEmpty()) return;
        Chip chip = new Chip(this);
        chip.setText("#" + tag);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            chipGroupTags.removeView(chip);
            hasTag = false;
            tagInputLayout.setVisibility(View.VISIBLE);
            updateAutoIcon();
        });
        chipGroupTags.addView(chip);
        hasTag = true;
        tagInputLayout.setVisibility(View.GONE);
        updateAutoIcon();
    }

    private String collectTags() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chipGroupTags.getChildCount(); i++)
            sb.append(((Chip) chipGroupTags.getChildAt(i)).getText()).append(",");
        return sb.toString();
    }

    // Image picker
    private final ActivityResultLauncher<Intent> imagePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    r -> { if (r.getResultCode() == RESULT_OK && r.getData() != null)
                        saveImageFromUri(r.getData().getData()); });
    private void pickImage() {
        imagePicker.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
    }

    //File picker
    private final ActivityResultLauncher<Intent> filePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    r -> { if (r.getResultCode() == RESULT_OK && r.getData() != null)
                        saveFileFromUri(r.getData().getData()); });
    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePicker.launch(intent);
    }

    // Camera

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    r -> { if (r.getResultCode() == RESULT_OK && cameraPhotoFile != null && cameraPhotoFile.exists()) {
                        attachmentPaths.add(cameraPhotoFile.getAbsolutePath()); updateAttachmentUI(); } });

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else { launchCamera(); }
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

    // Storage

    private void saveImageFromUri(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(in); in.close();
            File dir = new File(getFilesDir(), "attachments");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, System.currentTimeMillis() + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out); out.close();
            attachmentPaths.add(file.getAbsolutePath()); updateAttachmentUI();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveFileFromUri(Uri uri) {
        try {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(getContentResolver().getType(uri));
            if (ext == null) ext = "dat";
            File dir = new File(getFilesDir(), "attachments");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, System.currentTimeMillis() + "." + ext);
            InputStream in = getContentResolver().openInputStream(uri);
            FileOutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[4096]; int len;
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
            in.close(); out.close();
            attachmentPaths.add(file.getAbsolutePath()); updateAttachmentUI();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateAttachmentUI() {
        txtAttachmentStatus.setText(attachmentPaths.size() + " attachment(s) added");
        attachmentAdapter.notifyDataSetChanged();
    }

    //Save → insert DB → launch MCQ test

    private void saveAndLaunchMcq() {
        String title = ((EditText) findViewById(R.id.etTitle)).getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a topic title", Toast.LENGTH_SHORT).show();
            return;
        }

        layoutSavingOverlay.setVisibility(View.VISIBLE);
        txtSavingStatus.setText("💾 Saving topic…");
        findViewById(R.id.btnSave).setEnabled(false);

        KnowledgeEntity entity = new KnowledgeEntity();
        entity.title = title;
        entity.notes = ((EditText) findViewById(R.id.etNotes)).getText().toString();
        entity.youtubeLinks = ((EditText) findViewById(R.id.etYoutube)).getText().toString().trim();
        entity.confidence = String.valueOf(selectedConfidence);
        entity.tags = collectTags();
        entity.attachmentPaths = String.join(",", attachmentPaths);
        entity.createdAt = System.currentTimeMillis();
        entity.lastTestScore = -1;   // no test taken yet
        entity.isCompleted = false;

        new Thread(() -> {
            AppDatabase.getInstance(this).knowledgeDao().insert(entity);
            // After db.knowledgeDao().insert(entity) or update(entity):
            SyncManager.syncToCloud(this, new SyncManager.SyncCallback() {
                @Override public void onSuccess(String msg) { Log.d("Sync", "Backed up to cloud"); }
                @Override public void onFailure(String err) { Log.e("Sync", "Backup failed: " + err); }
            });

            KnowledgeEntity saved = AppDatabase.getInstance(this)
                    .knowledgeDao().getAll().get(0);

            runOnUiThread(() -> {
                txtSavingStatus.setText("🤖 Launching AI quiz…");
                // Launch MCQ test
                Intent intent = new Intent(this, McqTestActivity.class);
                intent.putExtra(McqTestActivity.EXTRA_TOPIC_ID, saved.id);
                intent.putExtra(McqTestActivity.EXTRA_TRIGGER, "MANUAL");
                layoutSavingOverlay.setVisibility(View.GONE);
                startActivity(intent);
                finish();
            });
        }).start();
    }

    //Cleanup

    private void deleteAllUnsavedAttachments() {
        for (String path : attachmentPaths) { File f = new File(path); if (f.exists()) f.delete(); }
        attachmentPaths.clear();
    }

    private void updateAutoIcon() {
        int iconRes = TopicIconHelper.getIconFromTags(collectTags());
        imgTopicIcon.setImageResource(iconRes);
    }
}
