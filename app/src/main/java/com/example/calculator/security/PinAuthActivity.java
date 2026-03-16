package com.example.calculator.security;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.example.calculator.MainActivity;
import com.example.calculator.R;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.Executor;

public class PinAuthActivity extends AppCompatActivity {

    private TextInputEditText etPin;
    private Button btnLogin, btnBiometric;
    private TextView btnForgot;
    private TextView tvError, tvAttempts, tvLockTimer;
    private PinManager pinManager;

    private CountDownTimer lockTimer;
    private BiometricPrompt biometricPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_auth);

        pinManager = new PinManager(this);

        initViews();
        setupListeners();
        checkLockStatus();
        setupBiometric();
    }

    private void initViews() {
        etPin = findViewById(R.id.etPin);
        btnLogin = findViewById(R.id.btnLogin);
        btnBiometric = findViewById(R.id.btnBiometric);
        btnForgot = findViewById(R.id.btnForgot);
        tvError = findViewById(R.id.tvError);
        tvAttempts = findViewById(R.id.tvAttempts);
        tvLockTimer = findViewById(R.id.tvLockTimer);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> {
            String pin = etPin.getText().toString().trim();
            if (!TextUtils.isEmpty(pin)) {
                checkPin(pin);
            } else {
                showError("Введите PIN-код");
            }
        });

        btnBiometric.setOnClickListener(v -> {
            showBiometricPrompt();
        });

        btnForgot.setOnClickListener(v -> {
            handleForgotPin();
        });
    }

    private void checkPin(String pin) {
        if (pinManager.isLocked()) {
            updateLockStatus();
            return;
        }

        boolean isValid = pinManager.checkPin(pin);

        if (isValid) {
            Toast.makeText(this, "Добро пожаловать!", Toast.LENGTH_SHORT).show();
            navigateToMain();
        } else {

            int remaining = pinManager.getRemainingAttempts();

            if (remaining > 0) {
                showError("Неверный PIN. Осталось попыток: " + remaining);
                tvAttempts.setText("Осталось попыток: " + remaining);

                if (remaining <= 2) {
                    tvAttempts.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }
            }

            etPin.setText("");

            if (pinManager.isLocked()) {
                updateLockStatus();
            }
        }
    }

    private void updateLockStatus() {
        etPin.setVisibility(View.GONE);
        btnLogin.setVisibility(View.GONE);
        btnBiometric.setVisibility(View.GONE);
        btnForgot.setVisibility(View.GONE);
        tvAttempts.setVisibility(View.GONE);

        tvLockTimer.setVisibility(View.VISIBLE);

        long remainingSeconds = pinManager.getLockTimeRemaining();

        lockTimer = new CountDownTimer(remainingSeconds * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                tvLockTimer.setText("Слишком много попыток.\nПодождите " + seconds + " сек.");
            }

            @Override
            public void onFinish() {
                etPin.setVisibility(View.VISIBLE);
                btnLogin.setVisibility(View.VISIBLE);

                if (isBiometricAvailable()) {
                    btnBiometric.setVisibility(View.VISIBLE);
                }

                btnForgot.setVisibility(View.VISIBLE);
                tvLockTimer.setVisibility(View.GONE);
                tvAttempts.setVisibility(View.VISIBLE);
                tvAttempts.setText("Осталось попыток: " + pinManager.getRemainingAttempts());

                etPin.setText("");
                tvError.setVisibility(View.GONE);
            }
        }.start();
    }

    private void checkLockStatus() {
        if (pinManager.isLocked()) {
            updateLockStatus();
        } else {
            int remaining = pinManager.getRemainingAttempts();
            tvAttempts.setText("Осталось попыток: " + remaining);

            if (isBiometricAvailable()) {
                btnBiometric.setVisibility(View.VISIBLE);
            } else {
                btnBiometric.setVisibility(View.GONE);
            }
        }
    }

    private void setupBiometric() {
        if (!isBiometricAvailable()) {
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        Toast.makeText(PinAuthActivity.this, "Вход по биометрии выполнен", Toast.LENGTH_SHORT).show();
                        pinManager.setAuthenticated(true);
                        navigateToMain();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            Toast.makeText(PinAuthActivity.this, "Ошибка: " + errString, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(PinAuthActivity.this, "Не удалось распознать отпечаток", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showBiometricPrompt() {
        if (!isBiometricAvailable()) {
            Toast.makeText(this, "Биометрия не доступна", Toast.LENGTH_SHORT).show();
            return;
        }

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Вход в приложение")
                .setSubtitle("Используйте отпечаток пальца или Face ID")
                .setDescription("Подтвердите личность для входа")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText("Отмена")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private boolean isBiometricAvailable() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
        );

        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void handleForgotPin() {
        Toast.makeText(this, "Для сброса PIN введите мастер-код: 123456", Toast.LENGTH_LONG).show();

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Сброс PIN-кода");
        builder.setMessage("Введите мастер-код для сброса");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Мастер-код");

        input.setPadding(48, 16, 48, 16);
        builder.setView(input);

        builder.setPositiveButton("Сбросить", (dialog, which) -> {
            String masterCode = input.getText().toString();
            if ("123456".equals(masterCode)) {
                pinManager.resetPin();
                Toast.makeText(this, "PIN сброшен. Установите новый PIN.", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, PinSetupActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Неверный мастер-код", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void navigateToMain() {
        pinManager.setAuthenticated(true);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);

        new Handler().postDelayed(() -> tvError.setVisibility(View.GONE), 3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lockTimer != null) {
            lockTimer.cancel();
        }
    }
}