package com.example.learnkeep;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Add topic FAB
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddKnowledgeActivity.class)));

        //AI Chat FAB (floating Gemini button)
        FloatingActionButton fabAi = findViewById(R.id.fabAiChat);
        fabAi.setOnClickListener(v ->
                new AiChatBottomSheet().show(getSupportFragmentManager(), "ai_chat"));

        //Bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            if      (item.getItemId() == R.id.nav_topics)   selected = new TopicsFragment();
            else if (item.getItemId() == R.id.nav_practice) selected = new PracticeFragment();
            else if (item.getItemId() == R.id.nav_stats)    selected = new StatsFragment();
            else if (item.getItemId() == R.id.nav_profile)  selected = new ProfileFragment();

            if (selected != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, selected)
                        .commit();
            }
            return true;
        });

        // Default screen
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new TopicsFragment())
                .commit();

        // Notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }
}
