package com.example.reproductormusica;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class MusicService extends Service {
    private static final String CHANNEL_ID = "media_playback_channel";
    private MediaPlayer mediaPlayer;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = MediaPlayer.create(this, R.raw.sample_music);
        mediaPlayer.setLooping(true);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Reproductor de Música",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Canal para controlar la reproducción de música");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "PLAY":
                    if (!mediaPlayer.isPlaying()) {
                        mediaPlayer.start();
                        showNotification(); // Actualiza la notificación al reanudar
                    }
                    break;

                case "PAUSE":
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        showNotification(); // Actualiza la notificación al pausar
                    }
                    break;

                case "SEEK":
                    int seekTo = intent.getIntExtra("SEEK_TO", mediaPlayer.getCurrentPosition());
                    mediaPlayer.seekTo(seekTo);
                    break;
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    private void showNotification() {
        Intent playPauseIntent = new Intent(this, MusicService.class);
        playPauseIntent.setAction(mediaPlayer.isPlaying() ? "PAUSE" : "PLAY");

        PendingIntent playPausePendingIntent = PendingIntent.getService(
                this,
                0,
                playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Reproductor de Música")
                .setContentText("Reproduciendo música")
                .addAction(
                        mediaPlayer.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        mediaPlayer.isPlaying() ? "Pausar" : "Reanudar",
                        playPausePendingIntent
                )
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0) // Mostrar los controles en la notificación expandida
                )
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        // Actualiza el progreso cada segundo
        new Thread(() -> {
            while (mediaPlayer != null && mediaPlayer.isPlaying()) {
                try {
                    int progress = mediaPlayer.getCurrentPosition();
                    notificationBuilder.setProgress(mediaPlayer.getDuration(), progress, false);
                    Notification notification = notificationBuilder.build();
                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    if (notificationManager != null) {
                        notificationManager.notify(1, notification);
                    }
                    Thread.sleep(1000); // Actualiza cada segundo
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        startForeground(1, notificationBuilder.build());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
