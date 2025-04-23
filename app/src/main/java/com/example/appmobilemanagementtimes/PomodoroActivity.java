package com.example.appmobilemanagementtimes;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.view.GestureDetectorCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PomodoroActivity extends AppCompatActivity {

    private static final String TAG = "PomodoroActivity";
    private static final String PREFS_NAME = "PomodoroPrefs";
    private static final String APP_PREFS_NAME = "AppPrefs";
    private static final String WORK_DURATION_KEY = "workDuration";
    private static final String BREAK_DURATION_KEY = "breakDuration";
    private static final String SELECTED_MUSIC_KEY = "selectedMusic";
    private static final String POMODORO_COUNT_KEY = "pomodoroCount";
    private static final String CURRENT_PHASE_KEY = "currentPhase";
    private static final String SESSION_HISTORY_KEY = "sessionHistory";
    private static final String CHANNEL_ID = "PomodoroChannel";
    private static final int REQUEST_POST_NOTIFICATIONS = 2;
    private static final int NOTIFICATION_ID = 1;
    private static final String ACTION_PERMISSION_DENIED = "com.example.appmobilemanagementtimes.PERMISSION_DENIED";

    // Firebase
    private FirebaseFirestore db;
    private String userId;

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
    private String[] pomodoroOptions;

    // Auto pause for call
    private boolean isPausedDueToCall = false;

    // Background service related
    private long backgroundStartTime;

    // Sound related
    private MediaPlayer mediaPlayer;
    private boolean isMusicPlaying = false;
    private float musicVolume = 0.5f;
    private int currentMusicIndex = -1;
    private int selectedMusicIndex = -1;
    private final String[] musicNames = {
            "None", "Clock Ticking", "Emotional Piano", "Light Rain",
            "Rain", "Relaxing Piano", "The Ocean"
    };
    private final int[] musicResIds = {
            0, R.raw.clockticking, R.raw.emotional_piano_music,
            R.raw.light_rain, R.raw.rain_sound,
            R.raw.relaxing_piano, R.raw.the_ocean_sound
    };
    private int clockTickingSoundResId = R.raw.clockticking;

    // Session history
    private List<PomodoroSession> sessionHistory = new ArrayList<>();

    // Phase and count
    private enum Phase { WORK, SHORT_BREAK }
    private Phase currentPhase = Phase.WORK;
    private int pomodoroCount = 0;

    // Broadcast receiver for reset and permission denied
    private final BroadcastReceiver pomodoroReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.example.appmobilemanagementtimes.RESET_POMODORO".equals(action)) {
                if (!sessionHistory.isEmpty()) {
                    PomodoroSession currentSession = sessionHistory.get(sessionHistory.size() - 1);
                    currentSession.endTime = System.currentTimeMillis();
                    currentSession.isDone = false;
                    syncSettingsToFirebase();
                    saveSettings();
                }
                resetTimer();
                Log.d(TAG, "Received RESET_POMODORO broadcast, timer reset");
                Toast.makeText(context, "Timer reset do không hoạt động", Toast.LENGTH_SHORT).show();
            } else if (ACTION_PERMISSION_DENIED.equals(action)) {
                Toast.makeText(context, "Quyền thông báo bị thiếu. Vui lòng cấp quyền trong cài đặt.", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Received PERMISSION_DENIED broadcast, notification permission missing");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(PomodoroActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
                }
            }
        }
    };

    // Session model class
    public static class PomodoroSession {
        long startTime;
        long endTime;
        boolean isDone;

        public PomodoroSession(long startTime) {
            this.startTime = startTime;
            this.endTime = 0;
            this.isDone = false;
        }
    }

    // Gesture Detector for Swipe Down
    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

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
                    Log.d(TAG, "Swipe down detected, exiting fullscreen");
                    exitFullscreenMode();
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.pomodoro_main);
            Log.d(TAG, "onCreate: Layout set, initializing views");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Failed to set layout pomodoro_main.xml", e);
            Toast.makeText(this, "Lỗi tải giao diện. Kiểm tra file layout.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Lấy userId từ Intent hoặc SharedPreferences
        Intent intent = getIntent();
        userId = intent.getStringExtra("userId");
        if (userId == null) {
            SharedPreferences prefs = getSharedPreferences(APP_PREFS_NAME, MODE_PRIVATE);
            userId = prefs.getString("userId", null);
            if (userId == null) {
                Log.e(TAG, "onCreate: Không tìm thấy userId, chuyển hướng về đăng nhập");
                Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivity(loginIntent);
                finish();
                return;
            }
        }
        Log.d(TAG, "onCreate: userId=" + userId);

        // Khởi tạo Firestore
        try {
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Failed to initialize FirebaseFirestore", e);
            Toast.makeText(this, "Lỗi kết nối Firestore", Toast.LENGTH_LONG).show();
        }

        // Load pomodoro modes từ strings.xml
        try {
            pomodoroOptions = getResources().getStringArray(R.array.pomodoro_modes);
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Failed to load pomodoro_modes from resources", e);
            pomodoroOptions = new String[]{"50/10"}; // Default fallback
        }

        // Request POST_NOTIFICATIONS permission cho Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onCreate: Requesting POST_NOTIFICATIONS permission");
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    Toast.makeText(this, "Quyền thông báo cần thiết để hiển thị trạng thái timer.", Toast.LENGTH_LONG).show();
                }
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
            } else {
                Log.d(TAG, "onCreate: POST_NOTIFICATIONS permission already granted");
            }
        }

        // Request runtime permission cho phone state
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onCreate: Requesting READ_PHONE_STATE permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
        } else {
            Log.d(TAG, "onCreate: READ_PHONE_STATE permission already granted");
        }

        // Khởi tạo các tính năng chính
        try {
            initializeViews();
            setupListeners();
            loadSoundResources();
            loadSettings();
            setupCallListener();
            createNotificationChannel();
            registerPomodoroReceiver();
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Failed to initialize components", e);
            Toast.makeText(this, "Lỗi khởi tạo ứng dụng", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!isTimerRunning) {
            setPhase(currentPhase);
        }
        updateButtonVisibility();

        // Xử lý notification tap
        if (getIntent().getBooleanExtra("fromNotification", false)) {
            Log.d(TAG, "onCreate: Detected notification tap with userId=" + userId);
            // Khôi phục trạng thái timer nếu cần
            stopService(new Intent(this, PomodoroService.class));
        }

        // Đồng bộ với Firebase
        if (isNetworkAvailable()) {
            Log.d(TAG, "onCreate: Network available, syncing with Firebase");
            syncWithFirebase();
        } else {
            Log.w(TAG, "onCreate: No network available, running in offline mode");
            Toast.makeText(this, "Không có kết nối mạng. Chạy ở chế độ offline.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void registerPomodoroReceiver() {
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.example.appmobilemanagementtimes.RESET_POMODORO");
            filter.addAction(ACTION_PERMISSION_DENIED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(pomodoroReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(pomodoroReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            }
            Log.d(TAG, "registerPomodoroReceiver: Receiver registered for RESET_POMODORO and PERMISSION_DENIED");
        } catch (Exception e) {
            Log.e(TAG, "registerPomodoroReceiver: Failed to register receiver", e);
            Toast.makeText(this, "Lỗi đăng ký receiver", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeViews() {
        try {
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

            fullscreenLayout = findViewById(R.id.fullscreenLayout);
            fullscreenControlPanel = findViewById(R.id.fullscreenControlPanel);
            fullscreenStartButton = findViewById(R.id.fullscreenStartButton);
            fullscreenPauseButton = findViewById(R.id.fullscreenPauseButton);
            fullscreenMusicButton = findViewById(R.id.fullscreenMusicButton);
            fullscreenExitButton = findViewById(R.id.fullscreenExitButton);
            fullscreenMinutesText = findViewById(R.id.fullscreenMinutesText);
            fullscreenSecondsText = findViewById(R.id.fullscreenSecondsText);
        } catch (Exception e) {
            Log.e(TAG, "initializeViews: Failed to find views in layout", e);
            Toast.makeText(this, "Lỗi giao diện. Kiểm tra pomodoro_main.xml", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (controlButtons == null || startButton == null || pauseButton == null || stopButton == null ||
                fullscreenLayout == null || fullscreenControlPanel == null || fullscreenMinutesText == null || fullscreenSecondsText == null) {
            Log.e(TAG, "UI components missing. Check pomodoro_main.xml for IDs.");
            Toast.makeText(this, "Lỗi UI: Thiếu thành phần", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        progressBar.setMax(100);
        progressBar.setProgress(0);

        pauseButton.setVisibility(View.GONE);
        stopButton.setVisibility(View.GONE);
        fullscreenPauseButton.setVisibility(View.GONE);

        gestureDetector = new GestureDetectorCompat(this, new SwipeGestureListener());
        if (gestureDetector == null) {
            Log.e(TAG, "Failed to initialize GestureDetectorCompat");
        } else {
            Log.d(TAG, "GestureDetectorCompat initialized successfully");
        }

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

        fullscreenColumn.setOnClickListener(v -> enterFullscreenMode());
        fullscreenIcon.setOnClickListener(v -> enterFullscreenMode());
        fullscreenText.setOnClickListener(v -> enterFullscreenMode());

        fullscreenStartButton.setOnClickListener(v -> startTimer());
        fullscreenPauseButton.setOnClickListener(v -> pauseTimer());
        fullscreenMusicButton.setOnClickListener(v -> showMusicSelectionDialog());
        fullscreenExitButton.setOnClickListener(v -> {
            resetTimer();
            exitFullscreenMode();
        });

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            } else {
                Log.e(TAG, "enterFullscreenMode: WindowInsetsController is null");
            }
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }

        fullscreenLayout.setVisibility(View.VISIBLE);
        normalLayout.setVisibility(View.GONE);

        updateTimerText();
        updateFullscreenButtonVisibility();

        isFullscreenMode = true;
        Log.d(TAG, "Entered fullscreen mode");
    }

    private void exitFullscreenMode() {
        if (!isFullscreenMode) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            } else {
                Log.e(TAG, "exitFullscreenMode: WindowInsetsController is null");
            }
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }

        fullscreenLayout.setVisibility(View.GONE);
        normalLayout.setVisibility(View.VISIBLE);
        fullscreenControlPanel.setVisibility(View.GONE);
        controlPanelHandler.removeCallbacks(hideControlPanelRunnable);

        updateTimerText();
        updateProgressBar();
        updateButtonVisibility();
        pomodoroText.setText((workDuration / 60000) + "/" + (shortBreakDuration / 60000));
        musicText.setText(selectedMusicIndex >= 0 ? musicNames[selectedMusicIndex] : "None");

        isFullscreenMode = false;
        Log.d(TAG, "Exited fullscreen mode, UI synced: timeLeftInMillis=" + timeLeftInMillis + ", phase=" + currentPhase);
    }

    private void showFullscreenControlPanel() {
        fullscreenControlPanel.setVisibility(View.VISIBLE);
        controlPanelHandler.removeCallbacks(hideControlPanelRunnable);
        controlPanelHandler.postDelayed(hideControlPanelRunnable, 3000);
        Log.d(TAG, "Fullscreen control panel shown");
    }

    private void loadSoundResources() {
        Log.d(TAG, "loadSoundResources: Loading predefined sound resources");

        if (!isResourceAvailable(clockTickingSoundResId)) {
            Log.e(TAG, "loadSoundResources: Clock ticking sound resource not found: R.raw.clockticking");
            clockTickingSoundResId = 0;
        } else {
            Log.d(TAG, "loadSoundResources: Clock ticking sound loaded: R.raw.clockticking");
        }

        for (int i = 1; i < musicResIds.length; i++) {
            if (!isResourceAvailable(musicResIds[i])) {
                Log.e(TAG, "loadSoundResources: Sound resource not available: " + musicNames[i]);
            } else {
                Log.d(TAG, "loadSoundResources: Loaded sound: " + musicNames[i]);
            }
        }
    }

    private boolean isResourceAvailable(int resId) {
        if (resId == 0) return false;
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
        workDuration = sharedPref.getLong(WORK_DURATION_KEY, 50 * 60 * 1000);
        shortBreakDuration = sharedPref.getLong(BREAK_DURATION_KEY, 10 * 60 * 1000);

        try {
            if (sharedPref.contains(SELECTED_MUSIC_KEY)) {
                Object musicValue = sharedPref.getAll().get(SELECTED_MUSIC_KEY);
                if (musicValue instanceof Integer) {
                    selectedMusicIndex = sharedPref.getInt(SELECTED_MUSIC_KEY, -1);
                } else if (musicValue instanceof String) {
                    String musicName = (String) musicValue;
                    selectedMusicIndex = -1;
                    for (int i = 0; i < musicNames.length; i++) {
                        if (musicNames[i].equals(musicName)) {
                            selectedMusicIndex = i;
                            break;
                        }
                    }
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putInt(SELECTED_MUSIC_KEY, selectedMusicIndex);
                    editor.apply();
                    Log.d(TAG, "loadSettings: Migrated SELECTED_MUSIC_KEY from String (" + musicName + ") to Integer (" + selectedMusicIndex + ")");
                } else {
                    selectedMusicIndex = -1;
                    Log.w(TAG, "loadSettings: Invalid type for SELECTED_MUSIC_KEY: " + musicValue.getClass().getSimpleName());
                }
            } else {
                selectedMusicIndex = -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "loadSettings: Error reading SELECTED_MUSIC_KEY", e);
            selectedMusicIndex = -1;
        }

        if (selectedMusicIndex >= musicNames.length || selectedMusicIndex < -1) {
            selectedMusicIndex = -1;
        }

        try {
            pomodoroCount = sharedPref.getInt(POMODORO_COUNT_KEY, 0);
        } catch (ClassCastException e) {
            Log.e(TAG, "loadSettings: ClassCastException for POMODORO_COUNT_KEY, resetting to 0", e);
            pomodoroCount = 0;
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(POMODORO_COUNT_KEY, 0);
            editor.apply();
        }

        try {
            int phaseIndex = sharedPref.getInt(CURRENT_PHASE_KEY, Phase.WORK.ordinal());
            if (phaseIndex >= 0 && phaseIndex < Phase.values().length) {
                currentPhase = Phase.values()[phaseIndex];
            } else {
                currentPhase = Phase.WORK;
            }
        } catch (ClassCastException e) {
            Log.e(TAG, "loadSettings: ClassCastException for CURRENT_PHASE_KEY, resetting to WORK", e);
            currentPhase = Phase.WORK;
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(CURRENT_PHASE_KEY, Phase.WORK.ordinal());
            editor.apply();
        }

        String sessionHistoryJson = sharedPref.getString(SESSION_HISTORY_KEY, null);
        if (sessionHistoryJson != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<PomodoroSession>>() {}.getType();
            sessionHistory = gson.fromJson(sessionHistoryJson, type);
            if (sessionHistory == null) {
                sessionHistory = new ArrayList<>();
            }
        } else {
            sessionHistory = new ArrayList<>();
        }

        String currentSettingText = (workDuration / 60000) + "/" + (shortBreakDuration / 60000);
        pomodoroText.setText(currentSettingText);
        musicText.setText(selectedMusicIndex >= 0 ? musicNames[selectedMusicIndex] : "None");

        Log.d(TAG, "Settings loaded: Work=" + workDuration + "ms, Break=" + shortBreakDuration + "ms, Phase=" + currentPhase +
                ", Count=" + pomodoroCount + ", SelectedMusicIndex=" + selectedMusicIndex);

        setPhase(currentPhase);
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putLong(WORK_DURATION_KEY, workDuration);
        editor.putLong(BREAK_DURATION_KEY, shortBreakDuration);
        editor.putInt(SELECTED_MUSIC_KEY, selectedMusicIndex);
        editor.putInt(POMODORO_COUNT_KEY, pomodoroCount);
        editor.putInt(CURRENT_PHASE_KEY, currentPhase.ordinal());

        Gson gson = new Gson();
        editor.putString(SESSION_HISTORY_KEY, gson.toJson(sessionHistory));
        editor.apply();
        Log.d(TAG, "Settings saved: Work=" + workDuration + "ms, Break=" + shortBreakDuration + "ms, Phase=" + currentPhase +
                ", Count=" + pomodoroCount + ", SelectedMusicIndex=" + selectedMusicIndex);
    }

    private void syncSettingsToFirebase() {
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "syncSettingsToFirebase: No userId, skipping Firebase sync");
            return;
        }

        Log.d(TAG, "syncSettingsToFirebase: Syncing settings with Firebase for userId: " + userId);
        Map<String, Object> settings = new HashMap<>();
        settings.put("workDuration", workDuration);
        settings.put("shortBreakDuration", shortBreakDuration);
        settings.put("selectedMusic", selectedMusicIndex >= 0 ? musicNames[selectedMusicIndex] : "None");

        db.collection("users").document(userId).collection("settings")
                .document("pomodoro").set(settings)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "syncSettingsToFirebase: Settings synced to Firebase"))
                .addOnFailureListener(e -> Log.e(TAG, "syncSettingsToFirebase: Error syncing settings to Firebase", e));

        if (!sessionHistory.isEmpty()) {
            WriteBatch batch = db.batch();
            for (PomodoroSession session : sessionHistory) {
                Map<String, Object> sessionData = new HashMap<>();
                sessionData.put("startTime", session.startTime);
                sessionData.put("endTime", session.endTime);
                sessionData.put("isDone", session.isDone);
                batch.set(
                        db.collection("users").document(userId).collection("sessions")
                                .document(String.valueOf(session.startTime)),
                        sessionData
                );
            }
            batch.commit()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "syncSettingsToFirebase: All sessions synced to Firebase"))
                    .addOnFailureListener(e -> Log.e(TAG, "syncSettingsToFirebase: Error syncing sessions to Firebase", e));
        }
    }

    private void syncWithFirebase() {
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "syncWithFirebase: No userId, skipping Firebase sync");
            return;
        }

        Log.d(TAG, "syncWithFirebase: Syncing settings with Firebase for userId: " + userId);
        db.collection("users").document(userId).collection("settings")
                .document("pomodoro").get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Long workDurationLong = document.getLong("workDuration");
                            Long shortBreakDurationLong = document.getLong("shortBreakDuration");
                            workDuration = workDurationLong != null ? workDurationLong : workDuration;
                            shortBreakDuration = shortBreakDurationLong != null ? shortBreakDurationLong : shortBreakDuration;
                            String soundName = document.getString("selectedMusic");
                            selectedMusicIndex = -1;
                            if (soundName != null) {
                                for (int i = 0; i < musicNames.length; i++) {
                                    if (musicNames[i].equals(soundName)) {
                                        selectedMusicIndex = i;
                                        break;
                                    }
                                }
                            }
                            setPhase(currentPhase);
                            String currentSettingText = (workDuration / 60000) + "/" + (shortBreakDuration / 60000);
                            pomodoroText.setText(currentSettingText);
                            musicText.setText(selectedMusicIndex >= 0 ? musicNames[selectedMusicIndex] : "None");
                            saveSettings();
                            Log.d(TAG, "syncWithFirebase: Settings synced from Firebase, selectedMusicIndex=" + selectedMusicIndex);
                        } else {
                            Log.d(TAG, "syncWithFirebase: No settings document found in Firebase");
                        }
                    } else {
                        Log.e(TAG, "syncWithFirebase: Error syncing settings from Firebase", task.getException());
                    }
                });

        db.collection("users").document(userId).collection("sessions")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "syncWithFirebase: Error syncing sessions from Firebase", error);
                        return;
                    }
                    if (value != null) {
                        sessionHistory.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Long startTime = doc.getLong("startTime");
                            Long endTime = doc.getLong("endTime");
                            Boolean isDone = doc.getBoolean("isDone");
                            if (startTime != null && endTime != null && isDone != null) {
                                PomodoroSession session = new PomodoroSession(startTime);
                                session.endTime = endTime;
                                session.isDone = isDone;
                                sessionHistory.add(session);
                            } else {
                                Log.w(TAG, "syncWithFirebase: Invalid session data in Firebase: " + doc.getId());
                            }
                        }
                        saveSettings();
                        Log.d(TAG, "syncWithFirebase: Session history synced from Firebase, size: " + sessionHistory.size());
                    } else {
                        Log.w(TAG, "syncWithFirebase: No session data received from Firebase");
                    }
                });
    }

    private void startTimer() {
        if (isTimerRunning) return;

        stopMusic();

        if (currentPhase == Phase.WORK && selectedMusicIndex > 0 && selectedMusicIndex < musicResIds.length) {
            playSound(musicResIds[selectedMusicIndex], musicNames[selectedMusicIndex], true);
        } else if (currentPhase == Phase.SHORT_BREAK && clockTickingSoundResId != 0) {
            playSound(clockTickingSoundResId, "Clock Ticking", true);
        }

        if (currentPhase == Phase.WORK) {
            PomodoroSession session = new PomodoroSession(System.currentTimeMillis());
            sessionHistory.add(session);
            Log.d(TAG, "startTimer: Added new session to history, size: " + sessionHistory.size());
        }

        totalDuration = currentPhase == Phase.WORK ? workDuration : shortBreakDuration;
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
                    sendNotification(currentPhase == Phase.WORK ? "Phiên làm việc hoàn tất!" : "Thời gian nghỉ kết thúc!");
                    onTimerFinish();
                    updateButtonVisibility();
                    updateFullscreenButtonVisibility();
                }, 200);
            }
        }.start();

        isTimerRunning = true;
        updateButtonVisibility();
        updateFullscreenButtonVisibility();
        Log.d(TAG, "startTimer: Timer started, timeLeftInMillis=" + timeLeftInMillis + ", phase=" + currentPhase);
    }

    private void sendNotification(String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "sendNotification: POST_NOTIFICATIONS permission not granted");
            return;
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            Log.e(TAG, "sendNotification: NotificationManager is null");
            return;
        }

        Intent intent = new Intent(this, PomodoroActivity.class);
        intent.putExtra("fromNotification", true);
        intent.putExtra("userId", userId);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Pomodoro Timer")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
        Log.d(TAG, "sendNotification: Notification sent - " + message);
    }

    private void playSound(int resId, String soundName, boolean looping) {
        if (resId == 0) {
            Log.w(TAG, "playSound: Invalid resource ID for sound: " + soundName);
            return;
        }

        if (!isResourceAvailable(resId)) {
            Log.e(TAG, "playSound: Sound resource not available: " + soundName);
        }

        releaseMediaPlayer();

        try {
            mediaPlayer = MediaPlayer.create(this, resId);
            if (mediaPlayer != null) {
                for (int i = 0; i < musicResIds.length; i++) {
                    if (musicResIds[i] == resId) {
                        currentMusicIndex = i;
                        break;
                    }
                }
                mediaPlayer.setLooping(looping);
                mediaPlayer.setVolume(musicVolume, musicVolume);
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "playSound: MediaPlayer error for sound " + soundName +
                            ": what=" + what + ", extra=" + extra);
                    releaseMediaPlayer();
                    return true;
                });
                mediaPlayer.start();
                isMusicPlaying = true;
                Log.d(TAG, "playSound: Playing sound: " + soundName);
            } else {
                Log.e(TAG, "playSound: MediaPlayer.create returned null for: " + soundName);
                currentMusicIndex = -1;
                isMusicPlaying = false;
            }
        } catch (Exception e) {
            Log.e(TAG, "playSound: Error playing sound: " + soundName, e);
            releaseMediaPlayer();
            currentMusicIndex = -1;
            isMusicPlaying = false;
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
        syncSettingsToFirebase();
        saveSettings();
        updateButtonVisibility();
        updateFullscreenButtonVisibility();
        stopMusic();

        Log.d(TAG, "Timer reset complete");
    }

    private void updateButtonVisibility() {
        if (isTimerRunning) {
            startButton.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);
            stopButton.setVisibility(View.VISIBLE);
            fullscreenStartButton.setVisibility(View.GONE);
            fullscreenPauseButton.setVisibility(View.VISIBLE);
        } else {
            pauseButton.setVisibility(View.GONE);
            startButton.setVisibility(View.VISIBLE);
            stopButton.setVisibility(timeLeftInMillis > 0 && timeLeftInMillis < totalDuration ? View.VISIBLE : View.GONE);
            fullscreenPauseButton.setVisibility(View.GONE);
            fullscreenStartButton.setVisibility(View.VISIBLE);
        }
    }

    private void updateFullscreenButtonVisibility() {
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

        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        timerText.setText(timeFormatted);

        if (isFullscreenMode) {
            fullscreenMinutesText.setText(String.format(Locale.getDefault(), "%02d", minutes));
            fullscreenSecondsText.setText(String.format(Locale.getDefault(), "%02d", seconds));
        }
        Log.d(TAG, "updateTimerText: timeLeftInMillis=" + timeLeftInMillis + ", formatted=" + timeFormatted);
    }

    private void updateProgressBar() {
        int progress = totalDuration > 0 ? (int) (((double) (totalDuration - timeLeftInMillis) / totalDuration) * 100) : 0;
        progressBar.setProgress(progress);
    }

    private void onTimerFinish() {
        Log.d(TAG, "Timer finished for phase: " + currentPhase);
        if (currentPhase == Phase.WORK) {
            PomodoroSession session = sessionHistory.get(sessionHistory.size() - 1);
            session.endTime = System.currentTimeMillis();
            session.isDone = true;
            pomodoroCount++;
            Log.d(TAG, "Pomodoro count incremented to: " + pomodoroCount);
            setPhase(Phase.SHORT_BREAK);
        } else {
            pomodoroCount = 0;
            Log.d(TAG, "Break finished, resetting pomodoroCount to 0");
            setPhase(Phase.WORK);
        }
        syncSettingsToFirebase();
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
                    Toast.makeText(this, "Thời gian làm việc và nghỉ phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.d(TAG, "Setting Pomodoro Cycle to: " + cycle + " -> Work: " + newWorkDuration + "ms, Break: " + newBreakDuration + "ms");

                workDuration = newWorkDuration;
                shortBreakDuration = newBreakDuration;

                syncSettingsToFirebase();
                saveSettings();
                resetTimer();
                pomodoroText.setText(cycle);
            } else {
                Toast.makeText(this, "Định dạng không hợp lệ. Dùng phút/phút (ví dụ: 50/10)", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Định dạng chu kỳ Pomodoro không hợp lệ: " + cycle, e);
            Toast.makeText(this, "Định dạng số không hợp lệ", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi thiết lập chu kỳ Pomodoro: " + cycle, e);
            Toast.makeText(this, "Lỗi thiết lập chu kỳ", Toast.LENGTH_SHORT).show();
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

        builder.setTitle("Thời Gian Pomodoro Tùy Chỉnh");
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            try {
                int workMinutes = Integer.parseInt(etWorkTime.getText().toString());
                int breakMinutes = Integer.parseInt(etBreakTime.getText().toString());

                if (workMinutes <= 0 || breakMinutes <= 0) {
                    Toast.makeText(this, "Thời gian làm việc và nghỉ phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (workMinutes > 180 || breakMinutes > 60) {
                    Toast.makeText(this, "Vui lòng nhập thời gian hợp lý (Làm việc <= 180, Nghỉ <= 60)", Toast.LENGTH_SHORT).show();
                    return;
                }

                String newPreset = workMinutes + "/" + breakMinutes;
                setPomodoroCycle(newPreset);

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Vui lòng nhập số hợp lệ", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Lỗi lưu thời gian tùy chỉnh", e);
                Toast.makeText(this, "Lỗi lưu thời gian tùy chỉnh", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Hủy", null);
        builder.create().show();
    }

    private void showMusicSelectionDialog() {
        if (isFinishing()) {
            Log.w(TAG, "showMusicSelectionDialog: Activity is finishing, cannot show dialog");
            return;
        }

        Log.d(TAG, "showMusicSelectionDialog: Showing music selection dialog with " + musicNames.length + " sounds");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_music_control, null);
        builder.setView(dialogView);
        builder.setTitle("Nhạc Nền");

        RadioGroup musicRadioGroup = dialogView.findViewById(R.id.musicRadioGroup);
        ImageView muteToggleIcon = dialogView.findViewById(R.id.muteToggleIcon);
        SeekBar volumeSeekBar = dialogView.findViewById(R.id.volumeSeekBar);

        if (musicRadioGroup == null || muteToggleIcon == null || volumeSeekBar == null) {
            Log.e(TAG, "showMusicSelectionDialog: UI components missing in dialog_music_control.xml");
            return;
        }

        musicRadioGroup.removeAllViews();
        for (int i = 0; i < musicNames.length; i++) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setId(View.generateViewId());
            radioButton.setText(musicNames[i]);
            radioButton.setTag(i);
            musicRadioGroup.addView(radioButton);
            if (i == selectedMusicIndex) {
                radioButton.setChecked(true);
            }
        }

        volumeSeekBar.setProgress((int) (musicVolume * 100));
        muteToggleIcon.setImageResource(musicVolume > 0 ? R.drawable.ic_volume_on : R.drawable.ic_volume_off);

        final int[] previewSelectionIndex = {selectedMusicIndex};

        musicRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selectedRadio = group.findViewById(checkedId);
            if (selectedRadio != null) {
                int position = (Integer) selectedRadio.getTag();
                Log.d(TAG, "showMusicSelectionDialog: Radio button selected: " + position + " (" + musicNames[position] + ")");
                previewSelectionIndex[0] = position;
                stopMusic();
                if (position == 0) {
                    isMusicPlaying = false;
                } else {
                    playSound(musicResIds[position], musicNames[position], true);
                }
                muteToggleIcon.setImageResource(isMusicPlaying ? R.drawable.ic_volume_on : R.drawable.ic_volume_off);
            }
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
                volumeSeekBar.setProgress((int) (musicVolume * 100));
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(musicVolume, musicVolume);
                    if (!mediaPlayer.isPlaying() && previewSelectionIndex[0] > 0) {
                        try {
                            mediaPlayer.start();
                        } catch (IllegalStateException e) {
                            releaseMediaPlayer();
                            playSound(musicResIds[previewSelectionIndex[0]], musicNames[previewSelectionIndex[0]], true);
                        }
                    }
                    isMusicPlaying = true;
                    muteToggleIcon.setImageResource(R.drawable.ic_volume_on);
                } else if (previewSelectionIndex[0] > 0) {
                    playSound(musicResIds[previewSelectionIndex[0]], musicNames[previewSelectionIndex[0]], true);
                    muteToggleIcon.setImageResource(R.drawable.ic_volume_on);
                }
            }
            Log.d(TAG, "showMusicSelectionDialog: Mute toggle clicked. isMusicPlaying: " + isMusicPlaying + ", Volume: " + musicVolume);
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
                    Log.d(TAG, "showMusicSelectionDialog: Volume changed to: " + musicVolume);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        builder.setPositiveButton("Chọn", (dialog, which) -> {
            if (previewSelectionIndex[0] >= 0) {
                selectedMusicIndex = previewSelectionIndex[0];
                musicText.setText(musicNames[selectedMusicIndex]);
                syncSettingsToFirebase();
                saveSettings();
                Log.d(TAG, "showMusicSelectionDialog: Music selected: " + musicNames[selectedMusicIndex]);
                stopMusic();
                if (isTimerRunning && currentPhase == Phase.WORK && selectedMusicIndex > 0) {
                    playSound(musicResIds[selectedMusicIndex], musicNames[selectedMusicIndex], true);
                } else if (isTimerRunning && currentPhase == Phase.SHORT_BREAK && clockTickingSoundResId != 0) {
                    playSound(clockTickingSoundResId, "Clock Ticking", true);
                }
            }
        });

        builder.setNegativeButton("Đóng", (dialog, which) -> {
            Log.d(TAG, "showMusicSelectionDialog: Music selection cancelled.");
            stopMusic();
            if (isTimerRunning && currentPhase == Phase.WORK && selectedMusicIndex > 0) {
                playSound(musicResIds[selectedMusicIndex], musicNames[selectedMusicIndex], true);
            } else if (isTimerRunning && currentPhase == Phase.SHORT_BREAK && clockTickingSoundResId != 0) {
                playSound(clockTickingSoundResId, "Clock Ticking", true);
            }
        });

        builder.setOnDismissListener(dialog -> {
            Log.d(TAG, "showMusicSelectionDialog: Dialog dismissed");
            stopMusic();
            if (isTimerRunning && currentPhase == Phase.WORK && selectedMusicIndex > 0) {
                playSound(musicResIds[selectedMusicIndex], musicNames[selectedMusicIndex], true);
            } else if (isTimerRunning && currentPhase == Phase.SHORT_BREAK && clockTickingSoundResId != 0) {
                playSound(clockTickingSoundResId, "Clock Ticking", true);
            }
        });

        builder.create().show();
    }

    private void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.pause();
                isMusicPlaying = false;
                Log.d(TAG, "pauseMusic: Music paused for index: " + currentMusicIndex);
            } catch (IllegalStateException e) {
                Log.e(TAG, "pauseMusic: Error pausing MediaPlayer, releasing.", e);
                releaseMediaPlayer();
            } catch (Exception e) {
                Log.e(TAG, "pauseMusic: Generic error pausing MediaPlayer.", e);
                releaseMediaPlayer();
            }
        } else {
            Log.d(TAG, "pauseMusic: No music is playing or MediaPlayer is null");
        }
    }

    private void stopMusic() {
        Log.d(TAG, "stopMusic: Stopping and releasing music player for index: " + currentMusicIndex);
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
                Log.d(TAG, "releaseMediaPlayer: MediaPlayer released for index: " + currentMusicIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "releaseMediaPlayer: Exception during MediaPlayer release", e);
        } finally {
            mediaPlayer = null;
            isMusicPlaying = false;
            currentMusicIndex = -1;
        }
    }

    private void setupCallListener() {
        Log.d(TAG, "setupCallListener: Setting up call listener");
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Executor executor = Executors.newSingleThreadExecutor();
            class CallStateListener extends TelephonyCallback implements TelephonyCallback.CallStateListener {
                @Override
                public void onCallStateChanged(int state) {
                    switch (state) {
                        case TelephonyManager.CALL_STATE_RINGING:
                        case TelephonyManager.CALL_STATE_OFFHOOK:
                            if (isTimerRunning && !isPausedDueToCall) {
                                Log.d(TAG, "onCallStateChanged: Call detected, pausing timer");
                                pauseTimer();
                                isPausedDueToCall = true;
                            }
                            break;
                        case TelephonyManager.CALL_STATE_IDLE:
                            if (isPausedDueToCall) {
                                Log.d(TAG, "onCallStateChanged: Call ended, resuming timer");
                                startTimer();
                                isPausedDueToCall = false;
                            }
                            break;
                    }
                    Log.d(TAG, "onCallStateChanged: Call state changed: " + state);
                }
            }
            telephonyManager.registerTelephonyCallback(executor, new CallStateListener());
            Log.d(TAG, "setupCallListener: Call listener set up successfully using TelephonyCallback");
        } else {
            Log.e(TAG, "setupCallListener: TelephonyManager is null or API level does not support TelephonyCallback");
        }
    }

    private void createNotificationChannel() {
        Log.d(TAG, "createNotificationChannel: Creating notification channel");
        CharSequence name = "Pomodoro Notifications";
        String description = "Channel for Pomodoro timer notifications";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "createNotificationChannel: Notification channel created");
        } else {
            Log.e(TAG, "createNotificationChannel: NotificationManager is null");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: POST_NOTIFICATIONS permission granted");
            } else {
                Log.w(TAG, "onRequestPermissionsResult: POST_NOTIFICATIONS permission denied");
                Toast.makeText(this, "Quyền thông báo bị từ chối. Một số tính năng có thể không hoạt động.", Toast.LENGTH_LONG).show();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    Toast.makeText(this, "Vui lòng cấp quyền thông báo trong cài đặt ứng dụng.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        userId = intent.getStringExtra("userId");
        if (userId == null) {
            SharedPreferences prefs = getSharedPreferences(APP_PREFS_NAME, MODE_PRIVATE);
            userId = prefs.getString("userId", null);
        }
        if (intent.getBooleanExtra("fromNotification", false)) {
            Log.d(TAG, "onNewIntent: Detected notification tap with userId=" + userId);
            stopService(new Intent(this, PomodoroService.class));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Activity paused");
        if (isTimerRunning) {
            backgroundStartTime = System.currentTimeMillis();
            Intent serviceIntent = new Intent(this, PomodoroService.class);
            serviceIntent.putExtra("userId", userId);
            startService(serviceIntent);
            Log.d(TAG, "onPause: Started PomodoroService for background countdown with userId: " + userId);
        }
        syncSettingsToFirebase();
        saveSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Activity resumed");
        if (backgroundStartTime > 0) {
            long timeInBackground = System.currentTimeMillis() - backgroundStartTime;
            Log.d(TAG, "onResume: Time in background: " + timeInBackground + "ms");
            if (timeInBackground < 12000) {
                stopService(new Intent(this, PomodoroService.class));
                Log.d(TAG, "onResume: Stopped PomodoroService, resuming timer");
            }
            backgroundStartTime = 0;
        }
        updateButtonVisibility();
        updateFullscreenButtonVisibility();
        updateTimerText();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Activity destroyed");
        stopService(new Intent(this, PomodoroService.class));
        controlPanelHandler.removeCallbacksAndMessages(null);
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        releaseMediaPlayer();
        try {
            unregisterReceiver(pomodoroReceiver);
            Log.d(TAG, "onDestroy: Pomodoro receiver unregistered");
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "onDestroy: Pomodoro receiver was not registered", e);
        }
    }
}