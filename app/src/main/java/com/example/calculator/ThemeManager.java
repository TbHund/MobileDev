package com.example.calculator;

import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class ThemeManager {
    private FirebaseRemoteConfig remoteConfig;
    private AppCompatActivity activity;

    private static final String DEFAULT_STATUS_BAR_COLOR = "#00ff15";
    private static final String DEFAULT_PRIMARY_COLOR = "#2196F3";
    private static final String DEFAULT_BACKGROUND_COLOR = "#F0F0F0";
    private static final String DEFAULT_TEXT_COLOR = "#333333";

    public ThemeManager(AppCompatActivity activity) {
        this.activity = activity;
        remoteConfig = FirebaseRemoteConfig.getInstance();

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                //.setMinimumFetchIntervalInSeconds(0)
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);

        Map<String, Object> defaults = new HashMap<>();
        defaults.put("status_bar_color", DEFAULT_STATUS_BAR_COLOR);
        defaults.put("primary_color", DEFAULT_PRIMARY_COLOR);
        defaults.put("background_color", DEFAULT_BACKGROUND_COLOR);
        defaults.put("text_color", DEFAULT_TEXT_COLOR);
        remoteConfig.setDefaultsAsync(defaults);
    }

    public void applyTheme() {
        String statusBarColor = remoteConfig.getString("status_bar_color");
        String primaryColor = remoteConfig.getString("primary_color");
        String backgroundColor = remoteConfig.getString("background_color");
        String textColor = remoteConfig.getString("text_color");

        setStatusBarColor(statusBarColor);

        CardView weatherCard = activity.findViewById(R.id.weatherCard);
        if (weatherCard != null) {
            try {
                weatherCard.setCardBackgroundColor(Color.parseColor(backgroundColor));
            } catch (Exception e) {
                weatherCard.setCardBackgroundColor(Color.parseColor(DEFAULT_BACKGROUND_COLOR));
            }
        }

        TextView cityName = activity.findViewById(R.id.cityName);
        if (cityName != null) {
            try {
                cityName.setTextColor(Color.parseColor(textColor));
            } catch (Exception e) {
                cityName.setTextColor(Color.parseColor(DEFAULT_TEXT_COLOR));
            }
        }

        TextView temperature = activity.findViewById(R.id.temperature);
        if (temperature != null) {
            try {
                temperature.setTextColor(Color.parseColor(primaryColor));
            } catch (Exception e) {
                temperature.setTextColor(Color.parseColor(DEFAULT_PRIMARY_COLOR));
            }
        }

        TextView weatherDesc = activity.findViewById(R.id.weatherDesc);
        if (weatherDesc != null) {
            try {
                weatherDesc.setTextColor(Color.parseColor(textColor));
            } catch (Exception e) {
                weatherDesc.setTextColor(Color.parseColor(DEFAULT_TEXT_COLOR));
            }
        }
    }

    private void setStatusBarColor(String colorHex) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            try {
                window.setStatusBarColor(Color.parseColor(colorHex));

                if (isColorLight(Color.parseColor(colorHex))) {
                    window.getDecorView().setSystemUiVisibility(
                            window.getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    );
                }
            } catch (Exception e) {
                window.setStatusBarColor(Color.parseColor(DEFAULT_STATUS_BAR_COLOR));
            }
        }
    }

    private boolean isColorLight(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness < 0.5;
    }

    public void fetchAndApply() {
        remoteConfig.fetchAndActivate()
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        applyTheme();
                    } else {
                        applyDefaultTheme();
                    }
                });
    }

    private void applyDefaultTheme() {
        setStatusBarColor(DEFAULT_STATUS_BAR_COLOR);

        CardView weatherCard = activity.findViewById(R.id.weatherCard);
        if (weatherCard != null) {
            weatherCard.setCardBackgroundColor(Color.parseColor(DEFAULT_BACKGROUND_COLOR));
        }

        TextView cityName = activity.findViewById(R.id.cityName);
        if (cityName != null) {
            cityName.setTextColor(Color.parseColor(DEFAULT_TEXT_COLOR));
        }

        TextView temperature = activity.findViewById(R.id.temperature);
        if (temperature != null) {
            temperature.setTextColor(Color.parseColor(DEFAULT_PRIMARY_COLOR));
        }
    }
}