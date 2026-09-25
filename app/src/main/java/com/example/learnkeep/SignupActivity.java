package com.example.learnkeep;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.learnkeep.api.ApiClient;
import com.example.learnkeep.api.ApiService;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    EditText etName, etEmail, etPassword, etConfirmPassword;
    Button btnSignup;
    CheckBox cbTerms;
    ImageButton ibTogglePassword, ibToggleConfirmPassword;
    boolean isPasswordVisible = false;
    boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etName              = findViewById(R.id.etName);
        etEmail             = findViewById(R.id.etEmail);
        etPassword          = findViewById(R.id.etPassword);
        etConfirmPassword   = findViewById(R.id.etConfirmPassword);
        btnSignup           = findViewById(R.id.btnSignup);
        cbTerms             = findViewById(R.id.cbTerms);
        ibTogglePassword        = findViewById(R.id.ibTogglePassword);
        ibToggleConfirmPassword = findViewById(R.id.ibToggleConfirmPassword);

        // ── BACK TO LOGIN ───────────────────────────────────────────────
        TextView tvBackToLogin = findViewById(R.id.tvBackToLogin);
        if (tvBackToLogin != null) {
            tvBackToLogin.setOnClickListener(v -> finish());
        }

        // ── PASSWORD TOGGLE ─────────────────────────────────────────────
        ibTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            toggleVisibility(etPassword, isPasswordVisible, ibTogglePassword);
        });

        ibToggleConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            toggleVisibility(etConfirmPassword, isConfirmPasswordVisible, ibToggleConfirmPassword);
        });

        // ── CREATE ACCOUNT ──────────────────────────────────────────────
        btnSignup.setOnClickListener(v -> validateAndSendOtp());
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

    private void validateAndSendOtp() {
        String name     = etName.getText().toString().trim();
        String email    = etEmail.getText().toString().trim().toLowerCase();
        String password = etPassword.getText().toString().trim();
        String confirm  = etConfirmPassword.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Enter your full name");
            etName.requestFocus();
            return;
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }
        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Please accept the Terms & Privacy Policy", Toast.LENGTH_SHORT).show();
            return;
        }

        // ── SEND SIGNUP OTP ─────────────────────────────────────────────
        btnSignup.setEnabled(false);
        btnSignup.setText("Sending OTP...");

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        api.sendSignupOtp(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call,
                                   Response<Map<String, Object>> response) {
                btnSignup.setEnabled(true);
                btnSignup.setText("Create Account  →");

                if (response.isSuccessful()) {
                    // Navigate to OTP verification screen
                    Intent intent = new Intent(SignupActivity.this, VerifyEmailActivity.class);
                    intent.putExtra("name",     name);
                    intent.putExtra("email",    email);
                    intent.putExtra("password", password);
                    startActivity(intent);
                } else {
                    Toast.makeText(SignupActivity.this,
                            "Failed to send OTP. Try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnSignup.setEnabled(true);
                btnSignup.setText("Create Account  →");
                Toast.makeText(SignupActivity.this,
                        "Server error. Check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
