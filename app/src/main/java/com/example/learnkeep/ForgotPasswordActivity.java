package com.example.learnkeep;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.learnkeep.api.ApiClient;
import com.example.learnkeep.api.ApiService;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    // IDs kept exactly as original — etOtp, etNewPassword, btnReset
    // are kept as hidden views in the layout so this file compiles unchanged,
    // but the real OTP flow happens in ResetPasswordActivity.
    EditText etEmail, etOtp, etNewPassword;
    Button btnSendOtp, btnReset;
    ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail       = findViewById(R.id.etEmail);
        etOtp         = findViewById(R.id.etOtp);          // hidden in new layout
        etNewPassword = findViewById(R.id.etNewPassword);  // hidden in new layout
        btnSendOtp    = findViewById(R.id.btnSendOtp);
        btnReset      = findViewById(R.id.btnReset);       // hidden in new layout

        apiService = ApiClient.getClient(this).create(ApiService.class);

        // Back button
        if (findViewById(R.id.btnBackForgot) != null) {
            findViewById(R.id.btnBackForgot).setOnClickListener(v -> finish());
        }

        btnSendOtp.setOnClickListener(v -> sendOtp());

        // btnReset is hidden but wired up for backward compatibility
        btnReset.setOnClickListener(v -> { /* handled by ResetPasswordActivity */ });
    }

    private void sendOtp() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Enter your email");
            etEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return;
        }

        btnSendOtp.setEnabled(false);
        btnSendOtp.setText("Sending...");

        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        apiService.sendForgotOtp(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call,
                                   Response<Map<String, Object>> response) {
                btnSendOtp.setEnabled(true);
                btnSendOtp.setText("Send OTP  →");

                Toast.makeText(ForgotPasswordActivity.this,
                        "OTP sent to " + email, Toast.LENGTH_SHORT).show();

                // Navigate to ResetPasswordActivity with the email
                Intent intent = new Intent(ForgotPasswordActivity.this,
                        ResetPasswordActivity.class);
                intent.putExtra("email", email);
                startActivity(intent);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnSendOtp.setEnabled(true);
                btnSendOtp.setText("Send OTP  →");
                Toast.makeText(ForgotPasswordActivity.this,
                        "Server error. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // resetPassword kept for backward compatibility (not called in new flow)
    private void resetPassword() {
        String email       = etEmail.getText().toString().trim();
        String otp         = etOtp.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        Map<String, String> body = new HashMap<>();
        body.put("email",       email);
        body.put("otp",         otp);
        body.put("newPassword", newPassword);

        apiService.resetPassword(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call,
                                   Response<Map<String, Object>> response) {
                Toast.makeText(ForgotPasswordActivity.this,
                        "Password Reset Successful", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(ForgotPasswordActivity.this,
                        "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
