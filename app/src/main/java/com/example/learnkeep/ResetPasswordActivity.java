package com.example.learnkeep;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.learnkeep.api.ApiClient;
import com.example.learnkeep.api.ApiService;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    EditText[] otpBoxes;
    EditText etNewPassword, etConfirmNewPassword;
    Button btnResetPassword;
    ImageButton ibToggleNewPass, ibToggleConfirmPass;
    TextView tvEmailHint, tvCountdown, btnResend;

    String email;
    CountDownTimer countDownTimer;
    boolean canResend = false;
    boolean isNewPassVisible    = false;
    boolean isConfirmPassVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        email = getIntent().getStringExtra("email");

        tvEmailHint        = findViewById(R.id.tvEmailHint);
        tvCountdown        = findViewById(R.id.tvCountdown);
        btnResend          = findViewById(R.id.btnResend);
        btnResetPassword   = findViewById(R.id.btnResetPassword);
        etNewPassword      = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        ibToggleNewPass    = findViewById(R.id.ibToggleNewPass);
        ibToggleConfirmPass = findViewById(R.id.ibToggleConfirmPass);

        if (email != null) {
            tvEmailHint.setText("Enter the 6-digit code sent to " + email);
        }

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

        // Password toggles
        ibToggleNewPass.setOnClickListener(v -> {
            isNewPassVisible = !isNewPassVisible;
            toggleVisibility(etNewPassword, isNewPassVisible, ibToggleNewPass);
        });

        ibToggleConfirmPass.setOnClickListener(v -> {
            isConfirmPassVisible = !isConfirmPassVisible;
            toggleVisibility(etConfirmNewPassword, isConfirmPassVisible, ibToggleConfirmPass);
        });

        btnResetPassword.setOnClickListener(v -> resetPassword());

        btnResend.setOnClickListener(v -> {
            if (canResend) resendOtp();
            else Toast.makeText(this, "Please wait before resending", Toast.LENGTH_SHORT).show();
        });

        if (findViewById(R.id.btnBack) != null) {
            findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        }
    }

    private void toggleVisibility(EditText field, boolean visible, ImageButton btn) {
        if (visible) {
            field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btn.setImageResource(R.drawable.ic_login_visibility_off);
        } else {
            field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btn.setImageResource(R.drawable.ic_login_visibility);
        }
        field.setSelection(field.getText().length());
    }

    private void setupOtpBoxes() {
        for (int i = 0; i < 6; i++) {
            final int idx = i;

            otpBoxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int c, int a) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && idx < 5) {
                        otpBoxes[idx + 1].requestFocus();
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });

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
        otpBoxes[0].requestFocus();
    }

    private String collectOtp() {
        StringBuilder sb = new StringBuilder();
        for (EditText box : otpBoxes) sb.append(box.getText().toString().trim());
        return sb.toString();
    }

    private void resetPassword() {
        String otp      = collectOtp();
        String newPass  = etNewPassword.getText().toString().trim();
        String confPass = etConfirmNewPassword.getText().toString().trim();

        if (otp.length() < 6) {
            Toast.makeText(this, "Enter all 6 OTP digits", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPass.length() < 6) {
            etNewPassword.setError("Minimum 6 characters");
            etNewPassword.requestFocus();
            return;
        }
        if (!newPass.equals(confPass)) {
            etConfirmNewPassword.setError("Passwords do not match");
            etConfirmNewPassword.requestFocus();
            return;
        }

        btnResetPassword.setEnabled(false);
        btnResetPassword.setText("Resetting...");

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        Map<String, String> body = new HashMap<>();
        body.put("email",       email);
        body.put("otp",         otp);
        body.put("newPassword", newPass);

        api.resetPassword(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call,
                                   Response<Map<String, Object>> response) {
                btnResetPassword.setEnabled(true);
                btnResetPassword.setText("Reset Password  →");

                if (response.isSuccessful()) {
                    Toast.makeText(ResetPasswordActivity.this,
                            "✅ Password reset! Please log in.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    clearOtpBoxes();
                    Toast.makeText(ResetPasswordActivity.this,
                            "❌ Invalid OTP. Try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnResetPassword.setEnabled(true);
                btnResetPassword.setText("Reset Password  →");
                Toast.makeText(ResetPasswordActivity.this,
                        "Server error. Check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startCountdown() {
        canResend = false;
        btnResend.setAlpha(0.4f);
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(60_000, 1000) {
            @Override
            public void onTick(long ms) {
                tvCountdown.setText(String.format(Locale.getDefault(), "Resend in 0:%02d", ms / 1000));
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

        api.sendForgotOtp(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call,
                                   Response<Map<String, Object>> response) {
                Toast.makeText(ResetPasswordActivity.this,
                        "OTP resent to " + email, Toast.LENGTH_SHORT).show();
                clearOtpBoxes();
                startCountdown();
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(ResetPasswordActivity.this,
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
