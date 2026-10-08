package com.videodownloader.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the application process alive while direct-file downloads are running.
 * The actual transfer remains resumable if Android stops the process.
 */
public class DownloadKeepAliveService extends Service {
    public static final String ACTION_START = "com.videodownloader.app.DOWNLOAD_START";
    public static final String ACTION_STOP = "com.videodownloader.app.DOWNLOAD_STOP";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "download_service";
    private static final Set<String> activeUrls = ConcurrentHashMap.newKeySet();

    public static boolean isActive(String url) { return activeUrls.contains(url); }
    public static void markActive(String url) { activeUrls.add(url); }
    public static void markInactive(String url) { activeUrls.remove(url); }
    public static boolean hasActiveDownloads() { return !activeUrls.isEmpty(); }

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "Active downloads", NotificationManager.IMPORTANCE_LOW));
        }
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction()) && !hasActiveDownloads()) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Video Downloader")
            .setContentText("Downloads are continuing")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
