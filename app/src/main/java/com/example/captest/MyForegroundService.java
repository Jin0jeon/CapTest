package com.example.captest;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.Tag;
import android.os.IBinder;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class MyForegroundService extends Service {
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private boolean isReceiverRegistered = false;

    private ScreenOnReceiver screenOnReceiver = new ScreenOnReceiver();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        if ("STOP_SERVICE".equals(intent.getAction())) {
            stopSelf(); // 서비스 중지
            return START_NOT_STICKY;
        }
        if (!isReceiverRegistered) {// 등록이 안되어있으면 등록하기.
            registerReceiver(screenOnReceiver, filter);
            isReceiverRegistered = true;
        }
        String input = intent.getStringExtra("inputExtra");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE // Ensure compatibility with Android 12+
        );
        Intent stopIntent = new Intent(this, MyForegroundService.class);
        stopIntent.setAction("STOP_SERVICE"); // 서비스 중지 액션 정의
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                0,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("LockInfo : 잠금화면 위젯 서비스")
                .setContentText("잠금화면에 편리한 기능을 제공중입니다.")
                .setContentIntent(pendingIntent)  // 이동할 액티비티 지정
                .setSmallIcon(R.drawable.captest_app_icon) // 알림 아이콘 설정
                .addAction(R.drawable.ic_launcher_foreground, "중지", stopPendingIntent) // 버튼 추가
                .build();
//        Toast.makeText(this, "포그라운드 서비스 시작", Toast.LENGTH_SHORT).show();
        startForeground(NOTIFICATION_ID, notification);

        // 여기에 서비스 로직 추가
        startWeatherWorker();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // 바인드 서비스가 아니므로 null 반환
    }

    @Override
    public void onDestroy() {
        // 서비스 종료 시 리시버 해제

        super.onDestroy();
        if (isReceiverRegistered) {
            unregisterReceiver(screenOnReceiver);
            Toast.makeText(this, "잠금화면 위젯 사용 해제", Toast.LENGTH_SHORT).show();
            Log.d("리시버해제" , "리시버가 해제됨");
            isReceiverRegistered = false;
        } else {
            Toast.makeText(this, "이미 해제됨", Toast.LENGTH_SHORT).show();
        }
        // WorkManager 중단
        stopWeatherWorker();

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "잠금화면날씨";
            String description = "잠금화면에 날씨와 메모 일정등을 띄워준다.";
            // 이후에 getString(R.string.channel_description);  등으로 수정할 것. 유지보수측면에서 유리.
            //getString(R.string.channel_name);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // WorkManager 시작 메서드
    private void startWeatherWorker() {
        PeriodicWorkRequest weatherWorkRequest = new PeriodicWorkRequest.Builder(
                WeatherData.class, // Worker 클래스
                1, TimeUnit.HOURS // 작업 주기: 1시간
//                15, TimeUnit.MINUTES // 작업주기 : 15분
        ).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "WeatherUpdateWork", // 고유 작업 이름
                ExistingPeriodicWorkPolicy.KEEP, // 중복 실행 방지
                weatherWorkRequest
        );

        Log.d("WorkManager", "Weather Worker가 시작되었습니다.");
    }

    // WorkManager 중단 메서드
    private void stopWeatherWorker() {
        WorkManager.getInstance(this).cancelUniqueWork("WeatherUpdateWork");
        Log.d("WorkManager", "Weather Worker가 중단되었습니다.");
    }



}