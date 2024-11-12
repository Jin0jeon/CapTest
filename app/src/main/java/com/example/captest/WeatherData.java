package com.example.captest;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


public class WeatherData extends Worker {
    private final Context context;

    private String sky, temperature, wind, rain, snow, humidity;
    private Map<String, Map<String, String>> weatherData = new HashMap<>();

    public WeatherData(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // 1. 현재 시간과 날짜를 가져오기
            String currentDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
            String Time = new SimpleDateFormat("HH", Locale.getDefault()).format(new Date()) + "00" ;
            String currentTime = timeChange(Time);

            String TimeForLog = new SimpleDateFormat("HHmm", Locale.getDefault()).format(new Date());
            Log.d("WeatherWorker", "워커 백그라운드 작업 실행중 : " + currentDate + " " + TimeForLog);

            // 2. 위치 데이터 설정 (예: 서울 지역)
            SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            String nx = sharedPref.getString("gridX", "60");
            String ny = sharedPref.getString("gridY", "127");

            if (nx == null || ny == null) {
                Log.e("WeatherWorker", "좌표 데이터가 없습니다");
                return Result.failure();
            }

            // 3. 날씨 데이터 가져오기
            Map<String, Map<String, String>> fetchedWeatherData = lookUpWeather(currentDate, currentTime, nx, ny);

            if (fetchedWeatherData != null) {
                // 4. 데이터베이스에 저장
                DBDataAccessObject dbDao = new DBDataAccessObject(getApplicationContext());
                dbDao.insertWeatherData(fetchedWeatherData);

                Log.d("WeatherWorker", "날씨 데이터 패치 완료: " + currentDate + " " + currentTime);
                return Result.success(); // 작업 성공
            } else {
                Log.e("WeatherWorker", "날씨 데이터 패치 실패");
                return Result.retry(); // 네트워크 또는 API 문제 시 재시도
            }
        } catch (IOException | JSONException e) {
            Log.e("WeatherWorker", "날씨 데이터 패치 중 오류 발생", e);
            return Result.failure(); // 작업 실패
        }


    }

    public class WeatherLookupTask extends AsyncTask<String, Void, Map<String, Map<String, String>>> {
        private ProgressDialog progressDialog;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(context, "Please Wait", "Fetching Weather Data...");
        }

        
        @Override
        protected Map<String, Map<String, String>> doInBackground(String... params) {
            String date = params[0];
            String time = params[1];
            String nx = params[2];
            String ny = params[3];
            try {
                if (date == null || time == null || nx == null || ny == null) {
                    Log.e("WeatherData", "One of the parameters is null: date=" + date + ", time=" + time + ", nx=" + nx + ", ny=" + ny);
                    return null;
                }
                Log.d("async lookUpWeather parameter",date + " " + time + " " + nx + " " + ny);
                return lookUpWeather(date, time, nx, ny);
            } catch (IOException | JSONException e) {
                Log.e("", "Failed to fetch weather data", e);
                return null;
            }
        }


        @Override
        protected void onPostExecute(Map<String, Map<String, String>> weatherData) {
            if (weatherData != null) {
                DBDataAccessObject dbDao = new DBDataAccessObject(context);
                dbDao.insertWeatherData(weatherData);
                Log.d("db","db done");
                Toast.makeText(context, "update done" , Toast.LENGTH_SHORT);
            }
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();

            }
        }


    }

    public Map<String, Map<String, String>> lookUpWeather(String date, String time, String nx, String ny) throws IOException, JSONException { // 중첩 map을 반환하는 메서드. 3일뒤의 날씨 정보를 map형태로 저장.
        String baseDate = date; // 2022xxxx 형식을 사용해야 함
        String baseTime = timeChange(time); // 0500 형식을 사용해야 함

        if (Objects.equals(baseTime, "12300")){ // 새벽00시, 1시일경우 23시로 수정하고 날짜를 1 줄여주는 코드
            int dateInt = Integer.parseInt(baseDate);
            dateInt -= 1;
            baseDate = String.valueOf(dateInt);
            baseTime = "2300";
        }
        // 0시, 1시일경우 baseTime을 12300으로 바꾸도록 timeChange() 를 수정했다.
        // basetime이 12300일경우 baseDate를 정수형으로 바꿔서 1을 빼준다.(날짜를 하루 전으로 바꿈)
        // 다시 baseDate를 문자열로 바꿔주고 baseTime을 2300으로 복구한다.

        String type = "json";

        // end point 주소값
        String apiUrl = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst";
        // 일반 인증키
        String serviceKey = "p1%2BISo6p7dFn6zsOzRfVJJk28r6XW%2B%2BN9AQs8CTXBCJFOAd9h%2BOPam3dLcszo0rSycvhaShbXj3DsGpjnDpARA%3D%3D";

        StringBuilder urlBuilder = new StringBuilder(apiUrl);
        urlBuilder.append("?" + URLEncoder.encode("ServiceKey", "UTF-8") + "=" + serviceKey); // 서비스 키
        urlBuilder.append("&" + URLEncoder.encode("nx", "UTF-8") + "=" + URLEncoder.encode(nx, "UTF-8")); // x좌표
        urlBuilder.append("&" + URLEncoder.encode("ny", "UTF-8") + "=" + URLEncoder.encode(ny, "UTF-8")); // y좌표
        urlBuilder.append("&" + URLEncoder.encode("numOfRows","UTF-8") + "=" + URLEncoder.encode("1000", "UTF-8")); /*한 페이지 결과 수*/
        urlBuilder.append("&" + URLEncoder.encode("base_date", "UTF-8") + "=" + URLEncoder.encode(baseDate, "UTF-8")); /* 조회하고싶은 날짜*/
        urlBuilder.append("&" + URLEncoder.encode("base_time", "UTF-8") + "=" + URLEncoder.encode(baseTime, "UTF-8")); /* 조회하고싶은 시간 AM 02시부터 3시간 단위 */
        urlBuilder.append("&" + URLEncoder.encode("dataType", "UTF-8") + "=" + URLEncoder.encode(type, "UTF-8"));    /* 타입 */

        /*
         * GET방식으로 전송해서 파라미터 받아오기
         */
        Log.d("요청주소" , urlBuilder.toString());
        URL url = new URL(urlBuilder.toString());
        // json데이터들을 웹페이지를통해 확인할 수 있게  로그캣에 링크 출력
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");

        BufferedReader rd;
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            Log.d("접속여부" , "Ok");
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            Log.d("접속여부" , "No");
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        conn.disconnect();
        String result = sb.toString();

        final int MAX_LEN = 2000; // api로부터 받아온 데이터를 2000 bytes 마다 끊어서 출력하기 위함. 데이터 누락이 있는지 확인하기 위한 부분. 개발 마무리에는 제거할 부분.
        int len = result.length();
        if(len > MAX_LEN) {
            int idx = 0, nextIdx = 0;
            while(idx < len) {
                nextIdx += MAX_LEN;
                Log.d("요청결과", result.substring(idx, nextIdx > len ? len : nextIdx));
                idx = nextIdx;
            }
        } else {
            Log.d("요청결과", result);
        }
        Log.d("요청결과" , result);

        // response 키를 가지고 데이터를 파싱
        JSONObject jsonObj_1 = new JSONObject(result);
        String response = jsonObj_1.getString("response");
        Log.d("response" , response);

        // response 로 부터 body 찾기
        JSONObject jsonObj_2 = new JSONObject(response);
        String body = jsonObj_2.getString("body");
        Log.d("body" , body);

        // body 로 부터 items 찾기
        JSONObject jsonObj_3 = new JSONObject(body);
        String items = jsonObj_3.getString("items");
        Log.d("ITEMS", items);

        // items로 부터 itemlist 를 받기
        JSONObject jsonObj_4 = new JSONObject(items);
        JSONArray jsonArray = jsonObj_4.getJSONArray("item");

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonItem = jsonArray.getJSONObject(i);
            String fcstDate = jsonItem.getString("fcstDate");
            String fcstTime = jsonItem.getString("fcstTime");
            String category = jsonItem.getString("category");
            String fcstValue = jsonItem.getString("fcstValue");

            String dateTime = fcstDate + fcstTime;

            weatherData.putIfAbsent(dateTime, new HashMap<>());

            switch (category) {
                case "SKY":
                    weatherData.get(dateTime).put("Weather", fcstValue);
                    break;
                case "TMP":
                    weatherData.get(dateTime).put("Temperature", fcstValue);
                    break;
                case "WSD":
                    weatherData.get(dateTime).put("WindSpeed", fcstValue);
                    break;
                case "POP":
                    weatherData.get(dateTime).put("RainProbability", fcstValue);
                    break;
                case "REH":
                    weatherData.get(dateTime).put("Humidity", fcstValue);
                    break;
            }
            // PTY(강수형태) , RN1(1시간 강수량) 추가할것.
        }
       return weatherData;
    }


    public Map<String, Map<String, String>> currentWeather(String date, String time, String nx, String ny) throws IOException, JSONException { // 중첩 map을 반환하는 메서드. 3일뒤의 날씨 정보를 map형태로 저장.
        String baseDate = date; // 2022xxxx 형식을 사용해야 함
        String baseTime = time; // 0500 형식을 사용해야 함. 초단기실황은 1시간 간격이기에 3시간간격으로 정제할 필요가 없음

        String type = "json";

        // end point 주소값
        String apiUrl = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst";
        // 일반 인증키
        String serviceKey = "p1%2BISo6p7dFn6zsOzRfVJJk28r6XW%2B%2BN9AQs8CTXBCJFOAd9h%2BOPam3dLcszo0rSycvhaShbXj3DsGpjnDpARA%3D%3D";

        StringBuilder urlBuilder = new StringBuilder(apiUrl);
        urlBuilder.append("?" + URLEncoder.encode("ServiceKey", "UTF-8") + "=" + serviceKey); // 서비스 키
        urlBuilder.append("&" + URLEncoder.encode("nx", "UTF-8") + "=" + URLEncoder.encode(nx, "UTF-8")); // x좌표
        urlBuilder.append("&" + URLEncoder.encode("ny", "UTF-8") + "=" + URLEncoder.encode(ny, "UTF-8")); // y좌표
        urlBuilder.append("&" + URLEncoder.encode("numOfRows","UTF-8") + "=" + URLEncoder.encode("10", "UTF-8")); /*한 페이지 결과 수 */
        urlBuilder.append("&" + URLEncoder.encode("base_date", "UTF-8") + "=" + URLEncoder.encode(baseDate, "UTF-8")); /* 조회하고싶은 날짜 */
        urlBuilder.append("&" + URLEncoder.encode("base_time", "UTF-8") + "=" + URLEncoder.encode(baseTime, "UTF-8")); /* 조회하고싶은 시간 */
        urlBuilder.append("&" + URLEncoder.encode("dataType", "UTF-8") + "=" + URLEncoder.encode(type, "UTF-8"));    /* 타입 */

        /*
         * GET방식으로 전송해서 파라미터 받아오기
         */
        Log.d("요청주소" , urlBuilder.toString());
        URL url = new URL(urlBuilder.toString());
        // json데이터들을 웹페이지를통해 확인할 수 있게  로그캣에 링크 출력
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");

        BufferedReader rd;
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            Log.d("접속여부" , "Ok");
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            Log.d("접속여부" , "No");
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        conn.disconnect();
        String result = sb.toString();


        // response 키를 가지고 데이터를 파싱
        JSONObject jsonObj_1 = new JSONObject(result);
        String response = jsonObj_1.getString("response");
        Log.d("response" , response);

        // response 로 부터 body 찾기
        JSONObject jsonObj_2 = new JSONObject(response);
        String body = jsonObj_2.getString("body");
        Log.d("body" , body);

        // body 로 부터 items 찾기
        JSONObject jsonObj_3 = new JSONObject(body);
        String items = jsonObj_3.getString("items");
        Log.d("ITEMS", items);

        // items로 부터 itemlist 를 받기
        JSONObject jsonObj_4 = new JSONObject(items);
        JSONArray jsonArray = jsonObj_4.getJSONArray("item");

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonItem = jsonArray.getJSONObject(i);
            String fcstDate = jsonItem.getString("fcstDate");
            String fcstTime = jsonItem.getString("fcstTime");
            String category = jsonItem.getString("category");
            String fcstValue = jsonItem.getString("fcstValue");

            String dateTime = fcstDate + fcstTime;

            weatherData.putIfAbsent(dateTime, new HashMap<>());// putIfAbsent(key , data) : 매개변수의 키가 Map에 없을때만 추가하고, 해당 키가 이미 Map에 존재한다면 아무 동작도 하지않음.

            switch (category) {
                case "SKY":
                    weatherData.get(dateTime).put("Weather", fcstValue);
                    break;
                case "TMP":
                    weatherData.get(dateTime).put("Temperature", fcstValue);
                    break;
                case "WSD":
                    weatherData.get(dateTime).put("WindSpeed", fcstValue);
                    break;
                case "POP":
                    weatherData.get(dateTime).put("RainProbability", fcstValue);
                    break;
                case "REH":
                    weatherData.get(dateTime).put("Humidity", fcstValue);
                    break;
            }
            // PTY(강수형태) , RN1(1시간 강수량) 추가할것.
        }
        return weatherData;
    }

    private String timeChange(String time)
    {
        // 현재 시간에 따라 데이터 시간 설정(3시간 마다 업데이트) //
        /**
         시간은 3시간 단위로 조회해야 함
         ex) 0200, 0500, 0800 ~ 2300
         시간을 3시간 단위로 변경하는 메소드
         **/
        switch(time) {
            case "0300":
            case "0400":
            case "0500":
                time = "0200";
                break;
            case "0600":
            case "0700":
            case "0800":
                time = "0500";
                break;
            case "0900":
            case "1000":
            case "1100":
                time = "0800";
                break;
            case "1200":
            case "1300":
            case "1400":
                time = "1100";
                break;
            case "1500":
            case "1600":
            case "1700":
                time = "1400";
                break;
            case "1800":
            case "1900":
            case "2000":
                time = "1700";
                break;
            case "2100":
            case "2200":
            case "2300":
                time = "2000";
                break;
            case "0000":
            case "0100":
            case "0200":
                time = "12300";


        }
        return time;
    }



}
