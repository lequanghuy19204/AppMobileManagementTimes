package com.example.appmobilemanagementtimes;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GestureDetectorCompat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PomodoroActivity extends AppCompatActivity {

    private static final String TAG = "PomodoroActivity";
    private static final String PREFS_NAME = "PomodoroPrefs";
    private static final String WORK_DURATION_KEY = "workDuration";
    private static final String BREAK_DURATION_KEY = "breakDuration";
    private static final String SELECTED_MUSIC_KEY = "selectedMusic";
    private static final String POMODORO_COUNT_KEY = "pomodoroCount";
    private static final String CURRENT_PHASE_KEY = "currentPhase";

    // UI Components (Normal Mode)
    private ImageView startButton, pauseButton, stopButton;
    private TextView timerText, pomodoroText, musicText, fullscreenText;
    private LinearLayout pomodoroDropdown, controlButtons, customTimeLayout;
    private ProgressBar progressBar;
    private ConstraintLayout mainLayout, normalLayout, musicColumn, fullscreenColumn;
    private ImageView musicIcon, fullscreenIcon;
    private FrameLayout timerContainer;

    // UI Components (Fullscreen Mode)
    private FrameLayout fullscreenLayout;
    private LinearLayout fullscreenControlPanel;
    private ImageView fullscreenStartButton, fullscreenPauseButton, fullscreenMusicButton, fullscreenExitButton;
    private TextView fullscreenMinutesText, fullscreenSecondsText;
    private Handler controlPanelHandler = new Handler(Looper.getMainLooper());
    private Runnable hideControlPanelRunnable;
    private GestureDetectorCompat gestureDetector;
    private boolean isFullscreenMode = false;

    // Timer related
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private long workDuration;
    private long shortBreakDuration;
    private long timeLeftInMillis;
    private long totalDuration;
    private final String[] pomodoroOptions = {"25/5", "50/10", "90/15"};

    // Sound related
    private MediaPlayer mediaPlayer;
    private boolean isMusicPlaying = false;
    private float musicVolume = 0.5f;
    private SoundItem currentSoundItem;
    private SoundItem selectedSoundItem;
    private List<SoundItem> availableSounds = new ArrayList<>();
    private SoundItem notificationSoundItem;
    private SoundItem clockTickingSoundItem;

    private enum Phase { WORK, SHORT_BREAK }
    private Phase currentPhase = Phase.WORK;
    private int pomodoroCount = 0;

    // Sound item model class
    private static class SoundItem {
        String name;
        int resourceId;

        SoundItem(String name, int resourceId) {
            this.name = name;
            this.resourceId = resourceId;
        }

        String getDisplayName() {
            return name.replace("_", " ")
                    .replace(".mp3", "")
                    .replace(".ogg", "")
                    .replaceAll("(\\p{Ll})(\\p{Lu})", "$1 $2")
                    .toLowerCase()
                    .replaceFirst("^\\w", String.valueOf(Character.toUpperCase(name.charAt(0))));
        }
    }

    // Gesture Detector for Swipe Down
    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 50; // Giảm ngưỡng để tăng độ nhạy
        private static final int SWIPE_VELOCITY_THRESHOLD = 50; // Giảm ngưỡng để tăng độ nhạy

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) {
                Log.d(TAG, "onFling: MotionEvent e1 or e2 is null");
                return false;
            }

            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();
            Log.d(TAG, "onFling: diffY=" + diffY + ", diffX=" + diffX + ", velocityY=" + velocityY);

            if (Math.abs(diffY) > Math.abs(diffX) && Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY > 0) { // Swipe down
                    Log.d(TAG, "Swipe down detected, resetting timer and exiting fullscreen");
                    resetTimer();
                    exitFullscreenMode();
                    return true;
                }
            }
            Log.d(TAG, "onFling: Swipe not detected - conditions not met");
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pomodoro_main);

        initializeViews();
        setupListeners();
        loadSoundResources();
        loadSettings();

        if (!isTimerRunning) {
            setPhase(currentPhase);
        }

        updateButtonVisibility();
    }

    private void initializeViews() {
        // Normal Mode Views
        startButton = findViewById(R.id.startButton);
        pauseButton = findViewById(R.id.pauseButton);
        stopButton = findViewById(R.id.stopButton);
        timerText = findViewById(R.id.timerText);
        pomodoroDropdown = findViewById(R.id.pomodoroDropdown);
        pomodoroText = findViewById(R.id.pomodoroText);
        progressBar = findViewById(R.id.circularProgress);
        controlButtons = findViewById(R.id.controlButtons);
        mainLayout = findViewById(R.id.pomodoroMainLayout);
        normalLayout = findViewById(R.id.normalLayout);
        musicColumn = findViewById(R.id.musicColumn);
        musicIcon = findViewById(R.id.musicIcon);
        musicText = findViewById(R.id.musicText);
        fullscreenColumn = findViewById(R.id.fullscreenColumn);
        fullscreenIcon = findViewById(R.id.fullscreenIcon);
        fullscreenText = findViewById(R.id.fullscreenText);
        customTimeLayout = findViewById(R.id.customTimeLayout);
        timerContainer = findViewById(R.id.timerContainer);

        // Fullscreen Mode Views
        fullscreenLayout = findViewById(R.id.fullscreenLayout);
        fullscreenControlPanel = findViewById(R.id.fullscreenControlPanel);
        fullscreenStartButton = findViewById(R.id.fullscreenStartButton);
        fullscreenPauseButton = findViewById(R.id.fullscreenPauseButton);
        fullscreenMusicButton = findViewById(R.id.fullscreenMusicButton);
        fullscreenExitButton = findViewById(R.id.fullscreenExitButton);
        fullscreenMinutesText = findViewById(R.id.fullscreenMinutesText);
        fullscreenSecondsText = findViewById(R.id.fullscreenSecondsText);

        if (controlButtons == null || startButton == null || pauseButton == null || stopButton == null ||
                fullscreenLayout == null || fullscreenControlPanel == null || fullscreenMinutesText == null || fullscreenSecondsText == null) {
            Log.e(TAG, "UI components missing. Check pomodoro_main.xml for IDs.");
            Toast.makeText(this, "UI error: Missing components", Toast.LENGTH_LONG).show();
            return;
        }

        progressBar.setMax(100);
        progressBar.setProgress(0);

        // Set initial visibility
        pauseButton.setVisibility(View.GONE);
        stopButton.setVisibility(View.GONE);
        fullscreenPauseButton.setVisibility(View.GONE);

        // Setup gesture detector for swipe down
        gestureDetector = new GestureDetectorCompat(this, new SwipeGestureListener());
        if (gestureDetector == null) {
            Log.e(TAG, "Failed to initialize GestureDetectorCompat");
        } else {
            Log.d(TAG, "GestureDetectorCompat initialized successfully");
        }

        // Setup control panel auto-hide
        hideControlPanelRunnable = () -> {
            fullscreenControlPanel.setVisibility(View.GONE);
            Log.d(TAG, "Fullscreen control panel auto-hidden");
        };
    }

    private void setupListeners() {
        startButton.setOnClickListener(v -> startTimer());
        pauseButton.setOnClickListener(v -> pauseTimer());
        stopButton.setOnClickListener(v -> resetTimer());
        pomodoroDropdown.setOnClickListener(v -> showPomodoroOptions());
        musicColumn.setOnClickListener(v -> showMusicSelectionDialog());

        // Fullscreen mode toggle
        fullscreenColumn.setOnClickListener(v -> enterFullscreenMode());
        fullscreenIcon.setOnClickListener(v -> enterFullscreenMode());
        fullscreenText.setOnClickListener(v -> enterFullscreenMode());

        // Fullscreen control panel listeners
        fullscreenStartButton.setOnClickListener(v -> startTimer());
        fullscreenPauseButton.setOnClickListener(v -> pauseTimer());
        fullscreenMusicButton.setOnClickListener(v -> showMusicSelectionDialog());
        fullscreenExitButton.setOnClickListener(v -> {
            resetTimer();
            exitFullscreenMode();
        });

        // Show control panel on tap in fullscreen mode
        fullscreenLayout.setOnTouchListener((v, event) -> {
            if (!isFullscreenMode) {
                Log.d(TAG, "Not in fullscreen mode, ignoring touch event");
                return false;
            }

            Log.d(TAG, "Touch event received: action=" + event.getAction() + ", x=" + event.getX() + ", y=" + event.getY());
            boolean handled = gestureDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_DOWN && !handled) {
                Log.d(TAG, "ACTION_DOWN detected, showing control panel");
                showFullscreenControlPanel();
                return true;
            }
            return handled;
        });
    }

    private void enterFullscreenMode() {
        if (isFullscreenMode) return;

        // Hide status bar and navigation bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        // Show fullscreen layout, hide normal layout
        fullscreenLayout.setVisibility(View.VISIBLE);
        normalLayout.setVisibility(View.GONE);

        // Sync timer text
        updateTimerText();
        updateFullscreenButtonVisibility();

        isFullscreenMode = true;
        Log.d(TAG, "Entered fullscreen mode");
    }

    private void exitFullscreenMode() {
        if (!isFullscreenMode) return;

        // Show status bar and navigation bar
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        // Show normal layout, hide fullscreen layout
        fullscreenLayout.setVisibility(View.GONE);
        normalLayout.setVisibility(View.VISIBLE);
        fullscreenControlPanel.setVisibility(View.GONE);
        controlPanelHandler.removeCallbacks(hideControlPanelRunnable);

        // Ensure normal mode UI is fully synced
        updateTimerText();
        updateProgressBar();
        updateButtonVisibility();

        isFullscreenMode = false;
        Log.d(TAG, "Exited fullscreen mode");
    }

    private void showFullscreenControlPanel() {
        fullscreenControlPanel.setVisibility(View.VISIBLE);
        controlPanelHandler.removeCallbacks(hideControlPanelRunnable);
        controlPanelHandler.postDelayed(hideControlPanelRunnable, 3000); // Hide after 3 seconds
        Log.d(TAG, "Fullscreen control panel shown");
    }

    private void loadSoundResources() {
        availableSounds.clear();

        Field[] fields = R.raw.class.getFields();
        for (Field field : fields) {
            try {
                String name = field.getName();
                int resId = field.getInt(null);

                if (!isResourceAvailable(resId)) {
                    Log.e(TAG, "Resource not available: " + name);
                    continue;
                }

                if (name.equals("notification_sound")) {
                    notificationSoundItem = new SoundItem(name, resId);
                    Log.d(TAG, "Loaded notification sound: " + name);
                } else if (name.equals("clockticking")) {
                    clockTickingSoundItem = new SoundItem(name, resId);
                    Log.d(TAG, "Loaded clock ticking sound: " + name);
                } else {
                    availableSounds.add(new SoundItem(name, resId));
                    Log.d(TAG, "Loaded background sound: " + name);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading sound resource: " + field.getName(), e);
            }
        }

        if (notificationSoundItem == null) {
            Log.e(TAG, "Notification sound not found. Check res/raw/notification_sound.mp3");
            runOnUiThread(() -> Toast.makeText(this, "Notification sound missing", Toast.LENGTH_LONG).show());
        }
        if (clockTickingSoundItem == null) {
            Log.e(TAG, "Clock ticking sound not found. Check res/raw/clockticking.mp3");
            runOnUiThread(() -> Toast.makeText(this, "Clock ticking sound missing", Toast.LENGTH_LONG).show());
        }

        Log.d(TAG, "Loaded " + availableSounds.size() + " background sounds");
    }

    private boolean isResourceAvailable(int resId) {
        try (AssetFileDescriptor fd = getResources().openRawResourceFd(resId)) {
            if (fd == null || fd.getDeclaredLength() <= 0) {
                Log.e(TAG, "Resource unavailable or empty for ID: " + resId);
                return false;
            }
            Log.d(TAG, "Resource available for ID: " + resId + ", size: " + fd.getDeclaredLength() + " bytes");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Resource check failed for ID: " + resId + ". Exception: " + e.getMessage(), e);
            return false;
        }
    }

    private void loadSettings() {
        SharedPreferences sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        workDuration = sharedPref.getLong(WORK_DURATION_KEY, 50 * 60 * 1000); // Default 50min
        shortBreakDuration = sharedPref.getLong(BREAK_DURATION_KEY, 10 * 60 * 1000); // Default 10min

        int selectedSoundResId = sharedPref.getInt(SELECTED_MUSIC_KEY, -1);
        if (selectedSoundResId != -1) {
            for (SoundItem item : availableSounds) {
                if (item.resourceId == selectedSoundResId) {
                    selectedSoundItem = item;
                    break;
                }
            }
        }

        pomodoroCount = sharedPref.getInt(POMODORO_COUNT_KEY, 0);
        currentPhase = Phase.values()[sharedPref.getInt(CURRENT_PHASE_KEY, Phase.WORK.ordinal())];

        String currentSettingText = (workDuration / 60000) + "/" + (shortBreakDuration / 60000);
        pomodoroText.setText(currentSettingText);

        Log.d(TAG, "Settings loaded: Work=" + workDuration + "ms, Break=" + shortBreakDuration + "ms, Phase=" + currentPhase + ", Count=" + pomodoroCount);

        setPhase(currentPhase);
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putLong(WORK_DURATION_KEY, workDuration);
        editor.putLong(BREAK_DURATION_KEY, shortBreakDuration);
        editor.putInt(SELECTED_MUSIC_KEY, selectedSoundItem != null ? selectedSoundItem.resourceId : -1);
        editor.putInt(POMODORO_COUNT_KEY, pomodoroCount);
        editor.putInt(CURRENT_PHASE_KEY, currentPhase.ordinal());
        editor.apply();
        Log.d(TAG, "Settings saved: Work=" + workDuration + "ms, Break=" + shortBreakDuration + "ms, Phase=" + currentPhase + ", Count=" + pomodoroCount);
    }

    private void startTimer() {
        if (isTimerRunning) return;

        if (currentPhase == Phase.WORK && selectedSoundItem != null) {
            playSound(selectedSoundItem, true);
        } else if (currentPhase == Phase.SHORT_BREAK && clockTickingSoundItem != null) {
            playSound(clockTickingSoundItem, true);
        }

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
                pauseMusic();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    playNotificationSound();
                    onTimerFinish();
                    updateButtonVisibility();
                    updateFullscreenButtonVisibility();
                }, 200);
            }
        }.start();

        isTimerRunning = true;
        updateButtonVisibility();
        updateFullscreenButtonVisibility();
    }

    private void playNotificationSound() {
        if (isFinishing() || isDestroyed() || notificationSoundItem == null) {
            Log.w(TAG, "Cannot play notification sound: Activity finishing/destroyed or notificationSoundItem null");
            return;
        }

        Log.d(TAG, "Attempting to play notification sound: " + notificationSoundItem.name);

        if (!isResourceAvailable(notificationSoundItem.resourceId)) {
            Log.e(TAG, "Notification sound resource not found: " + notificationSoundItem.name);
            runOnUiThread(() -> Toast.makeText(this, "Notification sound file not found", Toast.LENGTH_LONG).show());
            return;
        }

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
            Log.d(TAG, "Notification stream volume: " + currentVolume + "/" + maxVolume);
            if (currentVolume == 0) {
                Log.w(TAG, "Notification volume is muted");
                runOnUiThread(() -> Toast.makeText(this, "Notification volume is muted", Toast.LENGTH_LONG).show());
            }
            int ringerMode = audioManager.getRingerMode();
            if (ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                Log.w(TAG, "Device in silent/vibrate mode, notification sound may not play");
                runOnUiThread(() -> Toast.makeText(this, "Device in silent/vibrate mode", Toast.LENGTH_LONG).show());
            }
        }

        try {
            MediaPlayer notificationPlayer = new MediaPlayer();
            AssetFileDescriptor afd = getResources().openRawResourceFd(notificationSoundItem.resourceId);
            if (afd == null) {
                Log.e(TAG, "AssetFileDescriptor is null for: " + notificationSoundItem.name);
                runOnUiThread(() -> Toast.makeText(this, "Cannot load notification sound", Toast.LENGTH_LONG).show());
                return;
            }

            notificationPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            notificationPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());

            notificationPlayer.setVolume(1.0f, 1.0f);
            notificationPlayer.setLooping(false);

            notificationPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "Notification sound prepared, starting playback");
                mp.start();
            });

            notificationPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "Notification sound playback completed");
                mp.release();
            });

            notificationPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error for notification sound: what=" + what + ", extra=" + extra);
                mp.release();
                runOnUiThread(() -> Toast.makeText(this, "Error playing notification sound", Toast.LENGTH_LONG).show());
                return true;
            });

            notificationPlayer.prepare();
            Log.d(TAG, "Notification sound player prepared");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up notification sound: " + e.getMessage(), e);
            runOnUiThread(() -> Toast.makeText(this, "Failed to play notification sound", Toast.LENGTH_LONG).show());
        }
    }

    private void playSound(SoundItem soundItem, boolean looping) {
        if (soundItem == null) {
            Log.w(TAG, "SoundItem is null, cannot play sound");
            return;
        }

        if (!isResourceAvailable(soundItem.resourceId)) {
            Log.e(TAG, "Sound resource not available: " + soundItem.name);
            runOnUiThread(() -> Toast.makeText(this, "Sound not found: " + soundItem.getDisplayName(), Toast.LENGTH_SHORT).show());
            return;
        }

        releaseMediaPlayer();

        try {
            mediaPlayer = MediaPlayer.create(this, soundItem.resourceId);
            if (mediaPlayer != null) {
                currentSoundItem = soundItem;
                mediaPlayer.setLooping(looping);
                mediaPlayer.setVolume(musicVolume, musicVolume);
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer error for sound " + soundItem.name + ": what=" + what + ", extra=" + extra);
                    releaseMediaPlayer();
                    runOnUiThread(() -> Toast.makeText(this, "Error playing sound: " + soundItem.getDisplayName(), Toast.LENGTH_SHORT).show());
                    return true;
                });
                mediaPlayer.start();
                isMusicPlaying = true;
                Log.d(TAG, "Playing sound: " + soundItem.name);
            } else {
                Log.e(TAG, "MediaPlayer.create returned null for: " + soundItem.name);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing sound: " + soundItem.name, e);
            releaseMediaPlayer();
        }
    }

    private void pauseTimer() {
        if (!isTimerRunning || countDownTimer == null) return;

        countDownTimer.cancel();
        isTimerRunning = false;
        updateButtonVisibility();
        updateFullscreenButtonVisibility();
        pauseMusic();
        Log.d(TAG, "Timer paused at: " + timeLeftInMillis + "ms");
    }

    private void resetTimer() {
        Log.d(TAG, "Resetting timer...");
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        isTimerRunning = false;
        currentPhase = Phase.WORK;
        pomodoroCount = 0;
        Log.d(TAG, "Phase set to WORK, pomodoroCount reset to 0");

        setPhase(Phase.WORK);
        saveSettings();
        updateButtonVisibility();
        updateFullscreenButtonVisibility();
        stopMusic();

        Log.d(TAG, "Timer reset complete");
    }

    private void restartTimerInFullscreen() {
        Log.d(TAG, "Restarting timer in fullscreen mode...");
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        // Reset time to initial duration but keep currentPhase and pomodoroCount
        totalDuration = currentPhase == Phase.WORK ? workDuration : shortBreakDuration;
        timeLeftInMillis = totalDuration;
        Log.d(TAG, "Restarting timer for phase " + currentPhase + ". Duration: " + totalDuration + "ms");

        updateTimerText();
        progressBar.setProgress(0, true);

        // If timer was running, start it again
        if (isTimerRunning) {
            startTimer();
        }

        updateButtonVisibility();
        updateFullscreenButtonVisibility();
        Log.d(TAG, "Timer restarted in fullscreen mode");
    }

    private void updateButtonVisibility() {
        // Direct visibility change for normal mode buttons
        if (isTimerRunning) {
            startButton.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);
            stopButton.setVisibility(View.VISIBLE);
        } else {
            pauseButton.setVisibility(View.GONE);
            startButton.setVisibility(View.VISIBLE);
            stopButton.setVisibility(timeLeftInMillis > 0 && timeLeftInMillis < totalDuration ? View.VISIBLE : View.GONE);
        }
    }

    private void updateFullscreenButtonVisibility() {
        // Direct visibility change for fullscreen mode buttons
        if (isTimerRunning) {
            fullscreenStartButton.setVisibility(View.GONE);
            fullscreenPauseButton.setVisibility(View.VISIBLE);
        } else {
            fullscreenPauseButton.setVisibility(View.GONE);
            fullscreenStartButton.setVisibility(View.VISIBLE);
        }
    }

    private void updateTimerText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        // Update normal mode timer (single line: "MM:SS")
        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        timerText.setText(timeFormatted);

        // Update fullscreen mode timer (two lines: "MM" and "SS")
        fullscreenMinutesText.setText(String.format(Locale.getDefault(), "%02d", minutes));
        fullscreenSecondsText.setText(String.format(Locale.getDefault(), "%02d", seconds));
    }

    private void updateProgressBar() {
        int progress = totalDuration > 0 ? (int) (((double) (totalDuration - timeLeftInMillis) / totalDuration) * 100) : 0;
        progressBar.setProgress(progress, true);
    }

    private void onTimerFinish() {
        Log.d(TAG, "Timer finished for phase: " + currentPhase);
        if (currentPhase == Phase.WORK) {
            pomodoroCount++;
            Log.d(TAG, "Pomodoro count incremented to: " + pomodoroCount);
            setPhase(Phase.SHORT_BREAK);
        } else {
            pomodoroCount = 0;
            Log.d(TAG, "Break finished, resetting pomodoroCount to 0");
            setPhase(Phase.WORK);
        }
        saveSettings();
        startTimer();
    }

    private void setPhase(Phase phase) {
        currentPhase = phase;
        totalDuration = currentPhase == Phase.WORK ? workDuration : shortBreakDuration;
        timeLeftInMillis = totalDuration;
        Log.d(TAG, "Setting phase to " + currentPhase + ". Duration: " + totalDuration + "ms");

        updateTimerText();
        updateProgressBar();
    }

    private void setPomodoroCycle(String cycle) {
        try {
            String[] parts = cycle.split("/");
            if (parts.length == 2) {
                long newWorkDuration = Long.parseLong(parts[0]) * 60_000L;
                long newBreakDuration = Long.parseLong(parts[1]) * 60_000L;

                if (newWorkDuration <= 0 || newBreakDuration <= 0) {
                    Toast.makeText(this, "Work and break times must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.d(TAG, "Setting Pomodoro Cycle to: " + cycle + " -> Work: " + newWorkDuration + "ms, Break: " + newBreakDuration + "ms");

                workDuration = newWorkDuration;
                shortBreakDuration = newBreakDuration;

                saveSettings();
                resetTimer();
                pomodoroText.setText(cycle);
            } else {
                Toast.makeText(this, "Invalid format. Use minutes/minutes (e.g., 50/10)", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid pomodoro cycle format: " + cycle, e);
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error setting pomodoro cycle: " + cycle, e);
            Toast.makeText(this, "Error setting cycle", Toast.LENGTH_SHORT).show();
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
            } else {
                setPomodoroCycle(title);
            }
            return true;
        });

        popup.show();
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

        builder.setTitle("Custom Pomodoro Time");
        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                int workMinutes = Integer.parseInt(etWorkTime.getText().toString());
                int breakMinutes = Integer.parseInt(etBreakTime.getText().toString());

                if (workMinutes <= 0 || breakMinutes <= 0) {
                    Toast.makeText(this, "Work and break times must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (workMinutes > 180 || breakMinutes > 60) {
                    Toast.makeText(this, "Please enter reasonable times (Work <= 180, Break <= 60)", Toast.LENGTH_SHORT).show();
                    return;
                }

                String newPreset = workMinutes + "/" + breakMinutes;
                setPomodoroCycle(newPreset);

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error saving custom time", e);
                Toast.makeText(this, "Error saving custom time", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void showMusicSelectionDialog() {
        if (isFinishing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_music_control, null);
        builder.setView(dialogView);
        builder.setTitle("Background Music");

        ListView musicListView = dialogView.findViewById(R.id.musicListView);
        List<String> soundNames = new ArrayList<>();
        for (SoundItem item : availableSounds) {
            soundNames.add(item.getDisplayName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_single_choice, soundNames) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setTextColor(position == availableSounds.indexOf(currentSoundItem) && isMusicPlaying ?
                        getResources().getColor(android.R.color.holo_purple) :
                        getResources().getColor(android.R.color.black));
                return view;
            }
        };
        musicListView.setAdapter(adapter);
        musicListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        ImageView muteToggleIcon = dialogView.findViewById(R.id.muteToggleIcon);
        SeekBar volumeSeekBar = dialogView.findViewById(R.id.volumeSeekBar);

        volumeSeekBar.setProgress((int) (musicVolume * 100));
        muteToggleIcon.setImageResource(musicVolume > 0 ? R.drawable.ic_volume_on : R.drawable.ic_volume_off);

        int initialSelection = selectedSoundItem != null ? availableSounds.indexOf(selectedSoundItem) : -1;
        if (initialSelection >= 0) {
            musicListView.setItemChecked(initialSelection, true);
        }
        final int[] previewSelectionIndex = {initialSelection};

        musicListView.setOnItemClickListener((parent, view, position, id) -> {
            Log.d(TAG, "Music item clicked: " + position + " (" + soundNames.get(position) + ")");
            previewSelectionIndex[0] = position;
            playSound(availableSounds.get(position), true);
            muteToggleIcon.setImageResource(isMusicPlaying ? R.drawable.ic_volume_on : R.drawable.ic_volume_off);
            adapter.notifyDataSetChanged();
        });

        muteToggleIcon.setOnClickListener(v -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                musicVolume = 0;
                mediaPlayer.setVolume(0, 0);
                isMusicPlaying = false;
                muteToggleIcon.setImageResource(R.drawable.ic_volume_off);
                volumeSeekBar.setProgress(0);
            } else {
                musicVolume = 0.5f;
                volumeSeekBar.setProgress((int)(musicVolume * 100));
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(musicVolume, musicVolume);
                    if (!mediaPlayer.isPlaying() && previewSelectionIndex[0] >= 0) {
                        try {
                            mediaPlayer.start();
                        } catch (IllegalStateException e) {
                            releaseMediaPlayer();
                            playSound(availableSounds.get(previewSelectionIndex[0]), true);
                        }
                    }
                    isMusicPlaying = true;
                    muteToggleIcon.setImageResource(R.drawable.ic_volume_on);
                } else if (previewSelectionIndex[0] >= 0) {
                    playSound(availableSounds.get(previewSelectionIndex[0]), true);
                    muteToggleIcon.setImageResource(R.drawable.ic_volume_on);
                } else {
                    muteToggleIcon.setImageResource(R.drawable.ic_volume_off);
                }
            }
            Log.d(TAG, "Mute toggle clicked. isMusicPlaying: " + isMusicPlaying + ", Volume: " + musicVolume);
            adapter.notifyDataSetChanged();
        });

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    musicVolume = progress / 100.0f;
                    isMusicPlaying = musicVolume > 0;
                    muteToggleIcon.setImageResource(isMusicPlaying ? R.drawable.ic_volume_on : R.drawable.ic_volume_off);
                    if (mediaPlayer != null) {
                        mediaPlayer.setVolume(musicVolume, musicVolume);
                    }
                    Log.d(TAG, "Volume changed to: " + musicVolume);
                    adapter.notifyDataSetChanged();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        builder.setPositiveButton("Select", (dialog, which) -> {
            if (previewSelectionIndex[0] >= 0) {
                selectedSoundItem = availableSounds.get(previewSelectionIndex[0]);
                saveSettings();
                Log.d(TAG, "Music selected: " + selectedSoundItem.name);
                stopMusic();
                currentSoundItem = selectedSoundItem;
                if (isTimerRunning && currentPhase == Phase.WORK && selectedSoundItem != null) {
                    playSound(selectedSoundItem, true);
                }
            }
        });

        builder.setNegativeButton("Close", (dialog, which) -> {
            Log.d(TAG, "Music selection cancelled.");
            if (selectedSoundItem != null && !selectedSoundItem.equals(currentSoundItem)) {
                playSound(selectedSoundItem, true);
            } else {
                stopMusic();
            }
        });

        builder.setOnDismissListener(dialog -> {
            if (selectedSoundItem != null && !selectedSoundItem.equals(currentSoundItem)) {
                playSound(selectedSoundItem, true);
            }
        });

        builder.create().show();
    }

    private void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.pause();
                isMusicPlaying = false;
                Log.d(TAG, "Music paused.");
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error pausing MediaPlayer, releasing.", e);
                releaseMediaPlayer();
            } catch (Exception e) {
                Log.e(TAG, "Generic error pausing MediaPlayer.", e);
                releaseMediaPlayer();
            }
        }
    }

    private void stopMusic() {
        Log.d(TAG, "Stopping and releasing music player.");
        releaseMediaPlayer();
    }

    private void releaseMediaPlayer() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
                Log.d(TAG, "MediaPlayer released.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception during MediaPlayer release", e);
        } finally {
            mediaPlayer = null;
            isMusicPlaying = false;
            currentSoundItem = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isTimerRunning) {
            pauseTimer();
        }
        saveSettings();
        Log.d(TAG, "onPause called. Settings saved.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called.");
        updateButtonVisibility();
        updateFullscreenButtonVisibility();
        updateTimerText();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called.");
        controlPanelHandler.removeCallbacksAndMessages(null);
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        releaseMediaPlayer();
    }
}