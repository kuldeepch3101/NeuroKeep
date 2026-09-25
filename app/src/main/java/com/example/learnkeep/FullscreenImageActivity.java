package com.example.learnkeep;

import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;

public class FullscreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);

        PhotoView photoView = findViewById(R.id.photoView);

        String path = getIntent().getStringExtra("path");
        if (path == null) {
            finish();
            return;
        }

        File imageFile = new File(path); // ✅ renamed variable
        if (imageFile.exists()) {
            photoView.setImageURI(Uri.fromFile(imageFile));
        }
    }
}
