package com.example.calculator.security;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calculator.R;
import com.google.android.material.textfield.TextInputEditText;

public class PinSetupActivity extends AppCompatActivity {

    private TextInputEditText etPin, etConfirmPin;
    private Button btnSave;
    private TextView tvError;
    private PinManager pinManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_setup);

        pinManager = new PinManager(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        etPin = findViewById(R.id.etPin);
        etConfirmPin = findViewById(R.id.etConfirmPin);
        btnSave = findViewById(R.id.btnSave);
        tvError = findViewById(R.id.tvError);
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> {
            String pin = etPin.getText().toString().trim();
            String confirmPin = etConfirmPin.getText().toString().trim();

            if (validatePin(pin, confirmPin)) {
                savePin(pin);
            }
        });
    }

    private boolean validatePin(String pin, String confirmPin) {
        tvError.setVisibility(View.GONE);

        if (TextUtils.isEmpty(pin) || TextUtils.isEmpty(confirmPin)) {
            showError("Введите PIN-код");
            return false;
        }

        if (pin.length() < 4) {
            showError("PIN должен содержать минимум 4 цифры");
            return false;
        }

        if (!pin.matches("\\d+")) {
            showError("PIN может содержать только цифры");
            return false;
        }

        if (!pin.equals(confirmPin)) {
            showError("PIN-коды не совпадают");
            return false;
        }

        return true;
    }

    private void savePin(String pin) {
        boolean saved = pinManager.savePin(pin);

        if (saved) {
            Toast.makeText(this, "PIN успешно установлен", Toast.LENGTH_SHORT).show();

            // Задержка перед закрытием, чтобы пользователь увидел сообщение
            new Handler().postDelayed(() -> {
                setResult(RESULT_OK);
                finish();
            }, 1000);
        } else {
            showError("Ошибка при сохранении PIN");
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}