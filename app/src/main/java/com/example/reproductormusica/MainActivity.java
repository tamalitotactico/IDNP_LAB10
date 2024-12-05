package com.example.reproductormusica;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.reproductormusica.R;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private TextView timeTextView;
    private Handler handler = new Handler();
    private Runnable updateSeekBar;
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }


        Button playButton = findViewById(R.id.playButton);
        Button pauseButton = findViewById(R.id.pauseButton);
        seekBar = findViewById(R.id.seekBar);
        timeTextView = findViewById(R.id.timeTextView);

        // Inicializar MediaPlayer
        mediaPlayer = MediaPlayer.create(this, R.raw.sample_music);
        seekBar.setMax(mediaPlayer.getDuration());

        // Actualizar SeekBar
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    updateTimeText();
                    handler.postDelayed(this, 500);
                }
            }
        };

        // Play
        playButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MusicService.class);
            intent.setAction("PLAY");
            startService(intent);
        });

        // Pause
        pauseButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MusicService.class);
            intent.setAction("PAUSE");
            startService(intent);
        });

        // Configuración del SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress);
                    updateTimeText();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Liberar MediaPlayer al cerrar
        mediaPlayer.setOnCompletionListener(mp -> {
            seekBar.setProgress(0);
            updateTimeText();
            handler.removeCallbacks(updateSeekBar);
        });
    }


    private void updateTimeText() {
        int currentPosition = mediaPlayer.getCurrentPosition();
        int duration = mediaPlayer.getDuration();

        String currentTime = formatTime(currentPosition);
        String totalTime = formatTime(duration);

        timeTextView.setText(String.format("%s / %s", currentTime, totalTime));
    }

    private String formatTime(int milliseconds) {
        int minutes = (milliseconds / 1000) / 60;
        int seconds = (milliseconds / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, MusicService.class);
        stopService(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer.isPlaying()) {
            Intent intent = new Intent(this, MusicService.class);
            intent.putExtra("ACTION", "PLAY");
            startService(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, MusicService.class);
        stopService(intent);
    }
}
