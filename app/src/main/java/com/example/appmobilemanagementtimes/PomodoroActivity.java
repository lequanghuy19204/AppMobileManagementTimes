package com.example.appmobilemanagementtimes;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.Locale;

public class PomodoroActivity extends AppCompatActivity {

    private static final String TAG = "PomodoroActivity";
    private ImageView startButton, pauseButton, stopButton;
    private TextView timerText, pomodoroText;
    private LinearLayout pomodoroDropdown;
    private ProgressBar progressBar;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private long workDuration;
    private long shortBreakDuration;
    private final long longBreakDuration = 15 * 60 * 1000;
    private long timeLeftInMillis;
    private long totalDuration;
    private final String[] pomodoroOptions = {"25/5", "50/10", "90/15"};

    private LinearLayout controlButtons;
    private ConstraintLayout mainLayout;
    private ConstraintLayout musicColumn;

    private enum Phase {
        WORK, SHORT_BREAK, LONG_BREAK
    }

    private Phase currentPhase = Phase.WORK;
    private int pomodoroCount = 0;

    private MediaPlayer mediaPlayer;
    private boolean isMusicPlaying = false;
    private float musicVolume = 0.5f; // Âm lượng mặc định (0.0f đến 1.0f)
    private int currentMusicIndex = -1;
    private final String[] musicNames = {
            "Clock Ticking", "Emotional Piano", "Light Rain",
            "Rain", "Relaxing Piano", "The Ocean"
    };
    private final int[] musicResIds = {
            R.raw.clock_ticking, R.raw.emotional_piano_music,
            R.raw.light_rain, R.raw.rain_sound,
            R.raw.relaxing_piano, R.raw.the_ocean_sound
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pomodoro_main);

        initializeViews();
        setupListeners();
        setPomodoroCycle("25/5");

