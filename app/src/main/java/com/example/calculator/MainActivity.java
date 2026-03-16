package com.example.calculator;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Log;
import com.example.calculator.security.PinAuthActivity;
import com.example.calculator.security.PinManager;
import com.example.calculator.security.PinSetupActivity;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {
    // firebase
    private FirebaseHistoryManager historyManager;
    private TextView historyTextView;

    // theme
    private ThemeManager themeManager;


    // weather
    private TextView cityName, weatherDesc, temperature, weatherIcon;
    private final String API_KEY = "ae8a86580d0740e7a2673721261503";
    private final String CITY = "Минск";


    private TextView Label1;
    private TextView Label2;

    //pin
    private PinManager pinManager;

    private static final int REQUEST_CODE_PIN_SETUP = 1001;
    private static final int REQUEST_CODE_PIN_AUTH = 1002;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pinManager = new PinManager(this);

        if (pinManager.isAuthenticated()) {
            initializeMainUI();
        } else {
            if (!pinManager.isPinSet()) {
                startActivityForResult(new Intent(this, PinSetupActivity.class), REQUEST_CODE_PIN_SETUP);
            } else {
                startActivityForResult(new Intent(this, PinAuthActivity.class), REQUEST_CODE_PIN_AUTH);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PIN_SETUP) {
            if (resultCode == RESULT_OK) {
                startActivityForResult(new Intent(this, PinAuthActivity.class), REQUEST_CODE_PIN_AUTH);
            } else {
                finish();
            }
        } else if (requestCode == REQUEST_CODE_PIN_AUTH) {
            if (resultCode == RESULT_OK) {
                finish();
            } else {
                finish();
            }
        }
    }

    private void initializeMainUI() {
        setContentView(R.layout.activity_main);

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Label1 = findViewById(R.id.Label1);
        Label2 = findViewById(R.id.Label2);
        historyManager = new FirebaseHistoryManager();
        themeManager = new ThemeManager(this);
        historyTextView = findViewById(R.id.historyTextView);

        Label1.setText("0");
        Label2.setText("");

        cityName = findViewById(R.id.cityName);
        weatherDesc = findViewById(R.id.weatherDesc);
        temperature = findViewById(R.id.temperature);
        weatherIcon = findViewById(R.id.weatherIcon);

        themeManager.fetchAndApply();
        loadWeather();
        loadHistory();
        checkFCMToken();
    }

    private void logout() {
        pinManager.clearAuthentication();
        Intent intent = new Intent(this, PinAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    private void loadWeather() {
        ApiClient.getService()
                .getCurrentWeather(API_KEY, CITY, "ru")
                .enqueue(new Callback<WeatherData>() {
                    @Override
                    public void onResponse(Call<WeatherData> call, Response<WeatherData> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            WeatherData weather = response.body();

                            cityName.setText(weather.getLocation().getName());
                            weatherDesc.setText(weather.getCurrent().getCondition().getText());

                            int temp = (int) Math.round(weather.getCurrent().getTempC());
                            String tempText = (temp > 0 ? "+" : "") + temp + "°";
                            temperature.setText(tempText);

                            weatherIcon.setText(getWeatherEmoji(weather.getCurrent().getCondition().getText()));
                        }
                    }

                    @Override
                    public void onFailure(Call<WeatherData> call, Throwable t) {
                        Toast.makeText(MainActivity.this, "Ошибка: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getWeatherEmoji(String condition) {
        condition = condition.toLowerCase();
        if (condition.contains("солн") || condition.contains("ясн")) return "☀️";
        if (condition.contains("облач")) return "☁️";
        if (condition.contains("дожд")) return "🌧️";
        if (condition.contains("снег")) return "❄️";
        if (condition.contains("гроз")) return "⛈️";
        if (condition.contains("туман")) return "🌫️";
        return "☀️";
    }

    private void checkFCMToken() {
        String token = MyFirebaseMessagingService.getToken(this);
        if (token != null) {
            Log.d("FCM Token", "Токен устройства: " + token);
            Toast.makeText(this, "FCM готов", Toast.LENGTH_SHORT).show();
        } else {
            Log.d("FCM Token", "Токен ещё не получен");
        }
    }

    private void loadHistory() {
        historyManager.loadHistory()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    StringBuilder historyText = new StringBuilder();
                    for (CalculationHistory history :
                            queryDocumentSnapshots.toObjects(CalculationHistory.class)) {
                        historyText.append(history.getExpression())
                                .append(" ")
                                .append(history.getResult())
                                .append("\n");
                    }
                    historyTextView.setText(historyText.toString());
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Ошибка загрузки истории", e);
                    historyTextView.setText("Не удалось загрузить историю");
                });
    }

    private void handleDigit(String digit) {
        if (afterEqualsButton) {
            Label1.setText("");
            Label2.setText("");
            afterEqualsButton = false;
        }
        String currentText = Label1.getText().toString();
        if (inputAfterOperation) {
            Label1.setText(digit);
            inputAfterOperation = false;
        } else {
            if (currentText.equals("0")) {
                Label1.setText(digit);
            } else {
                String helper = currentText + digit;
                Label1.setText(helper);
            }
        }
        try {
            number = Float.parseFloat(Label1.getText().toString());
        } catch (NumberFormatException e) {
            number = 0;
        }
    }

    private void handleOperation(String operation) {
        skip = false;
        afterEqualsButton = false;
        helpstr = Label1.getText().toString() + " " + operation + " ";
        Label2.setText(helpstr);
        helpstr = "";
        inputAfterOperation = true;
    }

    String helpstr = "";
    float number = 0;
    float memory = 0;
    boolean inputAfterOperation = false;
    boolean afterEqualsButton = false;
    boolean skip = false;

    public void onButtonExitClick(View view) {
       logout();
    }
    public void onButtonClick(View view) {
        Button clickedButton = (Button) view;
        String buttonText = clickedButton.getText().toString();
        String label2Text;
        String label1Text;

        switch(buttonText) {
            case "1": case "2": case "3": case "4": case "5":
            case "6": case "7": case "8": case "9": case "0":
                handleDigit(buttonText);
                break;

            case ".":
                helpstr = Label1.getText().toString();
                if (!helpstr.contains(".")) {
                    String helper = helpstr + ".";
                    Label1.setText(helper);
                }
                helpstr = "";
                break;

            case "C":
                Label1.setText("0");
                Label2.setText("");
                helpstr = "";
                number = 0;
                break;

            case "CE":
                Label1.setText("0");
                number = 0;
                break;

            case "⌫":
                helpstr = Label1.getText().toString();
                String result;
                if (Label1.getText().length() == 1) {
                    result = "0";
                } else {
                    result = helpstr.substring(0, helpstr.length() - 1);
                }
                Label1.setText(result);
                try {
                    number = Float.parseFloat(result);
                } catch (NumberFormatException e) {
                    number = 0;
                }
                helpstr = "";
                break;

            case "±":
                helpstr = Label1.getText().toString();
                try {
                    number = Float.parseFloat(helpstr) * -1;
                    if (number == (int)number) {
                        Label1.setText(String.valueOf((int)number));
                    } else {
                        Label1.setText(String.valueOf(number));
                    }
                } catch (NumberFormatException e) {
                    number = 0;
                    Label1.setText("0");
                }
                helpstr = "";
                break;

            case "+": case "-": case "×": case "÷":
                handleOperation(buttonText);
                break;

            case "MC":
                memory = 0;
                break;

            case "MR":
                Label1.setText(String.valueOf(memory));
                number = memory;
                break;

            case "M+":
                try {
                    memory += Float.parseFloat(Label1.getText().toString());
                } catch (NumberFormatException e) {
                    memory += 0;
                }
                break;

            case "M-":
                try {
                    memory -= Float.parseFloat(Label1.getText().toString());
                } catch (NumberFormatException e) {
                    memory -= 0;
                }
                break;

            case "=":
                afterEqualsButton = true;
                label2Text = Label2.getText().toString();
                label1Text = Label1.getText().toString();
                if (label2Text.isEmpty()) {
                    String helper = number + " =";
                    Label2.setText(helper);
                } else if (label2Text.contains("=")) {
                    // do nothing
                } else if (!(label2Text.contains("+") || label2Text.contains("-") ||
                        label2Text.contains("×") || label2Text.contains("÷"))) {
                    String helper = label2Text + " =";
                    Label2.setText(helper);
                } else {
                    String[] parts = label2Text.split(" ");
                    float numInOperation = Float.parseFloat(parts[0]);
                    float res = 0;
                    switch (parts[1]) {
                        case "+":
                            res = numInOperation + number;
                            break;
                        case "-":
                            res = numInOperation - number;
                            break;
                        case "×":
                            res = numInOperation * number;
                            break;
                        case "÷":
                            if (number != 0) {
                                res = numInOperation / number;
                            } else {
                                String helper = "error";
                                Label1.setText(helper);
                                return;
                            }
                            break;
                    }
                    Label1.setText(String.valueOf(res));
                    if (!skip) {
                        String helper = label2Text + number + " =";
                        Label2.setText(helper);
                    } else {
                        String helper = label2Text + " =";
                        Label2.setText(helper);
                        skip = false;
                    }
                    number = res;
                }
                //firebase
                historyManager.saveCalculation(Label2.getText().toString(), Label1.getText().toString())
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Firebase", "Сохранено успешно");
                            loadHistory();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Firebase", "Ошибка сохранения", e);
                        });
                break;

            case "%":
                label2Text = Label2.getText().toString();
                if (!(label2Text.contains("+") || label2Text.contains("-") ||
                        label2Text.contains("×") || label2Text.contains("÷"))) {
                    Label2.setText("");
                    Label1.setText("0");
                    number = 0;
                } else {
                    String[] parts = label2Text.split(" ");
                    float numInOperation = Float.parseFloat(parts[0]);
                    float resultat = 0;

                    switch (parts[1]) {
                        case "+":
                        case "-":
                            resultat = numInOperation * (number / 100);
                            break;
                        case "÷":
                        case "×":
                            resultat = number / 100;
                            break;
                    }

                    Label1.setText(String.valueOf(resultat));
                    number = resultat;
                }
                break;

            case "x²":
                skip = true;
                double helper1 = number * number;
                Label1.setText(String.valueOf(helper1));
                label2Text = Label2.getText().toString();
                if (!label2Text.contains("sqr")) {
                    if (label2Text.contains("=")) {
                        String helper = "sqr(" + number + ")";
                        Label2.setText(helper);
                    } else {
                        String helper = label2Text + " sqr(" + number + ")";
                        Label2.setText(helper);
                    }
                } else {
                    if (label2Text.contains("=")) {
                        String helper = "sqr(" + number + ")";
                        Label2.setText(helper);
                    } else {
                        String replaced = label2Text.replaceAll("\\((\\d+\\.?\\d*)\\)", "(" + number + ")");
                        Label2.setText(replaced);
                    }
                }
                number = (float) helper1;
                break;

            case "√x":
                skip = true;
                double helper2 = Math.sqrt(number);
                Label1.setText(String.valueOf(helper2));
                label2Text = Label2.getText().toString();
                if (!label2Text.contains("sqrt")) {
                    if (label2Text.contains("=")) {
                        String helper = "sqrt(" + number + ")";
                        Label2.setText(helper);
                    } else {
                        String helper = label2Text + " sqrt(" + number + ")";
                        Label2.setText(helper);
                    }
                } else {
                    if (label2Text.contains("=")) {
                        String helper = "sqrt(" + number + ")";
                        Label2.setText(helper);
                    } else {
                        String replaced = label2Text.replaceAll("\\((\\d+\\.?\\d*)\\)", "(" + number + ")");
                        Label2.setText(replaced);
                    }
                }
                number = (float) helper2;
                break;

            case "1/x":
                skip = true;
                if (number == 0) {
                    String helper = "Error";
                    Label1.setText(helper);
                    return;
                }
                double reciprocal = 1.0 / number;
                Label1.setText(String.valueOf(reciprocal));
                label2Text = Label2.getText().toString();
                if (!label2Text.contains("1/(")) {
                    if (label2Text.contains("=")) {
                        String helper = "1/(" + number + ")";
                        Label2.setText(helper);
                    } else {
                        String helper = label2Text + " 1/(" + number + ")";
                        Label2.setText(helper);
                    }
                } else {
                    if (label2Text.contains("=")) {
                        String helper = "1/(" + number + ")";
                        Label2.setText(helper);
                    } else {
                        String replaced = label2Text.replaceAll("\\((\\d+\\.?\\d*)\\)", "(" + number + ")");
                        Label2.setText(replaced);
                    }
                }
                number = (float) reciprocal;
                break;

            default:
                break;
        }
    }
}