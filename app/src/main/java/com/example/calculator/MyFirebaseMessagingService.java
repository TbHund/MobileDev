package com.example.calculator;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM Service";
    private static final String CHANNEL_ID = "calculator_notifications";
    private static final String CHANNEL_NAME = "Calculator Notifications";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "Получено сообщение: " + remoteMessage.getData());

        if (remoteMessage.getData().size() > 0) {
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");

            if (title == null) title = "Калькулятор";
            if (body == null) body = "Новое уведомление";

            showNotification(title, body);
        }

        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();

            showNotification(title, body);
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Новый токен: " + token);
        saveTokenToSharedPrefs(token);
    }

    private void showNotification(String title, String message) {
        createNotificationChannel();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Иконка
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Канал для уведомлений калькулятора");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void saveTokenToSharedPrefs(String token) {
        getSharedPreferences("fcm_prefs", MODE_PRIVATE)
                .edit()
                .putString("fcm_token", token)
                .apply();
    }

    public static String getToken(Context context) {
        return context.getSharedPreferences("fcm_prefs", MODE_PRIVATE)
                .getString("fcm_token", null);
    }
}