        pauseButton.setVisibility(View.GONE);
        stopButton.setVisibility(View.GONE);
    }

    private void initializeViews() {
        startButton = findViewById(R.id.startButton);
        pauseButton = findViewById(R.id.pauseButton);
        stopButton = findViewById(R.id.stopButton);
        timerText = findViewById(R.id.timerText);
        pomodoroDropdown = findViewById(R.id.pomodoroDropdown);
        pomodoroText = findViewById(R.id.pomodoroText);
        progressBar = findViewById(R.id.circularProgress);
        controlButtons = findViewById(R.id.controlButtons);
        mainLayout = findViewById(R.id.pomodoroMainLayout);
        musicColumn = findViewById(R.id.musicColumn);
    }

    private void setupListeners() {
        startButton.setOnClickListener(v -> startTimer());
        pauseButton.setOnClickListener(v -> pauseTimer());
        stopButton.setOnClickListener(v -> resetTimer());
        pomodoroDropdown.setOnClickListener(v -> showPomodoroOptions());
        musicColumn.setOnClickListener(v -> showMusicSelectionDialog());
    }

    private void startTimer() {
        if (isTimerRunning) return;

        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerText();
                updateProgressBar();
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                playNotificationSound();
                onTimerFinish();
            }
        }.start();

        isTimerRunning = true;
        updateButtonVisibility();
    }

    private void pauseTimer() {
        if (!isTimerRunning || countDownTimer == null) return;

        try {
            countDownTimer.cancel();
        } catch (Exception e) {
            Log.e(TAG, "Error pausing timer", e);
        }

        isTimerRunning = false;
        updateButtonVisibility();
    }

    private void resetTimer() {
        try {
            if (countDownTimer != null) {
                countDownTimer.cancel();
                countDownTimer = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resetting timer", e);
        }

        isTimerRunning = false;
        setPhase(Phase.WORK);
        updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        startButton.setVisibility(isTimerRunning ? View.GONE : View.VISIBLE);
        pauseButton.setVisibility(isTimerRunning ? View.VISIBLE : View.GONE);
        stopButton.setVisibility(isTimerRunning ? View.VISIBLE : View.GONE);
    }

    private void updateTimerText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        timerText.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void updateProgressBar() {
        if (totalDuration > 0) {
            int progress = (int) ((totalDuration - timeLeftInMillis) * 100 / totalDuration);
            progressBar.setProgress(progress);
        }
    }

    private void playNotificationSound() {
        try {
            MediaPlayer notificationPlayer = MediaPlayer.create(this, R.raw.notification_sound);
            if (notificationPlayer != null) {
                notificationPlayer.setOnCompletionListener(MediaPlayer::release);
                notificationPlayer.start();
            } else {
                Log.w(TAG, "Failed to create notification MediaPlayer");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing notification sound", e);
        }
    }

    private void onTimerFinish() {
        pomodoroCount++;
        if (currentPhase == Phase.WORK) {
            if (pomodoroCount % 4 == 0) {
                setPhase(Phase.LONG_BREAK);
            } else {
                setPhase(Phase.SHORT_BREAK);
            }
        } else {
            setPhase(Phase.WORK);
        }
        startTimer();
    }

    private void setPhase(Phase phase) {
        currentPhase = phase;
        switch (currentPhase) {
            case WORK:
                totalDuration = workDuration;
                break;
            case SHORT_BREAK:
                totalDuration = shortBreakDuration;
                break;
            case LONG_BREAK:
                totalDuration = longBreakDuration;
                break;
        }
        timeLeftInMillis = totalDuration;
        updateTimerText();
        progressBar.setProgress(0);
    }

    private void setPomodoroCycle(String cycle) {
        try {
            String[] parts = cycle.split("/");
            if (parts.length == 2) {
                workDuration = Integer.parseInt(parts[0]) * 60_000L;
                shortBreakDuration = Integer.parseInt(parts[1]) * 60_000L;
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                    countDownTimer = null;
                }
                setPhase(Phase.WORK);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid pomodoro cycle format", e);
            Toast.makeText(this, "Invalid time format", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPomodoroOptions() {
        if (isFinishing()) return;

        PopupMenu popup = new PopupMenu(this, pomodoroDropdown);
        for (String option : pomodoroOptions) {
            popup.getMenu().add(option);
        }
        popup.getMenu().add("+ Custom");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("+ Custom")) {
                showCustomTimeDialog();
                return true;
            }
            pomodoroText.setText(title);
            setPomodoroCycle(title);
            return true;
        });

        try {
            popup.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing popup menu", e);
        }
    }

    private void showCustomTimeDialog() {
        if (isFinishing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_custom_time, null);
        builder.setView(dialogView);

        EditText etWorkTime = dialogView.findViewById(R.id.etWorkTime);
        EditText etBreakTime = dialogView.findViewById(R.id.etBreakTime);

        etWorkTime.setText(String.valueOf(workDuration / 60_000));
        etBreakTime.setText(String.valueOf(shortBreakDuration / 60_000));

        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                int workMinutes = Integer.parseInt(etWorkTime.getText().toString());
                int breakMinutes = Integer.parseInt(etBreakTime.getText().toString());
                if (workMinutes <= 0 || breakMinutes <= 0) {
                    Toast.makeText(this, "Thời gian phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                String newPreset = workMinutes + "/" + breakMinutes;
                pomodoroText.setText(newPreset);
                setPomodoroCycle(newPreset);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Vui lòng nhập số hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);

        try {
            builder.create().show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing custom time dialog", e);
        }
    }

    private void enableFullscreen() {
        // Giữ tên hàm, không có logic
    }

    private void showControlsTemporarily() {
        // Giữ tên hàm, hiển thị controls
        controlButtons.setVisibility(View.VISIBLE);
    }

    private void hideControlsTemporarily() {
        // Giữ tên hàm, gọi showControlsTemporarily
        showControlsTemporarily();
    }

    private void exitFullscreen() {
        // Giữ tên hàm, hiển thị controls
        controlButtons.setVisibility(View.VISIBLE);
    }

    private void showMusicSelectionDialog() {
        if (isFinishing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_music_control, null);
        builder.setView(dialogView);

        // Danh sách nhạc
        ListView musicListView = dialogView.findViewById(R.id.musicListView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, musicNames);
        musicListView.setAdapter(adapter);

        // Nút bật/tắt nhạc
        ToggleButton toggleMusicButton = dialogView.findViewById(R.id.toggleMusicButton);
        toggleMusicButton.setChecked(isMusicPlaying);

        // Thanh điều chỉnh âm lượng
        SeekBar volumeSeekBar = dialogView.findViewById(R.id.volumeSeekBar);
        volumeSeekBar.setProgress((int) (musicVolume * 100));

        // Xử lý chọn nhạc
        musicListView.setOnItemClickListener((parent, view, position, id) -> {
            try {
                // Nếu chọn bài khác hoặc MediaPlayer chưa khởi tạo
                if (currentMusicIndex != position || mediaPlayer == null) {
                    releaseMediaPlayer(); // Giải phóng MediaPlayer cũ
                    currentMusicIndex = position;
                    mediaPlayer = MediaPlayer.create(this, musicResIds[position]);
                    if (mediaPlayer != null) {
                        mediaPlayer.setLooping(true);
                        mediaPlayer.setVolume(musicVolume, musicVolume);
                        mediaPlayer.start();
                        isMusicPlaying = true;
                        toggleMusicButton.setChecked(true);
                        Toast.makeText(this, "Đang phát: " + musicNames[position], Toast.LENGTH_SHORT).show();
                    } else {
                        Log.w(TAG, "Failed to create MediaPlayer for " + musicNames[position]);
                        isMusicPlaying = false;
                        toggleMusicButton.setChecked(false);
                        currentMusicIndex = -1;
                        Toast.makeText(this, "Không thể phát " + musicNames[position], Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error playing music: " + musicNames[position], e);
                releaseMediaPlayer();
                isMusicPlaying = false;
                toggleMusicButton.setChecked(false);
                currentMusicIndex = -1;
                Toast.makeText(this, "Lỗi khi phát " + musicNames[position], Toast.LENGTH_SHORT).show();
            }
        });

        // Xử lý bật/tắt nhạc
        toggleMusicButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                if (isChecked) {
                    if (mediaPlayer == null && currentMusicIndex >= 0) {
                        // Khởi tạo lại nếu đã chọn bài trước đó
                        mediaPlayer = MediaPlayer.create(this, musicResIds[currentMusicIndex]);
                        if (mediaPlayer != null) {
                            mediaPlayer.setLooping(true);
                            mediaPlayer.setVolume(musicVolume, musicVolume);
                            mediaPlayer.start();
                            isMusicPlaying = true;
                            Toast.makeText(this, "Đang phát: " + musicNames[currentMusicIndex], Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, "Failed to create MediaPlayer for " + musicNames[currentMusicIndex]);
                            isMusicPlaying = false;
                            buttonView.setChecked(false);
                            Toast.makeText(this, "Không thể phát " + musicNames[currentMusicIndex], Toast.LENGTH_SHORT).show();
                        }
                    } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                        mediaPlayer.start();
                        isMusicPlaying = true;
                    }
                } else {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        isMusicPlaying = false;
                    }
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "IllegalStateException in toggle music", e);
                releaseMediaPlayer();
                isMusicPlaying = false;
                buttonView.setChecked(false);
                Toast.makeText(this, "Lỗi trạng thái nhạc", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error toggling music", e);
                releaseMediaPlayer();
                isMusicPlaying = false;
                buttonView.setChecked(false);
                Toast.makeText(this, "Lỗi khi bật/tắt nhạc", Toast.LENGTH_SHORT).show();
            }
        });

        // Xử lý âm lượng
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                musicVolume = progress / 100.0f;
                if (mediaPlayer != null) {
                    try {
                        mediaPlayer.setVolume(musicVolume, musicVolume);
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "IllegalStateException in setVolume", e);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        builder.setNegativeButton("Đóng", null);

        try {
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing music dialog", e);
        }
    }

    private void releaseMediaPlayer() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            isMusicPlaying = false;
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException in releaseMediaPlayer", e);
            mediaPlayer = null; // Đảm bảo đặt null dù có lỗi
            isMusicPlaying = false;
        } catch (Exception e) {
            Log.e(TAG, "Error releasing MediaPlayer", e);
            mediaPlayer = null;
            isMusicPlaying = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseTimer();
        if (mediaPlayer != null && isMusicPlaying) {
            try {
                mediaPlayer.pause();
            } catch (IllegalStateException e) {
                Log.e(TAG, "IllegalStateException in onPause", e);
                releaseMediaPlayer();
            } catch (Exception e) {
                Log.e(TAG, "Error pausing music in onPause", e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && isMusicPlaying && !mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.start();
            } catch (IllegalStateException e) {
                Log.e(TAG, "IllegalStateException in onResume", e);
                releaseMediaPlayer();
                Toast.makeText(this, "Lỗi khi tiếp tục nhạc", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error resuming music", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (countDownTimer != null) {
                countDownTimer.cancel();
                countDownTimer = null;
            }
            releaseMediaPlayer();
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
    }
}