package com.example.learnkeep;

import android.app.NotificationManager;
import android.content.Context;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class ReminderScheduler {

    public static void scheduleReminder(Context context, int topicId, String title, int confidence) {
        cancelReminder(context, topicId);
        int days;
        if (confidence <= 3)
            days = 1;
        else if (confidence <= 6)
            days = 3;
        else if (confidence <= 8)
            days = 7;
        else
            days = 14;

        Data data = new Data.Builder()
                .putString("title", title)
                .putInt("topicId", topicId)
                .build();

        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(ReminderWorker.class)
                        .setInitialDelay(days, TimeUnit.MINUTES)
                        .setInputData(data)
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(
                        "topic_reminder_" + topicId,
                        ExistingWorkPolicy.REPLACE,
                        request
                );
    }

    public static void cancelReminder(Context context, int topicId) {

        WorkManager.getInstance(context)
                .cancelUniqueWork("topic_reminder_" + topicId);
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(topicId); // ⭐ removes existing notification
    }
}
