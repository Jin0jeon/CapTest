package com.example.captest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.utils.widget.MotionLabel;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

//import com.google.android.gms.auth.api.signin.GoogleSignIn;
//import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
//import com.google.android.gms.auth.api.signin.GoogleSignInClient;
//import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.github.angads25.toggle.interfaces.OnToggledListener;
import com.github.angads25.toggle.model.ToggleableView;
import com.github.angads25.toggle.widget.LabeledSwitch;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
//import com.google.api.client.util.DateTime;
//import com.google.api.services.calendar.CalendarScopes;
//import com.google.api.services.calendar.model.Event;

import org.json.JSONException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_SELECT_REQUEST = 1;

    private static final int REQUEST_CODE = 1;

    private static int IS_SERVICE_WORKING = 1; // 매서드로 확인하는것 대신 static 변수로 확인해봄.
    private Button brstart;
    private Button brStop;
    private Button selectLocationButton;
    private TextView tvGridX;
    private TextView tvGridY;
    private TextView tvRegion;
    private TextView tvStartTime;
    private TextView tvEndTime;
    private String date;
    private String time;
    private String gridX;
    private String gridY;
    private String selectedRegion;
    private boolean isReceiverRegistered;
    private LabeledSwitch toggle1;

    // LocationHelper 인스턴스 생성
    private LocationHelper locationHelper;

    private ActivityResultLauncher<Intent> overlayPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(this)) {
                        // 권한이 부여된 후의 로직 처리
                        Toast.makeText(this, "Overlay permission granted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Overlay permission not granted", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvStartTime = findViewById(R.id.tvStartTime);
        tvEndTime = findViewById(R.id.tvEndTime);
        tvGridX = findViewById(R.id.tvGridX);
        tvGridY = findViewById(R.id.tvGridY);
        tvRegion = findViewById(R.id.tvRegion);
        selectLocationButton = findViewById(R.id.btnSelectLocation);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            overlayPermissionLauncher.launch(intent);
        }


        String[] location = getLocationData();
        gridX = location[1];
        gridY = location[2];
        selectedRegion = location[0];

        String[] timeData = getTimeData();
        tvStartTime.setText(timeData[0]);
        tvEndTime.setText(timeData[1]);

        // 값이 null인 경우 특정 Activity로 이동
        if (gridX == null || gridY == null || selectedRegion == null) {
            Intent intent = new Intent(this, LocationSelectActivity.class); // 이동할 액티비티
            startActivityForResult(intent, LOCATION_SELECT_REQUEST);
            Log.d("리스트뷰","시작");
        }

        toggle1 = findViewById(R.id.toggle1);
        toggle1.setOn(isMyServiceRunning(MyForegroundService.class));

        toggle1.setOnToggledListener(new OnToggledListener() {
            @Override
            public void onSwitched(ToggleableView toggleableView, boolean isOn) {

                if (isOn) {
                    if (!isMyServiceRunning(MyForegroundService.class)) {
                        // 서비스 시작
                        Intent serviceIntent = new Intent(MainActivity.this, MyForegroundService.class);
                        serviceIntent.putExtra("inputExtra", "잠금화면 포그라운드 서비스");
                        ContextCompat.startForegroundService(MainActivity.this, serviceIntent);

                        // 리시버 등록
                        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
                        registerReceiver(screenOnReceiver, filter);
                        isReceiverRegistered = true;

                        Toast.makeText(MainActivity.this, "서비스가 시작되었습니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "서비스가 이미 실행중입니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (isMyServiceRunning(MyForegroundService.class)) {
                        // 서비스 중지
                        Intent serviceIntent = new Intent(MainActivity.this, MyForegroundService.class);
                        stopService(serviceIntent);

                        // 리시버 해제
                        if (isReceiverRegistered) {
                            unregisterReceiver(screenOnReceiver);
                            isReceiverRegistered = false;
                        }

                        Toast.makeText(MainActivity.this, "서비스가 중지되었습니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "서비스 중이 아닙니다.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        long now = System.currentTimeMillis();
        Date mDate = new Date(now);
        SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("HH");

        // 현재 날짜를 받아오는 형식 설정 ex) 20221121
        date = simpleDateFormat1.format(mDate);
        time = simpleDateFormat2.format(mDate) + "00";

        updateTextview();
        initAutoCompleteTextView();

        // locationHelper 객체 초기화
        locationHelper = new LocationHelper(this);
        new Handler().postDelayed(() -> {
            DBHelper dbHelper = new DBHelper(this);
        }, 3000); // 3초 지연

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case REQUEST_CODE:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onStart();  // 실행코드
                } else{
                    Toast.makeText(getApplicationContext(), "권한없음", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private ScreenOnReceiver screenOnReceiver = new ScreenOnReceiver();

//    @Override
//    protected void onStart() {
//        super.onStart();
//        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
//        registerReceiver(screenOnReceiver, filter);
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true; // if문 검사로 true면 return true를 만나고 바로 메소드 종료 -> 결과값은 true
            }
        }
        return false; // if문 검사로 false면 if문 탈출 후 return false 를 만나고 메소드 종료 -> 결과값은 false
    }

    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.btnSelectLocation){
            Intent intent = new Intent(MainActivity.this, LocationSelectActivity.class);
            startActivityForResult(intent, LOCATION_SELECT_REQUEST);
            Log.d("리스트뷰","시작");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOCATION_SELECT_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedRegion = data.getStringExtra("selectedRegion");
            // currentRegion에 저장된 위도, 경도를 활용한 후처리 로직을 추가하면 됨
            gridX = data.getStringExtra("gridX");
            gridY = data.getStringExtra("gridY");
            saveLocationData(selectedRegion, gridX, gridY);

            tvGridX.setText(gridX);
            tvGridY.setText(gridY);
            tvRegion.setText(selectedRegion);

        }
    }

    // SharedPreferences에 데이터를 저장하는 메서드
    private void saveLocationData(String region, String latitude, String longitude) {
        SharedPreferences sharedPref = getSharedPreferences("LocationPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString("region", region);
        editor.putString("latitude", latitude);
        editor.putString("longitude", longitude);
        editor.apply();
    }

    // SharedPreferences에 사용자 커스텀 레이아웃에 들어갈 시간 데이터를 저장하는 메서드
    private void saveTimeData(String region, String latitude) {
        SharedPreferences sharedPref = getSharedPreferences("TimePreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("startTime", region);
        editor.putString("endTime", latitude);
        editor.apply();
    }

    public String[] getLocationData() {
        SharedPreferences sharedPref = getSharedPreferences("LocationPreferences", Context.MODE_PRIVATE);
        String region = sharedPref.getString("region", null);
        String latitude = sharedPref.getString("latitude", null);
        String longitude = sharedPref.getString("longitude", null);

        // 데이터가 존재하면 배열로 반환, 없으면 null 반환
        if (region != null && latitude != null && longitude != null) {
            return new String[]{region, latitude, longitude};
        } else {
            return null;
        }
    }
    // 사용자 커스텀 레이아웃에 들어갈 시간 데이터를 수정 / 사용 위해 반환받는 메서드
    public String[] getTimeData() {
        SharedPreferences sharedPref = getSharedPreferences("TimePreferences", Context.MODE_PRIVATE);
        String startTime = sharedPref.getString("startTime", "00:00");
        String endTime = sharedPref.getString("endTime", "00:00");
        Log.d("getTimeData" , startTime +  " " + endTime);
        // 데이터가 존재하면 배열로 반환, 없으면 null 반환
        return new String[]{startTime, endTime};
    }
//
//    private void initSetTimeRecyclerView() {
//        // RecyclerView 초기화
//        RecyclerView setTimeRecyclerView = findViewById(R.id.recyclerViewForTimeSet);
//
//        // 시간 데이터를 담을 리스트 생성
//
//        List<SetTimeDomain> items = new ArrayList<>();
//
//        String[] timeData = getTimeData();
//        // 기본 값 추가 (예: 오전 9시, 오후 6시)
//        for(String time : timeData){
//            items.add(new SetTimeDomain(time));
//        }
//
//        // 어댑터 생성 및 추가
//        SetTimeAdapter setTimeAdapter = new SetTimeAdapter(this, items, (position, newTime) -> {
//            // 클릭 이벤트 처리 (시간 업데이트)
//            items.get(position).setTime(newTime);
//            setTimeAdapter.notifyItemChanged(position);
//
//            // SharedPreferences에 저장
//            saveTimeData(items);
//        });
//
//        // RecyclerView 레이아웃 매니저 설정
//        setTimeRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
//
//        // RecyclerView 어댑터 설정
//        setTimeRecyclerView.setAdapter(setTimeAdapter);
//    }
//


    public static String getCurrentDateInYYYYMMDD() {
        // 현재 날짜를 가져오기
        Calendar calendar = Calendar.getInstance();

        // 포맷 설정
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

        // 형식에 맞게 날짜 반환
        return sdf.format(calendar.getTime());
    }

    private void initAutoCompleteTextView() {
        // 첫 번째 드롭다운 데이터
        String[] firstDropdownData = getResources().getStringArray(R.array.startTime);
        ArrayAdapter<String> firstAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                firstDropdownData
        );

        // 두 번째 드롭다운 데이터
        String[] secondDropdownData = getResources().getStringArray(R.array.endTime);
        ArrayAdapter<String> secondAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                secondDropdownData
        );

        // 첫 번째 AutoCompleteTextView 설정
        AutoCompleteTextView firstDropdown = findViewById(R.id.firstDropdown);
        firstDropdown.setAdapter(firstAdapter);

        firstDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = parent.getItemAtPosition(position).toString();
            String timeTemp = selectedItem;
            String temp = getCurrentDateInYYYYMMDD();
            selectedItem = temp + selectedItem;
            selectedItem = selectedItem.replace(":" , "");
            SharedPreferences sharedPref = getSharedPreferences("TimePreferences" , Context.MODE_PRIVATE); // 첫 번째 값 저장
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("startTime", selectedItem);
            editor.apply();
//            Toast.makeText(this, selectedItem + " startTime 저장", Toast.LENGTH_SHORT).show();
            Log.d("dropdown" , selectedItem + " startTime 저장");
            tvStartTime.setText(timeTemp);
        });

        // 두 번째 AutoCompleteTextView 설정
        AutoCompleteTextView secondDropdown = findViewById(R.id.secondDropdown);
        secondDropdown.setAdapter(secondAdapter);

        secondDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = parent.getItemAtPosition(position).toString();
            String timeTemp = selectedItem;
            String temp = getCurrentDateInYYYYMMDD();
            selectedItem = temp + selectedItem;
            selectedItem = selectedItem.replace(":" , "");
            SharedPreferences sharedPref = getSharedPreferences("TimePreferences", MODE_PRIVATE); // 두 번째 값 저장
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("endTime", selectedItem);
            editor.apply();

            String[]list =  getTimeData();
            Toast.makeText(this, list[0] + " startTime 저장" + list[1] + "endTime 저장", Toast.LENGTH_SHORT).show();

            Log.d("dropdown" , selectedItem + " endTIme 저장");
            tvEndTime.setText(timeTemp);
        });
    }

    private void updateTextview() {
        String[] tvUpdate = getLocationData();
        if (tvUpdate != null && tvUpdate.length >= 3) {
            // 배열에 충분한 데이터가 있는 경우에만 텍스트 설정
            tvRegion.setText(tvUpdate[0]);
            tvGridX.setText(tvUpdate[1]);
            tvGridY.setText(tvUpdate[2]);
        } else {
            // 예외 상황에 대한 처리
            tvRegion.setText("No Region Data");
            tvGridX.setText("No GridX Data");
            tvGridY.setText("No GridY Data");

            // 로그로 예외 상황 기록
            Log.e("MainActivity", "tvUpdate 배열에 데이터가 없습니다.");
        }

        String[] timeUpdate = getTimeData();
        if(timeUpdate != null && timeUpdate.length >= 2){
            tvStartTime.setText(timeUpdate[0]);
            tvEndTime.setText(timeUpdate[1]);
        }else{
            tvStartTime.setText("No Start Time Data");
            tvEndTime.setText("No End Time Data");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 등록된 경우에만 해제
        if (isReceiverRegistered) {
            unregisterReceiver(screenOnReceiver);
            isReceiverRegistered = false;
        }
    }

}

