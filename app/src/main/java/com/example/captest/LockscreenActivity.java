package com.example.captest;

import android.app.KeyguardManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.HasDefaultViewModelProviderFactory;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.captest.databinding.ActivityLockScreenBinding;
import com.github.angads25.toggle.interfaces.OnToggledListener;
import com.github.angads25.toggle.model.ToggleableView;
import com.github.angads25.toggle.widget.LabeledSwitch;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LockscreenActivity extends AppCompatActivity {

    private LocationHelper locationHelper;
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                locationHelper.onRequestPermissionsResult(isGranted);
                if (isGranted) {
                    //locationHelper.startLocationUpdates();
                }
            });
    private String date = "", time = "";

    private String weather = "";
    private String settedRegion;
    private String savedText1;
    private String savedText2;
    private TextView tvData;
    private TextView tvHum;
    private TextView tvTmp;
    private TextView tvRain;
    private TextView tvWindSpd;
    private TextView tvWeather;
    private Button btnGetData;
    private FloatingActionButton btnClose;
    private Button btnVisibility;
    private LinearLayout weatherLinearLayout;
    private LinearLayout todoLayout1;
    private LinearLayout todoLayout2;
    private LinearLayout scrollLayout;
    private EditText todoItem1;
    private EditText todoItem2;
    private RecyclerView recyclerView;
    private List<EditText> editTextList;
    private ActivityLockScreenBinding binding;
    private ConstraintLayout lockScreenLayout;
    private RecyclerView.Adapter adapterFuture;

    public static LockscreenActivity instance = null;
    public static boolean isActive = false;

    private NoteAdapter noteAdapter;
    private DBDataAccessObject DBDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLockScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // 이렇게 뷰 바인딩을 사용해서 위젯 결합을 한 경우 fragment를 사용할때는 onDestroyView()를 오버라이드 할떄  binding = null; 코드를 넣어주어야 한다고 함.
        //https://todaycode.tistory.com/29 참고.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) { // 잠금화면 위에 액티비티 표시를 위한 코드. 아래 사이트 참조 컨트롤 클릭으로 사이트 들어갈수있음
            //https://android-developer.tistory.com/entry/%EC%9E%A0%EA%B8%88-%ED%99%94%EB%A9%B4-%EC%9C%84%EC%97%90-Activity-%EC%97%B4%EA%B8%B0-%EC%95%88%EB%93%9C%EB%A1%9C%EC%9D%B4%EB%93%9C-13-%EB%8C%80%EC%9D%91
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            keyguardManager.requestDismissKeyguard(this, null);
        } else {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        instance = this;
        isActive = true;


        scrollLayout = findViewById(R.id.scrollLayout);

        btnClose = findViewById(R.id.btnClose);
        long now = System.currentTimeMillis();
        Date mDate = new Date(now);

        SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("HH");

        // 현재 날짜를 받아오는 형식 설정 ex) 20221121
        String getDate = simpleDateFormat1.format(mDate);
        // 현재 시간를 받아오는 형식 설정, 시간만 가져오고 WeatherData의 timechange()를 사용하기 위해 시간만 가져오고 뒤에 00을 붙임 ex) 02 + "00"
        String getTime = simpleDateFormat2.format(mDate) + "00";
        date = simpleDateFormat1.format(mDate);
        time = simpleDateFormat2.format(mDate) + "00";

        DBHelper dbHelper = DBHelper.getInstance(this);
        DBDao = new DBDataAccessObject(this);
        ArrayList<String> notes = DBDao.getAllNotes();

        noteAdapter = new NoteAdapter(notes, () -> {
            notes.add(""); // 빈 항목 추가
            noteAdapter.notifyItemInserted(notes.size() - 1);
        });

        updateWeatherUI();
    }

    private void updateWeatherUI(){
        DBDataAccessObject dbDao = new DBDataAccessObject(this);
        Map<String, String> weatherData;
        dbDao.logAllWeatherData();
        weatherData = dbDao.getCurrentWeatherData();

        if (!weatherData.isEmpty()) {
            StringBuilder weatherInfo = new StringBuilder();
            weatherInfo.append("Weather: ").append(weatherData.get("weather")).append("\n")
                    .append("Temperature: ").append(weatherData.get("temperature")).append("\n")
                    .append("Wind Speed: ").append(weatherData.get("windSpeed")).append("\n")
                    .append("Rain Probability: ").append(weatherData.get("rainProbability")).append("\n")
                    .append("Humidity: ").append(weatherData.get("humidity"));

            Log.d("WeatherUpdate", weatherInfo.toString());

            // 현재 시간 가져오기
            long time = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd | hh:mm a", Locale.getDefault());
            Date currentDate = new Date(time);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(currentDate);
            int currentHour = calendar.get(Calendar.HOUR_OF_DAY);

            //현재 시간 textview에 반영하기
            String formattedDateTime = sdf.format(currentDate);
            binding.cityTxt.setText(formattedDateTime);

            // 오전/오후 구분
            SimpleDateFormat hourFormat = new SimpleDateFormat("a", Locale.getDefault());
            String amPm = hourFormat.format(currentDate);

            // 날씨 정보 가져오기
            String weather = weatherData.get("weather");
            String temperature = weatherData.get("temperature");
            String windSpeed = weatherData.get("windSpeed");
            int rainProbability = Integer.parseInt(weatherData.get("rainProbability"));
            //강수확률만 int로 가져온 이유는 아래의 이미지뷰 설정에서 비오는지 여부에 따라 설정하기위함.
            String humidity = weatherData.get("humidity");

            //현재 기온 textview에 반영
            binding.tempCurrent.setText(temperature + "°");
            Pair<Double, Double> tempRange = dbDao.getTodayTemperatureRange();
            if (tempRange != null) {
                Double maxTempDouble = tempRange.first;
                Double minTempDouble = tempRange.second;

                int maxTemp = maxTempDouble != null ? (int) Math.round(maxTempDouble) : 0;
                int minTemp = minTempDouble != null ? (int) Math.round(minTempDouble) : 0;

                Log.d("Temperature", "최고 기온: " + maxTemp + "°C, 최저 기온: " + minTemp + "°C");
                binding.tempMax.setText(maxTemp + "°");
                binding.tempMin.setText(minTemp + "°");
            } else {
                Log.d("Temperature", "오늘의 기온 데이터가 없습니다.");
                binding.tempMax.setText("Error");
                binding.tempMin.setText("Error");
            }
            //현재 강수확률 textview에 반영
            binding.tvNotice.setText("오늘의 강수확률은 " + rainProbability + "%");

            //현재 풍속 textview에 반영
            binding.tv2.setText(windSpeed + "m/s");

            //현재 습도 textview에 반영
            binding.tv3.setText(humidity + "%");

            //현재 지역 textview에 반영
            SharedPreferences sharedPref = getSharedPreferences("LocationPreferences", MODE_PRIVATE);
            settedRegion = sharedPref.getString("region","지역명");
            binding.statusTxt.setText(settedRegion);

             //조건문을 사용하여 이미지 변경
            if (currentHour < 18 && currentHour >= 6) {
                // 오전일 경우
                if (weather.equals("1")) {
                    // UI를 비/눈 오는 아침 사진으로 수정
                    binding.bgImage.setImageResource(R.drawable.sunny_removebg);
                    binding.lockScreenLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.sunny_bg));
                    if(currentHour < 12){
                        binding.tvNotice.setText("하늘이 맑은 아침, 강수확률은 " + rainProbability + " % 입니다.");
                    } else{
                        binding.tvNotice.setText("하늘이 맑은 낮, 강수확률은 " + rainProbability + " % 입니다.");
                    }

            } else if(weather.equals("3") || weather.equals("4")){
                    if(rainProbability >= 50){
                        binding.bgImage.setImageResource(R.drawable.rainy_removebg);
                        binding.lockScreenLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.rainy_bg));
                        binding.tvNotice.setText("강수확률이" + rainProbability + "%입니다. " +"우산을 챙기는게 좋겟어요.");
                    }
                    if(weather.equals("3")){
                        binding.bgImage.setImageResource(R.drawable.cloudy_sunny_removebg);
                        binding.lockScreenLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.cloudy_sunny_bg));
                    }else{
                        binding.bgImage.setImageResource(R.drawable.cloudy_removebg);
                        binding.lockScreenLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.cloudy_bg));
                    }
                    binding.tvNotice.setText("구름이 많은 날, 강수확률은 " + rainProbability + "% 입니다.");
                }
            } else {
                // 오후일 경우
                if (weather.equals("1")) {
                    // UI를 비/눈 오는 아침 사진으로 수정
                    binding.bgImage.setImageResource(R.drawable.clear_night_removebg);
                    binding.lockScreenLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.clear_night_bg));
                    if(currentHour < 22){
                        binding.tvNotice.setText("하늘이 맑은 저녁, 강수확률은 " + rainProbability + " % 입니다.");
                    } else{
                        binding.tvNotice.setText("하늘이 맑은 밤, 강수확률은 " + rainProbability + " % 입니다.");
                    }
                } else if(weather.equals("3") || weather.equals("4")){
                    if(rainProbability >= 50){
                        binding.bgImage.setImageResource(R.drawable.rainy_removebg);
                        binding.lockScreenLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.rainy_bg));
                        binding.tvNotice.setText("강수확률이" + rainProbability + "%입니다. " +"우산을 챙기는게 좋겟어요.");
                    }
                    if(weather.equals("3")){
                        binding.bgImage.setImageResource(R.drawable.cloudy_night_removebg);
                        binding.lockScreenLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.cloudy_night_bg));
                    }else{
                        binding.bgImage.setImageResource(R.drawable.clear_night_removebg);
                        binding.lockScreenLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.clear_night_bg));
                    }
                    binding.tvNotice.setText("구름이 많은 밤, 강수확률은 " + rainProbability + "% 입니다.");
                }
            }


        } else {
            binding.tvNotice.setText("날씨 데이터에 오류가 생겼습니다.");
            Log.d("WeatherUpdate", "Weather data not available.");
        }
        initRecyclerview();
        initEditTextRecyclerView();
        initCustomRecyclerview();
        updateSelectedTime();

    }

    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.btnClose) {
            finishAffinity();
            // 이미지버튼으로 타임피커를 호출 -> 커스텀 날씨 위젯의 시간대를 지정해주도록 하는 온클릭이지만 init 메서드에서 동적으로 등록하는걸로 수정할 예정이라 주석처리.
//        }else if(id == R.id.btnSetStartTime){
//            showTimePickerDialog(R.id.startHourTxt, true);
//        }else if(id == R.id.btnSetEndTime) {
//            showTimePickerDialog(R.id.endHourTxt, false);
        }
    }


    private void initRecyclerview() { // 이후 10시간 날씨
        ArrayList<FutureDomain> items = new ArrayList<>();
        DBDataAccessObject dao = new DBDataAccessObject(this);
        List<String> weatherData = dao.getHourlyWeatherData();
        List<String> list = getFutureTime();

        for(int i = 0; i < 10; i++){

            if(!list.isEmpty()){
                time = list.get(i);
            }

            String weather = weatherData.get(i);
            String[]tempAndPic = weather.split(",");
            String pic = tempAndPic[0];
            int rain = Integer.parseInt(tempAndPic[1]);
            int temp = Integer.parseInt(tempAndPic[2]);
            String picPath;
            switch(pic){
                case "1":
                    if(rain > 50){
                        picPath = "hour_rainy";
                    }else{
                        picPath = "hour_sunny";
                    }
                    break;
                case "3":
                    if(rain > 50){
                        picPath = "hour_rainy";
                    }else{
                        picPath = "hour_cloudy_sunny";
                    }
                    break;
                case "4":
                    if(rain > 50){
                        picPath = "hour_rainy";
                    }else{
                        picPath = "hour_cloudy";
                    }
                    break;
                default:
                    picPath = "hour_sunny";
                    break;
            }

            items.add(new FutureDomain(time, temp, picPath));
        }

        recyclerView = findViewById(R.id.view1);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        adapterFuture = new FutureAdapter(items);
        recyclerView.setAdapter(adapterFuture);

    }

    private void initEditTextRecyclerView() { // 노트
        RecyclerView editTextRecyclerView = findViewById(R.id.view2);
        ArrayList<String> notes = new ArrayList<>(DBDao.getAllNotes());// EditText용 리스트 데이터
        if (notes.isEmpty()) {
            notes.add(""); // 최소 하나의 항목을 추가
        }

        NoteAdapter noteAdapter = new NoteAdapter(notes, null);
        NoteAdapter finalNoteAdapter = noteAdapter;
        noteAdapter = new NoteAdapter(notes, () -> {
            notes.add(""); // 새로운 항목 추가
            finalNoteAdapter.notifyItemInserted(notes.size() - 1);
        });

        editTextRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        editTextRecyclerView.setAdapter(noteAdapter);
    }

    private void initCustomRecyclerview() { // 사용자 지정 시간대 날씨 리사이클러뷰 시작
        ArrayList<CustomDomain> items = new ArrayList<>();
        DBDataAccessObject dao = new DBDataAccessObject(this);
        String day = null;

        String startTime = getTimePreference("startTime");
        String endTime = getTimePreference("endTime");
//        String startTime = "202412150900";
//        String endTime = "202412152100";
        if(isValidFormat(startTime)){
            Log.d("startTime" , "is formatted " + startTime);
        }else{
            Log.d("startTime" , "is not formatted " + startTime);
        }

        if(isValidFormat(endTime)){
            Log.d("endTime" , "is formatted " + endTime);
        }else{
            Log.d("endTime" , "is not formatted " + endTime);
        }

        Log.d("sharedPref","StartTime EndTime" + startTime + ", " + endTime);

        if(startTime.equals(endTime)){
            endTime = addOneHour(endTime);
        }
        Log.d("afterAddOneHour","StartTime EndTime" + startTime + ", " + endTime);

        List<Map<String, String>> weatherDataList = new ArrayList<>();
        String currentTime = getCurrentTimeInCustomFormat();

        if (startTime.compareTo(endTime) > 0) {
            // startTime과 endTime을 정렬
            String temp = startTime;
            startTime = endTime;
            endTime = temp;
        }
        Log.d("afterCompareTO" , "StartTime EndTime" + startTime + ", " + endTime);

        if (currentTime.compareTo(startTime) > 0 && currentTime.compareTo(endTime) < 0) {
            // currentTime이 startTime과 endTime 사이에 있는 경우
            weatherDataList.add(dao.getWeatherDataForTime(endTime));
            weatherDataList.add(dao.getWeatherDataForTime(addOneDay(startTime)));
            Log.d("initCustom" ,"currentTime이 startTime과 endTime 사이에 있는 경우" + endTime + " : endtime " + addOneDay(startTime) + " : add starttinme" );
        } else if (currentTime.compareTo(startTime) > 0 && currentTime.compareTo(endTime) > 0) {
            // currentTime이 startTime과 endTime보다 늦은 경우
            weatherDataList.add(dao.getWeatherDataForTime(addOneDay(startTime)));
            weatherDataList.add(dao.getWeatherDataForTime(addOneDay(endTime)));
            Log.d("initCustom" ," currentTime이 startTime과 endTime보다 늦은 경우" + addOneDay(startTime) + " : add starttinme" + addOneDay(endTime) + " : add endTime");

        } else {
            // currentTime이 startTime보다 이전이거나 다른 조건
            weatherDataList.add(dao.getWeatherDataForTime(startTime));
            weatherDataList.add(dao.getWeatherDataForTime(endTime));
            Log.d("initCustom" ,"currentTime이 startTime보다 이전이거나 다른 조건" + startTime + " :starttinme" + endTime + " : endTime");
        }
        Log.d("addOneDay",addOneDay(startTime));
        weatherDataList.add(dao.getWeatherDataForTime(endTime));
        weatherDataList.add(dao.getWeatherDataForTime(addOneDay(startTime)));


        for(Map<String, String> weatherData :  weatherDataList){
            String tempCurrentTime = currentTime;
            String weather = weatherData.get("weather");
            int temp = Integer.parseInt(weatherData.get("temperature"));
            int rain = Integer.parseInt(weatherData.get("rainProbability"));
            String customTime = weatherData.get("dateTime");
            if (customTime == null || customTime.length() != 12) {
                throw new IllegalArgumentException("Invalid dateTime format. Expected YYYYMMDDHH00.");
            }


            Log.d("currentTime123" , customTime);

            // HH 추출
            String hour = customTime.substring(4, 6) + " / " +customTime.substring(6, 8)  + " " +customTime.substring(8, 10);

            // HH:00 형식으로 반환
            hour = hour + ":00";

            String picPath;

            switch (weather) {
                case "1":
                    picPath = (rain > 50) ? "hour_rainy" : "hour_sunny";
                    break;
                case "3":
                    picPath = (rain > 50) ? "hour_rainy" : "hour_cloudy_sunny";
                    break;
                case "4":
                    picPath = (rain > 50) ? "hour_rainy" : "hour_cloudy";
                    break;
                default:
                    picPath = "hour_sunny";
                    break;
            }

            items.add(new CustomDomain(hour, temp, rain, picPath));
        }


        RecyclerView customRecyclerView = findViewById(R.id.view3); // 새로운 RecyclerView ID
        customRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        CustomAdapter customAdapter = new CustomAdapter(this, items); // CustomAdapter 사용
        customRecyclerView.setAdapter(customAdapter);
    }

    public boolean isValidFormat(String input) {
        // 입력이 null이거나 길이가 12가 아닌 경우 false 반환
        if (input == null || input.length() != 12) {
            return false;
        }

        try {
            // 연, 월, 일, 시간 부분을 분리
            int year = Integer.parseInt(input.substring(0, 4));
            int month = Integer.parseInt(input.substring(4, 6));
            int day = Integer.parseInt(input.substring(6, 8));
            int hour = Integer.parseInt(input.substring(8, 10));
            String minutes = input.substring(10, 12);

            // 시간의 마지막 00 검증
            if (!"00".equals(minutes)) {
                return false;
            }

            // 월이 1~12 범위인지 확인
            if (month < 1 || month > 12) {
                return false;
            }

            // 각 월에 따른 일 수 확인 (윤년 계산 포함)
            int[] daysInMonth = {31, isLeapYear(year) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
            if (day < 1 || day > daysInMonth[month - 1]) {
                return false;
            }

            // 시간이 0~23 범위인지 확인
            if (hour < 0 || hour > 23) {
                return false;
            }

            return true; // 모든 조건을 통과하면 true 반환
        } catch (NumberFormatException e) {
            // 숫자로 변환할 수 없는 경우 false 반환
            return false;
        }
    }



    private static String addOneDay(String time) {
        Log.d("addOneDay", "parameter : " + time);

        // 입력 검증
        if (time == null || time.length() != 12) {
            throw new IllegalArgumentException("Invalid time format. Expected YYYYMMDDHH00.");
        }

        // 날짜와 시간 분리
        String year = time.substring(0, 4);
        String month = time.substring(4, 6);
        String day = time.substring(6, 8);
        String hour = time.substring(8, 10);

        // 각 부분을 정수로 변환
        int yearInt = Integer.parseInt(year);
        int monthInt = Integer.parseInt(month);
        int dayInt = Integer.parseInt(day);

        // 하루 추가
        dayInt++;

//        // 월말 및 연말 처리
//        int[] daysInMonth = {31, isLeapYear(yearInt) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
//        if (dayInt > daysInMonth[monthInt - 1]) {
//            dayInt = 1;
//            monthInt++;
//            if (monthInt > 12) {
//                monthInt = 1;
//                yearInt++;
//            }
//        }

        // 결과를 다시 문자열로 포맷
        String newDate = String.format("%04d%02d%02d%s00", yearInt, monthInt, dayInt, hour);
        Log.d("After_addOneDay", "returnValue : " + newDate);
        return newDate;
    }

    // 윤년 계산 함수
    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }


    private static String addOneHour(String dateTime) {
        Log.d("addOneHour","parameter : " + dateTime);
        // YYYYMMDDHH00 포맷인지 확인
        if (dateTime.length() != 12) {
            throw new IllegalArgumentException("Invalid dateTime format. Expected YYYYMMDDHH00.");
        }

        // 연, 월, 일, 시 추출
        int year = Integer.parseInt(dateTime.substring(0, 4));
        int month = Integer.parseInt(dateTime.substring(4, 6));
        int day = Integer.parseInt(dateTime.substring(6, 8));
        int hour = Integer.parseInt(dateTime.substring(8, 10));

        // 한 시간 더하기
        hour++;
        if (hour == 24) {
            hour = 0;
            day++;

            // 월별 일 수 계산 (윤년 고려)
            int[] daysInMonth = {31, isLeapYear(year) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
            if (day > daysInMonth[month - 1]) {
                day = 1;
                month++;

                if (month > 12) {
                    month = 1;
                    year++;
                }
            }
        }
        String returnvalue = ("" + year + month + day + hour + "00");
        //String returnvalue = String.format("%04d%02d%02d%02d00", year, month, day, hour);
        Log.d("After_addOneHour","returnValue : " + returnvalue);
        // 새로운 시간 문자열 생성
        return returnvalue;
    }


    public String convertToFullDate(String hourString) {
        // 현재 날짜 가져오기
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH는 0부터 시작하므로 +1
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // "HH:00"에서 시간만 추출
        String hour = hourString.split(":")[0];

        // YYYYMMDDHH00 형식으로 문자열 생성
        String fullDate = String.format(Locale.getDefault(), "%04d%02d%02d%02d00", year, month, day, Integer.parseInt(hour));

        return fullDate;
    }


    private List<String> getFutureTime() {
        List<String> timeList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("h a", Locale.ENGLISH); // h: 12시간 형식, a: AM/PM
        Calendar calendar = Calendar.getInstance();

        for (int i = 0; i < 10; i++) {
            String formattedTime = sdf.format(calendar.getTime()); // 현재 시간 포맷팅
            timeList.add(formattedTime); // 리스트에 추가
            calendar.add(Calendar.HOUR_OF_DAY, 1); // 1시간씩 증가
        }

        return timeList;
    }

    private List<EditText> getAllEditTexts(ViewGroup parent) {
        List<EditText> editTexts = new ArrayList<>();

        // 부모 뷰의 자식 뷰를 반복 탐색
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);

            // 자식이 EditText인 경우 리스트에 추가
            if (child instanceof EditText) {
                editTexts.add((EditText) child);
            }

            // 자식이 ViewGroup이라면 재귀적으로 탐색
            if (child instanceof ViewGroup) {
                editTexts.addAll(getAllEditTexts((ViewGroup) child));
            }
        }
        return editTexts;
    }

    private void showTimePickerDialog(int textViewId, boolean isStartTime) {
        TextView hourTxt = findViewById(textViewId);
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, TimePickerDialog.THEME_HOLO_LIGHT, (view, hourOfDay, minuteOfHour) -> {

            Calendar selectedTimeCalendar = Calendar.getInstance();
            selectedTimeCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            selectedTimeCalendar.set(Calendar.MINUTE, 0);
            selectedTimeCalendar.set(Calendar.SECOND, 0);

            // "HH:00" 형식으로 변환 (UI용)
            String displayTime = String.format(Locale.getDefault(), "%02d:00", hourOfDay);
            hourTxt.setText(displayTime); // 사용자가 보는 TextView 업데이트            hourTxt.setText(selectedTime); // 선택된 시간 설정

            // "YYYYMMddHH00" 형식으로 변환 (로직용)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH00", Locale.getDefault());
            String formattedTime = sdf.format(selectedTimeCalendar.getTime());

            if (isStartTime) {
                saveTimePreference("startTime", displayTime);
            } else {
                saveTimePreference("endTime", displayTime);
            }
            //updateWeatherData(formattedTime, isStartTime); // 날씨 데이터 업데이트
        }, hour, minute, true);

        timePickerDialog.show();
    }

    private void saveTimePreference(String key, String value) {
        SharedPreferences sharedPref = getSharedPreferences("TimePreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.apply();
    }
    private String getTimePreference(String key) {
        SharedPreferences sharedPref = getSharedPreferences("TimePreferences", Context.MODE_PRIVATE);
//        String value =  sharedPref.getString(key, getCurrentTimeInCustomFormat());
        String value =  sharedPref.getString(key, getCurrentTimeInCustomFormat());
        if(value.equals(getCurrentTimeInCustomFormat())){
            Log.d("warning" , "no value for SharedPref");
        }
        String formattedvalue = normalizeToFullFormat(value);
        Log.d("format" , value + "->" + formattedvalue);
        return formattedvalue;
    }

    public String normalizeToFullFormat(String inputTime) {
        // 현재 날짜 가져오기
        Calendar calendar = Calendar.getInstance();

        // 입력 데이터가 YYYYMMDDHH00 형식인지 확인
        if (inputTime.length() == 12 && inputTime.matches("\\d{12}")) {
            return inputTime; // 이미 YYYYMMDDHH00 형식인 경우 그대로 반환
        }

        // 입력 데이터가 HH:00 형식인지 확인
        if (inputTime.length() == 5 && inputTime.matches("\\d{2}:00")) {
            String hour = inputTime.substring(0, 2); // HH 추출
            String currentDate = String.format(Locale.getDefault(), "%04d%02d%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1, // MONTH는 0부터 시작
                    calendar.get(Calendar.DAY_OF_MONTH));
            return currentDate + hour + "00"; // YYYYMMDDHH00 형식으로 반환
        }

        // 형식이 올바르지 않은 경우 예외 처리
        throw new IllegalArgumentException("Invalid time format. Expected YYYYMMDDHH00 or HH:00.");
    }


    private void updateSelectedTime(){
        String start, end;
        start = getTimePreference("startTime");
        end = getTimePreference("endTime");

        // 리사이클러뷰의 시간 textView에 들어갈 시간을 불러와서 업데이트하는 부분. 지금은 리니어를 리사이클러로 바꾸기위해 주석처리함.
//        binding.startHourTxt.setText(start);
//        binding.endHourTxt.setText(end);

        String formattedStart = formatSelectedTime(start);
        String formattedEnd = formatSelectedTime(end);

//        updateWeatherData(formattedStart, true);
//        updateWeatherData(formattedEnd, false);
    }

    private String formatSelectedTime(String selectedTime){
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String currentDate = dateFormat.format(calendar.getTime()); // "YYYYMMdd"

        // "HH:00" 형식에서 시간(HH)만 추출
        String[] parts = selectedTime.split(":");
        String hour = parts[0]; // "HH"

        // "YYYYMMddHH00" 형식으로 조합
        return currentDate + hour + "00";
    }




    public static String getCurrentTimeInCustomFormat() {
        // 현재 시간 가져오기
        Calendar calendar = Calendar.getInstance();

        // 포맷 설정
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH00", Locale.getDefault());
        Log.d("getCurrentTime" , sdf.format(calendar.getTime()));
        // 형식에 맞게 반환
        return sdf.format(calendar.getTime());

    }

    private boolean isPastTime(String time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault());
            Date inputTime = sdf.parse(time);
            Date currentTime = new Date();

            return inputTime != null && inputTime.before(currentTime);
        } catch (ParseException e) {
            Log.e("TimeComparison", "시간 비교 실패: " + e.getMessage(), e);
            return false;
        }
    }

    private String getNextDayTime(String time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault());
            Date inputTime = sdf.parse(time);

            if (inputTime != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(inputTime);
                calendar.add(Calendar.DATE, 1); // 다음날로 이동

                return sdf.format(calendar.getTime());
            }
        } catch (ParseException e) {
            Log.e("NextDayCalculation", "다음날 시간 계산 실패: " + e.getMessage(), e);
        }

        return time; // 실패 시 기본값 반환
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        List<EditText>editTexts = getAllEditTexts(binding.view2);
        ArrayList<String> editTextContents = new ArrayList<>();

        // 로그로 텍스트 확인
        for (int i = 0; i < editTextContents.size(); i++) {
            Log.d("EditTextContent", "EditText " + i + ": " + editTextContents.get(i));
        }

        for (EditText editText : editTexts) {
            String text = editText.getText().toString(); // EditText의 텍스트 추출
            editTextContents.add(text); // 텍스트를 리스트에 추가
            Log.d("getAllEditTexts",text);
        }
        DBDao.saveAllNotes(editTextContents);

        isActive = false;
        instance = null;

    }

}
