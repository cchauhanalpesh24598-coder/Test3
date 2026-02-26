package com.mknotes.app;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;

import com.mknotes.app.db.NotesRepository;
import com.mknotes.app.util.SessionManager;

public class NotesApplication extends Application {

    public static final String CHANNEL_ID_REMINDER = "notes_reminder_channel";
    public static final String CHANNEL_ID_GENERAL = "notes_general_channel";

    public void onCreate() {
        super.onCreate();

        // Allow file:// URIs to be shared with external apps
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        createNotificationChannels();

        // Firebase initializes automatically via google-services.json plugin

        // Auto-delete trash notes older than 30 days on app startup
        try {
            NotesRepository.getInstance(this).cleanupOldTrash();
        } catch (Exception e) {
            // Fail silently - don't block app startup
        }

        // Register ActivityLifecycleCallbacks for session timeout tracking
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            private int activityCount = 0;

            public void onActivityStarted(Activity activity) {
                if (activityCount == 0) {
                    SessionManager.getInstance(activity).onAppForegrounded();
                }
                activityCount++;
            }

            public void onActivityStopped(Activity activity) {
                activityCount--;
                if (activityCount == 0) {
                    SessionManager.getInstance(activity).onAppBackgrounded();
                }
            }

            public void onActivityCreated(Activity a, Bundle b) {}
            public void onActivityResumed(Activity a) {}
            public void onActivityPaused(Activity a) {}
            public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            public void onActivityDestroyed(Activity a) {}
        });
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel reminderChannel = new NotificationChannel(
                    CHANNEL_ID_REMINDER,
                    "Note Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            reminderChannel.setDescription("Notifications for note reminders");
            reminderChannel.enableVibration(true);

            NotificationChannel generalChannel = new NotificationChannel(
                    CHANNEL_ID_GENERAL,
                    "General",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            generalChannel.setDescription("General notifications");

            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(reminderChannel);
                manager.createNotificationChannel(generalChannel);
            }
        }
    }
}
