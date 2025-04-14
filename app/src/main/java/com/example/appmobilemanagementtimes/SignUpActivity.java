package com.example.appmobilemanagementtimes;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import android.util.Log;

public class SignUpActivity extends AppCompatActivity {
    private EditText displayNameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private Button signUpButton;
    private ImageButton googleSignUp;
    private ImageButton facebookSignUp;
    private TextView signInText;
    private CheckBox agreeCheckbox;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    
    private final ActivityResultLauncher<Intent> signInLauncher = 
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                handleGoogleSignInResult(task);
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Cấu hình Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        displayNameInput = findViewById(R.id.displayNameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        signUpButton = findViewById(R.id.signUpButton);
        googleSignUp = findViewById(R.id.googleSignUp);
        facebookSignUp = findViewById(R.id.facebookSignUp);
        signInText = findViewById(R.id.signInText);
        agreeCheckbox = findViewById(R.id.agreeCheckbox);

        signUpButton.setOnClickListener(v -> {
            String displayName = displayNameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (displayName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!agreeCheckbox.isChecked()) {
                Toast.makeText(SignUpActivity.this, "Vui lòng đồng ý với điều khoản", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUser(displayName, email, password);
        });
        
        googleSignUp.setOnClickListener(v -> {
            // Đăng xuất khỏi tài khoản hiện tại trước khi bắt đầu luồng đăng nhập mới
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                // Tạo lại client với cài đặt để luôn chọn tài khoản
                GoogleSignInOptions gsoWithPrompt = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();
                GoogleSignInClient clientWithPrompt = GoogleSignIn.getClient(SignUpActivity.this, gsoWithPrompt);
                
                // Bắt đầu luồng đăng nhập với chế độ hiển thị hộp thoại chọn tài khoản
                Intent signInIntent = clientWithPrompt.getSignInIntent();
                signInLauncher.launch(signInIntent);
            });
        });

        signInText.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
    
    private void registerUser(String displayName, String email, String password) {
        Toast.makeText(SignUpActivity.this, "Đang đăng ký...", Toast.LENGTH_SHORT).show();
        
        Map<String, Object> user = new HashMap<>();
        user.put("displayName", displayName);
        user.put("email", email);
        user.put("password", password);
        user.put("avatar", null);
        user.put("createdAt", new Timestamp(new Date()));
        
        // Tạo map settings
        Map<String, Object> settings = new HashMap<>();
        settings.put("notifications", true);
        settings.put("theme", "light");
        settings.put("language", "vi");
        user.put("settings", settings);
        
        db.collection("Users")
            .add(user)
            .addOnSuccessListener(documentReference -> {
                String userId = documentReference.getId();
                
                db.collection("Users").document(userId)
                    .update("userId", userId)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(SignUpActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                        
                        getSharedPreferences("AppPrefs", MODE_PRIVATE)
                            .edit()
                            .putString("userId", userId)
                            .apply();
                        
                        Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(SignUpActivity.this, "Cập nhật userId thất bại: " + e.getMessage(), 
                                      Toast.LENGTH_LONG).show();
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(SignUpActivity.this, "Đăng ký thất bại: " + e.getMessage(), 
                              Toast.LENGTH_LONG).show();
            });
    }
    
    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                // Log để debug
                Log.d("GoogleSignIn", "Đã nhận thông tin Google: " + account.getEmail());
                Toast.makeText(this, "Đăng ký với: " + account.getEmail(), Toast.LENGTH_SHORT).show();
                
                // Đăng nhập Firebase với tài khoản Google
                firebaseAuthWithGoogle(account.getIdToken());
            }
        } catch (ApiException e) {
            // Chi tiết lỗi
            String errorMessage = "Đăng ký Google thất bại: " + e.getMessage();
            int statusCode = e.getStatusCode();
            errorMessage += " (Code: " + statusCode + ")";
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            
            // Log chi tiết
            Log.e("GoogleSignIn", "SignUp failed: " + e.getMessage() + ", code: " + statusCode, e);
        }
    }
    
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Đăng nhập Firebase Auth thành công
                    FirebaseAuth auth = FirebaseAuth.getInstance();
                    if (auth.getCurrentUser() != null) {
                        String email = auth.getCurrentUser().getEmail();
                        String displayName = auth.getCurrentUser().getDisplayName();
                        String photoUrl = auth.getCurrentUser().getPhotoUrl() != null ? 
                                auth.getCurrentUser().getPhotoUrl().toString() : null;
                        
                        // Kiểm tra xem user đã tồn tại chưa
                        checkUserExistsInFirestore(email, displayName, photoUrl);
                    }
                } else {
                    Toast.makeText(SignUpActivity.this, "Xác thực Google thất bại", 
                                  Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void checkUserExistsInFirestore(String email, String displayName, String photoUrl) {
        db.collection("Users")
            .whereEqualTo("email", email)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (task.getResult().isEmpty()) {
                        // Tạo user mới nếu chưa tồn tại
                        createGoogleUserInFirestore(email, displayName, photoUrl);
                    } else {
                        // User đã tồn tại, thông báo và chuyển sang đăng nhập
                        Toast.makeText(SignUpActivity.this, "Tài khoản đã tồn tại, vui lòng đăng nhập", 
                                      Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    Toast.makeText(SignUpActivity.this, "Kiểm tra tài khoản thất bại", 
                                  Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void createGoogleUserInFirestore(String email, String displayName, String photoUrl) {
        Map<String, Object> user = new HashMap<>();
        user.put("displayName", displayName);
        user.put("email", email);
        user.put("password", "google_auth"); // Đánh dấu là tài khoản Google
        user.put("avatar", photoUrl);
        user.put("createdAt", new Timestamp(new Date()));
        
        // Tạo map settings
        Map<String, Object> settings = new HashMap<>();
        settings.put("notifications", true);
        settings.put("theme", "light");
        settings.put("language", "vi");
        user.put("settings", settings);
        
        db.collection("Users")
            .add(user)
            .addOnSuccessListener(documentReference -> {
                String userId = documentReference.getId();
                
                db.collection("Users").document(userId)
                    .update("userId", userId)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(SignUpActivity.this, "Đăng ký Google thành công!", Toast.LENGTH_SHORT).show();
                        
                        getSharedPreferences("AppPrefs", MODE_PRIVATE)
                            .edit()
                            .putString("userId", userId)
                            .apply();
                        
                        Intent intent = new Intent(SignUpActivity.this, Today.class);
                        intent.putExtra("userId", userId);
                        startActivity(intent);
                        finish();
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(SignUpActivity.this, "Tạo tài khoản Google thất bại", 
                              Toast.LENGTH_SHORT).show();
            });
    }
} 