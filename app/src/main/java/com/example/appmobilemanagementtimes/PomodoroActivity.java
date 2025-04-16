package com.example.appmobilemanagementtimes;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.Locale;

public class PomodoroActivity extends AppCompatActivity {

    public enum Phase {
        WORK, SHORT_BREAK, LONG_BREAK
    }

    private static final String TAG = "PomodoroActivity";
    private static final String PREFS_NAME = "PomodoroPrefs";
    private static final String WORK_DURATION_KEY = "workDuration";
    private static final String BREAK_DURATION_KEY = "breakDuration";
    private static final String SELECTED_MUSIC_KEY = "selectedMusic";
    private static final String POMODORO_COUNT_KEY = "pomodoroCount";
    private static final String CURRENT_PHASE_KEY = "currentPhase";

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
    private ConstraintLayout mainLayout, musicColumn;
    private SoundPool soundPool;
    private int notificationSoundId;
    private boolean isSoundPoolLoaded = false;

    private Phase currentPhase = Phase.WORK;
    private int pomodoroCount = 0;

    private MediaPlayer mediaPlayer;
    private boolean isMusicPlaying = false;
    private float musicVolume = 0.5f;
    private int currentMusicIndex = -1;
    private int selectedMusicIndex = -1;
    private final String[] musicNames = {
            "None", "Clock ticking", "Emotional Piano", "Light Rain",
            "Rain", "Relaxing Piano", "The Ocean"
    };
    private final int[] musicResIds = {
            0, R.raw.clock_ticking, R.raw.emotional_piano_music,
            R.raw.light_rain, R.raw.rain_sound,
            R.raw.relaxing_piano, R.raw.the_ocean_sound
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pomodoro_main);

        initializeSoundPool();
        initializeViews();
        setupListeners();
        loadSettings();

        if (!isTimerRunning) {
            setPhase(currentPhase);
        }

