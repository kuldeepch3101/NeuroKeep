package com.example.learnkeep;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.learnkeep.api.ApiClient;
import com.example.learnkeep.api.ApiService;
import com.example.learnkeep.api.LoginRequest;
import com.example.learnkeep.api.LoginResponse;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    ImageButton ibTogglePassword;
    boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        ibTogglePassword = findViewById(R.id.ibTogglePassword);

        TextView txtSignup = findViewById(R.id.txtSignup);
        txtSignup.setOnClickListener(v ->
                startActivity(new Intent(this, SignupActivity.class)));

        findViewById(R.id.txtForgotPassword).setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        // ── PASSWORD VISIBILITY TOGGLE ──────────────────────────────────
        ibTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                // Show password
                etPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ibTogglePassword.setImageResource(R.drawable.ic_login_visibility_off);
            } else {
                // Hide password
                etPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ibTogglePassword.setImageResource(R.drawable.ic_login_visibility);
            }
            // Keep cursor at end
            etPassword.setSelection(etPassword.getText().length());
        });

        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {
        String email    = etEmail.getText().toString().trim().toLowerCase();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        LoginRequest request  = new LoginRequest(email, password);

        apiService.login(request).enqueue(new retrofit2.Callback<LoginResponse>() {
            @Override
            public void onResponse(retrofit2.Call<LoginResponse> call,
                                   retrofit2.Response<LoginResponse> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().success) {

                    SessionManager session = new SessionManager(LoginActivity.this);
                    session.saveLogin(
                            response.body().name,
                            response.body().email,
                            response.body().token);

                    // After saving session on successful login:
                    SyncManager.restoreFromCloud(LoginActivity.this, new SyncManager.SyncCallback() {
                        @Override
                        public void onSuccess(String msg) {
                            Log.d("Sync", "Data restored from cloud");
                            // Now navigate to MainActivity
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                        @Override
                        public void onFailure(String err) {
                            Log.e("Sync", "Restore failed: " + err);
                            // Still go to MainActivity even if restore fails (offline scenario)
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                    });

                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Invalid email or password", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this,
                        "Server error. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
