package com.example.appmobilemanagementtimes;

import static android.provider.Settings.System.getString;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView signUpText;
    private ImageButton googleSignIn;
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    
    private final ActivityResultLauncher<Intent> signInLauncher = 
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                handleGoogleSignInResult(task);
            }
        });

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Cấu hình Google Sign-In với cài đặt để luôn hiển thị hộp thoại chọn tài khoản
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Khởi tạo các view
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        signUpText = findViewById(R.id.signUpText);
        googleSignIn = findViewById(R.id.googleSignIn);

        // Xử lý đăng nhập thông thường
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            loginUser(email, password);
        });

        // Xử lý đăng nhập Google
        googleSignIn.setOnClickListener(v -> {
            // Đăng xuất khỏi tài khoản hiện tại trước khi bắt đầu luồng đăng nhập mới
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                // Tạo lại client với cài đặt để luôn chọn tài khoản
                GoogleSignInOptions gsoWithPrompt = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();
                GoogleSignInClient clientWithPrompt = GoogleSignIn.getClient(LoginActivity.this, gsoWithPrompt);
                
                // Bắt đầu luồng đăng nhập với chế độ hiển thị hộp thoại chọn tài khoản
                Intent signInIntent = clientWithPrompt.getSignInIntent();
                signInLauncher.launch(signInIntent);
            });
        });

        // Xử lý chuyển sang trang đăng ký
        signUpText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
            finish();
        });
    }
    
    private void loginUser(String email, String password) {
        Toast.makeText(LoginActivity.this, "Đang đăng nhập...", Toast.LENGTH_SHORT).show();
        
        db.collection("Users")
            .whereEqualTo("email", email)
            .whereEqualTo("password", password)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot documents = task.getResult();
                    if (!documents.isEmpty()) {
                        String userId = documents.getDocuments().get(0).getId();
                        
                        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        prefs.edit().putString("userId", userId).apply();
                        
                        Intent intent = new Intent(LoginActivity.this, Today.class);
                        intent.putExtra("userId", userId);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Email hoặc mật khẩu không đúng", 
                                       Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + task.getException().getMessage(), 
                                   Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                // Log để debug
                Log.d("GoogleSignIn", "Đã nhận thông tin Google: " + account.getEmail());
                Toast.makeText(this, "Đăng nhập với: " + account.getEmail(), Toast.LENGTH_SHORT).show();
                
                // Đăng nhập Firebase với tài khoản Google
                firebaseAuthWithGoogle(account.getIdToken());
            }
        } catch (ApiException e) {
            // Thêm chi tiết hơn về lỗi
            String errorMessage = "Đăng nhập Google thất bại: " + e.getMessage();
            int statusCode = e.getStatusCode();
            errorMessage += " (Code: " + statusCode + ")";
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            
            // Log chi tiết hơn
            Log.e("GoogleSignIn", "SignIn failed: " + e.getMessage() + ", code: " + statusCode, e);
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
                    Toast.makeText(LoginActivity.this, "Xác thực Google thất bại", 
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
                        // User đã tồn tại, lấy userId và chuyển màn hình
                        String userId = task.getResult().getDocuments().get(0).getId();
                        
                        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        prefs.edit().putString("userId", userId).apply();
                        
                        Intent intent = new Intent(LoginActivity.this, Today.class);
                        intent.putExtra("userId", userId);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Kiểm tra tài khoản thất bại", 
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
                        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        prefs.edit().putString("userId", userId).apply();
                        
                        Intent intent = new Intent(LoginActivity.this, Today.class);
                        intent.putExtra("userId", userId);
                        startActivity(intent);
                        finish();
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(LoginActivity.this, "Tạo tài khoản Google thất bại", 
                              Toast.LENGTH_SHORT).show();
            });
    }
}