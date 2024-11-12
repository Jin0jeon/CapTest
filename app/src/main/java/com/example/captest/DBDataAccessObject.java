package com.example.captest;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DBDataAccessObject {
    private DBHelper dbHelper;
    private String startDateTime;
    private String endDateTime;
    private SQLiteDatabase db;

    public DBDataAccessObject(Context context) {
        dbHelper = new DBHelper(context);
    }

    public void insertWeatherData(Map<String, Map<String, String>> weatherData) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("weather", null, null);
            for (Map.Entry<String, Map<String, String>> entry : weatherData.entrySet()) {
                String dateTime = entry.getKey();
                Map<String, String> details = entry.getValue();

                ContentValues values = new ContentValues();
                values.put("dateTime", dateTime);
                values.put("weather", details.get("Weather"));
                values.put("temperature", details.get("Temperature"));
                values.put("windSpeed", details.get("WindSpeed"));
                values.put("rainProbability", details.get("RainProbability"));
                values.put("humidity", details.get("Humidity"));

                db.insert("weather", null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void logAllWeatherData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("weather", null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String dateTime = cursor.getString(cursor.getColumnIndex("dateTime"));
                @SuppressLint("Range") String weather = cursor.getString(cursor.getColumnIndex("weather"));
                @SuppressLint("Range") String temperature = cursor.getString(cursor.getColumnIndex("temperature"));
                @SuppressLint("Range") String windSpeed = cursor.getString(cursor.getColumnIndex("windSpeed"));
                @SuppressLint("Range") String rainProbability = cursor.getString(cursor.getColumnIndex("rainProbability"));
                @SuppressLint("Range") String humidity = cursor.getString(cursor.getColumnIndex("humidity"));

                Log.d("WeatherData현황", "dateTime: " + dateTime + ", weather: " + weather +
                        ", temperature: " + temperature + ", windSpeed: " + windSpeed +
                        ", rainProbability: " + rainProbability + ", humidity: " + humidity);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }


    public Map<String, String> getCurrentWeatherData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH00", Locale.getDefault());

        String currentDateTime = sdf.format(new Date());
        Cursor cursor = db.query("weather",
                null,
                "dateTime = ?",
                new String[]{currentDateTime},
                null,
                null,
                null);

        Map<String, String> weatherData = new HashMap<>();
        if (cursor.moveToFirst()) {
            int weatherIndex = cursor.getColumnIndex("weather");
            int temperatureIndex = cursor.getColumnIndex("temperature");
            int windSpeedIndex = cursor.getColumnIndex("windSpeed");
            int rainProbabilityIndex = cursor.getColumnIndex("rainProbability");
            int humidityIndex = cursor.getColumnIndex("humidity");

            // 유효성 검사 후 데이터 가져오기
            if (weatherIndex >= 0 && temperatureIndex >= 0 && windSpeedIndex >= 0 && rainProbabilityIndex >= 0 && humidityIndex >= 0) {
                weatherData.put("weather", cursor.getString(weatherIndex));
                weatherData.put("temperature", cursor.getString(temperatureIndex));
                weatherData.put("windSpeed", cursor.getString(windSpeedIndex));
                weatherData.put("rainProbability", cursor.getString(rainProbabilityIndex));
                weatherData.put("humidity", cursor.getString(humidityIndex));
            } else {
                Log.e("WeatherData", "One or more column indexes are invalid.");
            }
        } else {
            Log.e("WeatherData", "No data found for the current dateTime: " + currentDateTime);
        }
        cursor.close();
        db.close();
        return weatherData;
    }

    public Map<String, String> getWeatherDataForTime(String time) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            // 특정 시간대의 데이터를 가져오기 위해 쿼리 실행
            cursor = db.query(
                    "weather",       // 테이블 이름
                    null,            // 모든 열 선택
                    "dateTime = ?",  // 조건: dateTime이 특정 시간대
                    new String[]{time}, // 조건 값
                    null,            // 그룹화 없음
                    null,            // 필터 없음
                    null             // 정렬 없음
            );

            Map<String, String> weatherData = new HashMap<>();
            if (cursor.moveToFirst()) {
                int weatherIndex = cursor.getColumnIndex("weather");
                int temperatureIndex = cursor.getColumnIndex("temperature");
                int rainProbabilityIndex = cursor.getColumnIndex("rainProbability");
                int dateTimeIndex = cursor.getColumnIndex("dateTime");

                // 유효성 검사 후 데이터 가져오기
                if (weatherIndex >= 0 && temperatureIndex >= 0 && rainProbabilityIndex >= 0) {
                    weatherData.put("weather", cursor.getString(weatherIndex));
                    weatherData.put("temperature", cursor.getString(temperatureIndex));
                    weatherData.put("rainProbability", cursor.getString(rainProbabilityIndex));
                    weatherData.put("dateTime", cursor.getString(dateTimeIndex));

                } else {
                    Log.e("WeatherData", "One or more column indexes are invalid.");
                }
            } else {
                Log.e("WeatherData", "No data found for the given time: " + time);
            }
            return weatherData;
        } catch (Exception e) {
            Log.e("WeatherData", "Error fetching weather data: " + e.getMessage(), e);
            return new HashMap<>(); // 빈 맵 반환
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }


    public List<String> getHourlyWeatherData() { //List(날씨 , 강수확률 , 온도) 를 반환하는 함수
        List<String> weatherList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHH00", Locale.getDefault());

        // 10시간 동안의 시간 리스트 생성
        List<String> dateTimes = new ArrayList<>();
        for (int i = 1; i < 11; i++) {
            dateTimes.add(LocalDateTime.now().plusHours(i).format(formatter));
        }

        // IN 쿼리로 데이터 가져오기
        String placeholders = new String(new char[dateTimes.size()]).replace("\0", "?,").replaceAll(",$", "");
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT weather, temperature, rainProbability FROM weather WHERE dateTime IN (" + placeholders + ") ORDER BY dateTime ASC",
                    dateTimes.toArray(new String[0])
            );

            while (cursor.moveToNext()) {
                int weatherIndex = cursor.getColumnIndex("weather");
                int temperatureIndex = cursor.getColumnIndex("temperature");
                int rainProbabilityIndex = cursor.getColumnIndex("rainProbability");

                // 컬럼 존재 여부 확인
                if (weatherIndex >= 0 && rainProbabilityIndex >= 0) {

                    String weather = cursor.getString(weatherIndex);
                    String temperature = cursor.getString(temperatureIndex);
                    String rainProbability = cursor.getString(rainProbabilityIndex);

                    String weatherInfo = weather + "," + rainProbability + "," + temperature;
                    Log.d("getHourlyWeatherData", "날씨 정보: " + weatherInfo);
                    weatherList.add(weatherInfo);
                } else {
                    Log.e("Database", "쿼리 결과에 필요한 컬럼이 없습니다.");
                }
            }
            if (weatherList.isEmpty()) {
                Log.w("Database", "해당 시간의 날씨 데이터가 없습니다.");
            }
        } finally {
            cursor.close();
            db.close();
        }

        return weatherList;
    }

    public Pair<Double, Double> getTodayTemperatureRange() {
        db = dbHelper.getReadableDatabase();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String todayDate = sdf.format(new Date()); // 오늘 날짜 가져오기

        String query = "SELECT MAX(CAST(temperature AS REAL)) AS maxTemp, " +
                "MIN(CAST(temperature AS REAL)) AS minTemp " +
                "FROM weather " +
                "WHERE dateTime LIKE ?";

        Cursor cursor = db.rawQuery(query, new String[]{todayDate + "%"});

        Double maxTemp = null;
        Double minTemp = null;

        if (cursor.moveToFirst()) {
            int maxTempIndex = cursor.getColumnIndex("maxTemp");
            int minTempIndex = cursor.getColumnIndex("minTemp");

            if (maxTempIndex != -1 && minTempIndex != -1) {
                maxTemp = cursor.isNull(maxTempIndex) ? null : cursor.getDouble(maxTempIndex);
                minTemp = cursor.isNull(minTempIndex) ? null : cursor.getDouble(minTempIndex);
            } else {
                Log.e("DBError", "One or more columns not found in the query result.");
            }
        }

        cursor.close();
        db.close();

        return new Pair<>(maxTemp, minTemp); // 최대, 최소 기온 반환
    }

    // 리스트의 모든 데이터를 저장하는 함수
    public void saveAllNotes(ArrayList<String> notes) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction(); // 트랜잭션 시작
        if(notes.isEmpty()){
            Log.d("saveAllNotes","notes = Empty");
        }
        try {
            db.execSQL("DELETE FROM notepad"); // 기존 데이터 삭제

            for (String note : notes) {
                ContentValues values = new ContentValues();
                values.put("inputText", note); // inputText 값 삽입
                db.insert("notepad", null, values); // notepad 테이블에 삽입
                Log.d("saveAllNotes",note);
            }
            Log.d("DBDataAccessObject", "노트 데이터 저장 완료");
            // id 는 인덱스 + 1 로 대응됨! 즉 id가 1인 속성의 텍스트는 note[0] 의 데이터가 들어감.
            db.setTransactionSuccessful(); // 트랜잭션 성공 설정
        } finally {
            db.endTransaction(); // 트랜잭션 종료
            db.close(); // 데이터베이스 닫기
        }
    }

    // notepad 테이블의 모든 데이터를 반환하는 메서드
    public ArrayList<String> getAllNotes() {
        ArrayList<String> notes = new ArrayList<>(); // 결과를 저장할 리스트
        SQLiteDatabase db = dbHelper.getReadableDatabase(); // 읽기 가능한 DB 가져오기

        // 모든 데이터를 조회하는 쿼리 실행
        Cursor cursor = db.rawQuery("SELECT inputText FROM notepad", null);

        if (cursor.moveToFirst()) {
            do {
                // inputText 값을 가져와 리스트에 추가
                String inputText = cursor.getString(cursor.getColumnIndexOrThrow("inputText"));
                notes.add(inputText);
            } while (cursor.moveToNext()); // 다음 행으로 이동
        }

        cursor.close(); // 커서 닫기
        db.close(); // DB 닫기
        return notes; // 결과 반환
    }

}
