package com.example.learnkeep;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ReminderWorker extends Worker {
    public ReminderWorker(Context context, WorkerParameters params) {
        super(context, params);
    }
    @Override
    public Result doWork() {
        String title  = getInputData().getString("title");
        int    topicId = getInputData().getInt("topicId", 0);
        if (title == null) title = "Your topic";

        // Check if topic is completed or deleted — skip if so
        KnowledgeEntity entity = AppDatabase.getInstance(getApplicationContext())
                .knowledgeDao().getById(topicId);

        if (entity == null || entity.isCompleted) return Result.success();

        createNotificationChannel();

        // ⭐ Deep-link straight into McqTestActivity with trigger=REMINDER
        Intent intent = new Intent(getApplicationContext(), McqTestActivity.class);
        intent.putExtra(McqTestActivity.EXTRA_TOPIC_ID, topicId);
        intent.putExtra(McqTestActivity.EXTRA_TRIGGER, "REMINDER");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                topicId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(getApplicationContext(), "study_channel")
                        .setSmallIcon(R.drawable.learnkeep_logo)
                        .setContentTitle("📚 Time to Review: " + title)
                        .setContentText("Tap to take a quick AI-generated quiz and update your reminder schedule!")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText("It's time to review \"" + title + "\". " +
                                         "Tap to take a quick AI quiz — your next reminder will be based on your score!"))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(getApplicationContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                manager.notify(topicId, builder.build());
            }
        } else {
            manager.notify(topicId, builder.build());
        }

        return Result.success();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "study_channel", "Study Reminder", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("AI-powered study reminders");
            NotificationManager nm = getApplicationContext()
                    .getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }
}