        updateButtonVisibility();
    }

    private void initializeSoundPool() {
        try {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(1)
                    .setAudioAttributes(attributes)
                    .build();
            soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
                isSoundPoolLoaded = status == 0;
                Log.d(TAG, "SoundPool load status: " + (isSoundPoolLoaded ? "Success" : "Failed"));
            });

            if (isResourceAvailable(R.raw.notification_sound)) {
                notificationSoundId = soundPool.load(this, R.raw.notification_sound, 1);
            } else {
                Log.e(TAG, "Notification sound resource not found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing SoundPool", e);
        }
    }

    private boolean isResourceAvailable(int resId) {
        try (AssetFileDescriptor fd = getResources().openRawResourceFd(resId)) {
            return fd != null;
        } catch (Exception e) {
            Log.e(TAG, "Resource check failed for ID: " + resId, e);
            return false;
        }
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

        progressBar.setMax(100);
        progressBar.setProgress(0);
    }

    private void setupListeners() {
        startButton.setOnClickListener(v -> startTimer());
        pauseButton.setOnClickListener(v -> pauseTimer());
        stopButton.setOnClickListener(v -> resetTimer());
        pomodoroDropdown.setOnClickListener(v -> showPomodoroOptions());
        musicColumn.setOnClickListener(v -> showMusicSelectionDialog());
    }

    private void loadSettings() {
        SharedPreferences sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        workDuration = sharedPref.getLong(WORK_DURATION_KEY, 25 * 60 * 1000);
        shortBreakDuration = sharedPref.getLong(BREAK_DURATION_KEY, 5 * 60 * 1000);
        selectedMusicIndex = sharedPref.getInt(SELECTED_MUSIC_KEY, -1);
        pomodoroCount = sharedPref.getInt(POMODORO_COUNT_KEY, 0);

        int phaseIndex = sharedPref.getInt(CURRENT_PHASE_KEY, Phase.WORK.ordinal());
        if (phaseIndex >= 0 && phaseIndex < Phase.values().length) {
            currentPhase = Phase.values()[phaseIndex];
        } else {
            currentPhase = Phase.WORK;
            Log.e(TAG, "Invalid phase index loaded: " + phaseIndex);
        }

        String currentSettingText = (workDuration / 60000) + "/" + (shortBreakDuration / 60000);
        pomodoroText.setText(currentSettingText);

        Log.d(TAG, "Settings loaded: Work=" + workDuration + "ms, Break=" + shortBreakDuration + "ms, Phase=" + currentPhase + ", Count=" + pomodoroCount);

        setPhase(currentPhase);
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putLong(WORK_DURATION_KEY, workDuration);
        editor.putLong(BREAK_DURATION_KEY, shortBreakDuration);
        editor.putInt(SELECTED_MUSIC_KEY, selectedMusicIndex);
        editor.putInt(POMODORO_COUNT_KEY, pomodoroCount);
        editor.putInt(CURRENT_PHASE_KEY, currentPhase.ordinal());
        editor.apply();
        Log.d(TAG, "Settings saved: Work=" + workDuration + "ms, Break=" + shortBreakDuration + "ms, Phase=" + currentPhase + ", Count=" + pomodoroCount);
    }

    private void startTimer() {
        if (isTimerRunning) return;

        if (selectedMusicIndex > 0 && !isMusicPlaying) {
            playMusic(selectedMusicIndex);
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

                new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> {
                    playNotificationSound();
                    onTimerFinish();
                    updateButtonVisibility();
                }, 500);
            }
        }.start();

        isTimerRunning = true;
        updateButtonVisibility();
    }

    private void pauseTimer() {
        if (!isTimerRunning || countDownTimer == null) return;

        countDownTimer.cancel();
        isTimerRunning = false;
        updateButtonVisibility();
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
        Log.d(TAG, "Phase explicitly set to WORK, pomodoroCount reset to 0");

        setPhase(Phase.WORK);
        saveSettings();
        updateButtonVisibility();
        stopMusic();

        Log.d(TAG, "Timer reset complete. UI should show work duration: " + (workDuration / 1000) + "s");
    }

    private void updateButtonVisibility() {
        if (isTimerRunning) {
            startButton.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);
            stopButton.setVisibility(View.VISIBLE);
        } else {
            startButton.setVisibility(View.VISIBLE);
            pauseButton.setVisibility(View.GONE);
            if (timeLeftInMillis > 0 && timeLeftInMillis < totalDuration) {
                stopButton.setVisibility(View.VISIBLE);
            } else {
                stopButton.setVisibility(View.GONE);
            }
        }
    }

    private void updateTimerText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        timerText.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void updateProgressBar() {
        if (totalDuration > 0) {
            int progress = (int) (((double) (totalDuration - timeLeftInMillis) / totalDuration) * 100);
            progressBar.setProgress(progress, true);
        } else {
            progressBar.setProgress(0, true);
        }
    }

    private void playNotificationSound() {
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "Activity is finishing/destroyed, skipping notification sound.");
            return;
        }
        if (!isSoundPoolLoaded || soundPool == null) {
            Log.e(TAG, "SoundPool not loaded or null, cannot play notification sound.");
            return;
        }
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null && audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
                Log.w(TAG, "Media volume is muted, notification sound might not be heard.");
            }
            soundPool.play(notificationSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
            Log.d(TAG, "Notification sound played.");
        } catch (Exception e) {
            Log.e(TAG, "Error playing notification sound", e);
        }
    }

    private void onTimerFinish() {
        Log.d(TAG, "Timer finished for phase: " + currentPhase);
        if (currentPhase == Phase.WORK) {
            pomodoroCount++;
            Log.d(TAG, "Pomodoro count incremented to: " + pomodoroCount);
            setPhase(Phase.SHORT_BREAK);
        } else {
            setPhase(Phase.WORK);
        }
        saveSettings();
    }

    private void setPhase(Phase phase) {
        currentPhase = phase;
        switch (currentPhase) {
            case WORK:
                totalDuration = workDuration;
                Log.d(TAG, "Setting phase to WORK. Duration: " + totalDuration + "ms");
                break;
            case SHORT_BREAK:
                totalDuration = shortBreakDuration;
                Log.d(TAG, "Setting phase to SHORT_BREAK. Duration: " + totalDuration + "ms");
                break;
            case LONG_BREAK:
                totalDuration = longBreakDuration;
                Log.d(TAG, "Setting phase to LONG_BREAK. Duration: " + totalDuration + "ms");
                break;
        }
        timeLeftInMillis = totalDuration;
        updateTimerText();
        progressBar.setProgress(0, true);
    }

    private void setPomodoroCycle(String cycle) {
        try {
            String[] parts = cycle.split("/");
            if (parts.length == 2) {
                long newWorkDuration = Long.parseLong(parts[0]) * 60_000L;
                long newBreakDuration = Long.parseLong(parts[1]) * 60_000L;

                if (newWorkDuration <= 0 || newBreakDuration <= 0) {
                    Toast.makeText(this, "Thời gian làm việc và giải lao phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.d(TAG, "Đặt chu kỳ Pomodoro: " + cycle + " -> Work: " + newWorkDuration + "ms, Break: " + newBreakDuration + "ms");

                workDuration = newWorkDuration;
                shortBreakDuration = newBreakDuration;

                saveSettings();
                resetTimer();
                pomodoroText.setText(cycle);
            } else {
                Toast.makeText(this, "Định dạng sai. Dùng phút/phút (ví dụ: 50/10)", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Định dạng chu kỳ Pomodoro sai: " + cycle, e);
            Toast.makeText(this, "Số không hợp lệ", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi đặt chu kỳ Pomodoro: " + cycle, e);
            Toast.makeText(this, "Lỗi khi đặt chu kỳ", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPomodoroOptions() {
        if (isFinishing()) return;

        PopupMenu popup = new PopupMenu(this, pomodoroDropdown);
        for (String option : pomodoroOptions) {
            popup.getMenu().add(option);
        }
        popup.getMenu().add("+ Tùy chỉnh");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("+ Tùy chỉnh")) {
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

        builder.setTitle("Tùy chỉnh thời gian Pomodoro");
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            try {
                int workMinutes = Integer.parseInt(etWorkTime.getText().toString());
                int breakMinutes = Integer.parseInt(etBreakTime.getText().toString());

                if (workMinutes <= 0 || breakMinutes <= 0) {
                    Toast.makeText(this, "Thời gian làm việc và giải lao phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (workMinutes > 180 || breakMinutes > 60) {
                    Toast.makeText(this, "Vui lòng nhập thời gian hợp lý (Làm việc <= 180, Giải lao <= 60)", Toast.LENGTH_SHORT).show();
                    return;
                }

                String newPreset = workMinutes + "/" + breakMinutes;
                setPomodoroCycle(newPreset);

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Vui lòng nhập số hợp lệ", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi lưu thời gian tùy chỉnh", e);
                Toast.makeText(this, "Lỗi khi lưu thời gian", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Hủy", null);
        builder.create().show();
    }

    private void showMusicSelectionDialog() {
        if (isFinishing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_music_control, null);
        builder.setView(dialogView);
        builder.setTitle("Nhạc nền");

        ListView musicListView = dialogView.findViewById(R.id.musicListView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_single_choice, musicNames);
        musicListView.setAdapter(adapter);
        musicListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        ImageView muteToggleIcon = dialogView.findViewById(R.id.muteToggleIcon);
        SeekBar volumeSeekBar = dialogView.findViewById(R.id.volumeSeekBar);

        volumeSeekBar.setProgress((int) (musicVolume * 100));
        muteToggleIcon.setImageResource(musicVolume > 0 ? R.drawable.ic_volume_on : R.drawable.ic_volume_off);

        int initialSelection = selectedMusicIndex >= 0 ? selectedMusicIndex : 0;
        musicListView.setItemChecked(initialSelection, true);
        final int[] previewSelectionIndex = {initialSelection};

        musicListView.setOnItemClickListener((parent, view, position, id) -> {
            Log.d(TAG, "Nhạc được chọn: " + position + " (" + musicNames[position] + ")");
            previewSelectionIndex[0] = position;
            playMusicForPreview(position);
            muteToggleIcon.setImageResource(isMusicPlaying ? R.drawable.ic_volume_on : R.drawable.ic_volume_off);
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
                    if (!mediaPlayer.isPlaying() && previewSelectionIndex[0] > 0) {
                        try {
                            mediaPlayer.start();
                        } catch (IllegalStateException e) {
                            releaseMediaPlayer();
                            playMusicForPreview(previewSelectionIndex[0]);
                        }
                    }
                    isMusicPlaying = true;
                    muteToggleIcon.setImageResource(R.drawable.ic_volume_on);
                } else if (previewSelectionIndex[0] > 0) {
                    playMusicForPreview(previewSelectionIndex[0]);
                    muteToggleIcon.setImageResource(R.drawable.ic_volume_on);
                } else {
                    muteToggleIcon.setImageResource(R.drawable.ic_volume_off);
                }
            }
            Log.d(TAG, "Nút mute được nhấn. isMusicPlaying: " + isMusicPlaying + ", Volume: " + musicVolume);
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
                    Log.d(TAG, "Âm lượng thay đổi thành: " + musicVolume);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        builder.setPositiveButton("Chọn", (dialog, which) -> {
            selectedMusicIndex = previewSelectionIndex[0];
            saveSettings();
            Log.d(TAG, "Nhạc được chọn: " + selectedMusicIndex + " (" + musicNames[selectedMusicIndex] + ")");
            if (selectedMusicIndex <= 0) {
                stopMusic();
            } else {
                if (currentMusicIndex != selectedMusicIndex) {
                    stopMusic();
                    if (isTimerRunning) {
                        playMusic(selectedMusicIndex);
                    }
                }
            }
            currentMusicIndex = selectedMusicIndex;
        });

        builder.setNegativeButton("Đóng", (dialog, which) -> {
            Log.d(TAG, "Hủy chọn nhạc.");
            if (mediaPlayer != null && currentMusicIndex != selectedMusicIndex) {
                stopMusic();
            }
            currentMusicIndex = selectedMusicIndex;
        });

        builder.setOnDismissListener(dialog -> {
            if (mediaPlayer != null && currentMusicIndex != selectedMusicIndex) {
                stopMusic();
                currentMusicIndex = selectedMusicIndex;
            }
        });

        builder.create().show();
    }

    private void playMusicForPreview(int position) {
        releaseMediaPlayer();
        currentMusicIndex = position;

        if (position <= 0 || musicResIds[position] == 0) {
            isMusicPlaying = false;
            Log.d(TAG, "Dừng preview (Không chọn hoặc resId không hợp lệ).");
            return;
        }

        if (!isResourceAvailable(musicResIds[position])) {
            Toast.makeText(this, "Không tìm thấy file nhạc: " + musicNames[position], Toast.LENGTH_SHORT).show();
            isMusicPlaying = false;
            currentMusicIndex = -1;
            return;
        }

        try {
            mediaPlayer = MediaPlayer.create(this, musicResIds[position]);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.setVolume(musicVolume, musicVolume);
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "Lỗi MediaPlayer khi preview: what=" + what + ", extra=" + extra + " cho " + musicNames[position]);
                    releaseMediaPlayer();
                    Toast.makeText(PomodoroActivity.this, "Lỗi phát nhạc", Toast.LENGTH_SHORT).show();
                    return true;
                });
                mediaPlayer.start();
                isMusicPlaying = true;
                Log.d(TAG, "Bắt đầu preview cho: " + musicNames[position]);
            } else {
                Log.e(TAG, "MediaPlayer.create trả về null cho: " + musicNames[position]);
                Toast.makeText(this, "Không thể tạo trình phát nhạc", Toast.LENGTH_SHORT).show();
                isMusicPlaying = false;
                currentMusicIndex = -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi phát preview nhạc: " + musicNames[position], e);
            Toast.makeText(this, "Lỗi phát nhạc", Toast.LENGTH_SHORT).show();
            isMusicPlaying = false;
            currentMusicIndex = -1;
            releaseMediaPlayer();
        }
    }

    private void playMusic(int position) {
        if (position == currentMusicIndex && mediaPlayer != null && mediaPlayer.isPlaying()) {
            Log.d(TAG, "Nhạc đã phát: " + musicNames[position]);
            return;
        }
        if (position == currentMusicIndex && mediaPlayer != null && !mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.setVolume(musicVolume, musicVolume);
                mediaPlayer.start();
                isMusicPlaying = true;
                Log.d(TAG, "Tiếp tục nhạc: " + musicNames[position]);
                return;
            } catch (IllegalStateException e) {
                Log.e(TAG, "Lỗi khi tiếp tục MediaPlayer, đang giải phóng.", e);
                releaseMediaPlayer();
            }
        }

        releaseMediaPlayer();
        currentMusicIndex = position;

        if (position <= 0 || musicResIds[position] == 0) {
            isMusicPlaying = false;
            Log.d(TAG, "Dừng phát nhạc (Không chọn hoặc resId không hợp lệ).");
            return;
        }

        if (!isResourceAvailable(musicResIds[position])) {
            Log.e(TAG, "Không tìm thấy tài nguyên nhạc: " + musicNames[position]);
            isMusicPlaying = false;
            currentMusicIndex = -1;
            return;
        }

        try {
            mediaPlayer = MediaPlayer.create(this, musicResIds[position]);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.setVolume(musicVolume, musicVolume);
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "Lỗi MediaPlayer khi phát: what=" + what + ", extra=" + extra + " cho " + musicNames[position]);
                    releaseMediaPlayer();
                    return true;
                });
                mediaPlayer.start();
                isMusicPlaying = true;
                Log.d(TAG, "Bắt đầu nhạc: " + musicNames[position]);
            } else {
                Log.e(TAG, "MediaPlayer.create trả về null khi phát: " + musicNames[position]);
                isMusicPlaying = false;
                currentMusicIndex = -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi phát nhạc: " + musicNames[position], e);
            isMusicPlaying = false;
            currentMusicIndex = -1;
            releaseMediaPlayer();
        }
    }

    private void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.pause();
                isMusicPlaying = false;
                Log.d(TAG, "Nhạc đã tạm dừng.");
            } catch (IllegalStateException e) {
                Log.e(TAG, "Lỗi khi tạm dừng MediaPlayer, đang giải phóng.", e);
                releaseMediaPlayer();
            } catch (Exception e) {
                Log.e(TAG, "Lỗi chung khi tạm dừng MediaPlayer.", e);
                releaseMediaPlayer();
            }
        }
    }

    private void stopMusic() {
        Log.d(TAG, "Dừng và giải phóng trình phát nhạc.");
        releaseMediaPlayer();
        currentMusicIndex = -1;
    }

    private void releaseMediaPlayer() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
                Log.d(TAG, "MediaPlayer đã được giải phóng.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi giải phóng MediaPlayer", e);
        } finally {
            mediaPlayer = null;
            isMusicPlaying = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isTimerRunning) {
            pauseTimer();
        }
        saveSettings();
        Log.d(TAG, "onPause được gọi. Đã lưu cài đặt.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume được gọi.");
        updateButtonVisibility();
        updateTimerText();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy được gọi.");
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        releaseMediaPlayer();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
            Log.d(TAG, "SoundPool đã được giải phóng.");
        }
    }
}