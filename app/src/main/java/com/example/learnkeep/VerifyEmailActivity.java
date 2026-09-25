package com.example.learnkeep;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.learnkeep.api.ApiClient;
import com.example.learnkeep.api.ApiService;
import com.example.learnkeep.api.OtpSignupRequest;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyEmailActivity extends AppCompatActivity {

    EditText[] otpBoxes;
    TextView tvEmailHint, tvCountdown, btnResend, btnVerify;
    String name, email, password;
    CountDownTimer countDownTimer;
    boolean canResend = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);

        // Get data from SignupActivity
        name     = getIntent().getStringExtra("name");
        email    = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");

        // Views
        tvEmailHint = findViewById(R.id.tvEmailHint);
        tvCountdown = findViewById(R.id.tvCountdown);
        btnResend   = findViewById(R.id.btnResend);
        btnVerify   = findViewById(R.id.btnVerify);

        tvEmailHint.setText("We've sent a 6-digit code to " + email);

        // OTP boxes
        otpBoxes = new EditText[]{
            findViewById(R.id.etOtp1),
            findViewById(R.id.etOtp2),
            findViewById(R.id.etOtp3),
            findViewById(R.id.etOtp4),
            findViewById(R.id.etOtp5),
            findViewById(R.id.etOtp6)
        };

        setupOtpBoxes();
        startCountdown();

        btnVerify.setOnClickListener(v -> verifyAndCreateAccount());

        btnResend.setOnClickListener(v -> {
            if (canResend) resendOtp();
            else Toast.makeText(this, "Please wait before resending", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    // ── OTP BOX AUTO-ADVANCE & BACKSPACE ──────────────────────────────
    private void setupOtpBoxes() {
        for (int i = 0; i < 6; i++) {
            final int idx = i;

            otpBoxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int c, int a) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Move to next box when a digit is entered
                    if (s.length() == 1 && idx < 5) {
                        otpBoxes[idx + 1].requestFocus();
                    }
                }

                @Override public void afterTextChanged(Editable s) {}
            });

            // Move to previous box on backspace if current box is empty
            otpBoxes[i].setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && keyCode == KeyEvent.KEYCODE_DEL
                        && otpBoxes[idx].getText().toString().isEmpty()
                        && idx > 0) {
                    otpBoxes[idx - 1].requestFocus();
                    otpBoxes[idx - 1].setText("");
                    return true;
                }
                return false;
            });
        }

        // Auto-focus first box
        otpBoxes[0].requestFocus();
    }

    private String collectOtp() {
        StringBuilder sb = new StringBuilder();
        for (EditText box : otpBoxes) sb.append(box.getText().toString().trim());
        return sb.toString();
    }

    // ── VERIFY OTP & CREATE ACCOUNT ───────────────────────────────────
    private void verifyAndCreateAccount() {
        String otp = collectOtp();
        if (otp.length() < 6) {
            Toast.makeText(this, "Please enter all 6 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        btnVerify.setEnabled(false);
        ((android.widget.Button) btnVerify).setText("Verifying...");

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        OtpSignupRequest request = new OtpSignupRequest(name, email, password, otp);

        api.signupWithOtp(request).enqueue(new Callback<com.example.learnkeep.api.LoginResponse>() {
            @Override
            public void onResponse(Call<com.example.learnkeep.api.LoginResponse> call,
                                   Response<com.example.learnkeep.api.LoginResponse> response) {
                btnVerify.setEnabled(true);
                ((android.widget.Button) btnVerify).setText("Verify  →");

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().success) {

                    Toast.makeText(VerifyEmailActivity.this,
                            "✅ Account created! Please log in.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(VerifyEmailActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();

                } else {
                    clearOtpBoxes();
                    Toast.makeText(VerifyEmailActivity.this,
                            "❌ Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.example.learnkeep.api.LoginResponse> call, Throwable t) {
                btnVerify.setEnabled(true);
                ((android.widget.Button) btnVerify).setText("Verify  →");
                Toast.makeText(VerifyEmailActivity.this,
                        "Server error. Check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── COUNTDOWN TIMER ───────────────────────────────────────────────
    private void startCountdown() {
        canResend = false;
        btnResend.setAlpha(0.4f);

        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(60_000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secs = millisUntilFinished / 1000;
                tvCountdown.setText(String.format(Locale.getDefault(),
                        "Resend in 0:%02d", secs));
            }
            @Override
            public void onFinish() {
                tvCountdown.setText("You can resend now");
                canResend = true;
                btnResend.setAlpha(1.0f);
            }
        }.start();
    }

    private void resendOtp() {
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        api.sendSignupOtp(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call,
                                   Response<Map<String, Object>> response) {
                Toast.makeText(VerifyEmailActivity.this,
                        "OTP resent to " + email, Toast.LENGTH_SHORT).show();
                clearOtpBoxes();
                startCountdown();
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(VerifyEmailActivity.this,
                        "Failed to resend. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearOtpBoxes() {
        for (EditText box : otpBoxes) box.setText("");
        otpBoxes[0].requestFocus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
