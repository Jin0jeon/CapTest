package com.example.captest;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

public class LocationHelper {

    private static final int REQUEST_CODE = 1;  // 권한 요청 코드
    private final FusedLocationProviderClient fusedLocationProviderClient;
    private final Context context;
    private boolean locationPermissionGranted;
    private LocationCallback locationCallback;
    private Location currentLocation;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private Map<String, String> locationMap;

//    public LocationHelper(Context context, ActivityResultLauncher<String> requestPermissionLauncher) {
//        this.context = context;
//        this.requestPermissionLauncher = requestPermissionLauncher;
//        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
//        loadLocationData();
//    }
    public LocationHelper(Context context) {
        this.context = context;
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    private void loadLocationData() {
        locationMap = new HashMap<>();
        Workbook workbook = null;
        try {
            InputStream inputStream = context.getResources().getAssets().open("local_name.xls");
            workbook = Workbook.getWorkbook(inputStream);
            Sheet sheet = workbook.getSheet(0);
            int rows = sheet.getRows();
            for (int row = 1; row < rows; row++) {
                String region = sheet.getCell(0, row).getContents();
                String latitude = sheet.getCell(1, row).getContents();
                String longitude = sheet.getCell(2, row).getContents();

                String key = latitude + "," + longitude;
                Log.d("위치 정보" , latitude + "는 위도 , " + longitude + " 는 경도, " + region + " 는 지역");
                locationMap.put(key, region);
            }
        } catch (IOException | BiffException e) {
            e.printStackTrace();
        } finally {
            if (workbook != null) {
                workbook.close();
            }
        }
    }

    public void getLocationPermission(Activity activity) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            }
        }


    public void onRequestPermissionsResult(boolean isGranted) {
        locationPermissionGranted = isGranted;
    }




//    public void startLocationUpdates() {
//        if (locationPermissionGranted) {
//            LocationRequest locationRequest = LocationRequest.create();
//            locationRequest.setInterval(10000);
//            locationRequest.setFastestInterval(5000);
//            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//
//            locationCallback = new LocationCallback() {
//                @Override
//                public void onLocationResult(LocationResult locationResult) {
//                    if (locationResult == null) {
//                        return;
//                    }
//                    for (Location location : locationResult.getLocations()) {
//                        if (location != null) {
//                            currentLocation = location;
//                        }
//                    }
//                }
//            };
//
//            if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
//            }
//        }
//    }

    public void startLocationUpdates(final LocationResultCallback locationResultCallback) {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                currentLocation = locationResult.getLastLocation();
                // 위치 정보가 들어오면 콜백을 호출하여 결과를 전달
                if (currentLocation != null) {
                    locationResultCallback.onLocationResult(currentLocation);
                } else {
                    locationResultCallback.onLocationFailed("위치 정보를 가져올 수 없습니다.");
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }


    public interface LocationResultCallback {
        void onLocationResult(Location location);
        void onLocationFailed(String errorMessage);
    }

    public void getCurrentLocation(LocationResultCallback locationResultCallback) {
        if (currentLocation != null) {
            locationResultCallback.onLocationResult(currentLocation);
        } else {
            locationResultCallback.onLocationFailed("위치 정보를 가져올 수 없습니다.");
        }
    }

    public String getCurrentRegion() {
        if (currentLocation != null) {
            double latitude = currentLocation.getLatitude(); // 위도
            double longitude = currentLocation.getLongitude(); // 경도
            String key = latitude + "," + longitude;
            return locationMap.getOrDefault(key, "Unknown Location");
        }
        return "Location not available";
    }

    public void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    /* --------------------    대충 지역 이름 받아서 해당 지역의 위도 경도 반환하는 메서드   ------------------- */
//    public String[] getLocationCoordinatesByRegion(String regionName) {
//        Workbook workbook = null;
//        try {
//            // 엑셀 파일 로드
//            InputStream inputStream = context.getResources().getAssets().open("local_name.xls");
//            workbook = Workbook.getWorkbook(inputStream);
//            Sheet sheet = workbook.getSheet(0);
//            int rows = sheet.getRows();
//
//            // 엑셀 데이터 순회
//            for (int row = 1; row < rows; row++) {
//                String region = sheet.getCell(0, row).getContents(); // 0번째 열의 지역명
//
//                // 입력한 지역명과 엑셀의 지역명이 일치하는 경우
//                if (region.equals(regionName)) {
//                    String latitude = sheet.getCell(1, row).getContents(); // 1번째 열의 위도
//                    String longitude = sheet.getCell(2, row).getContents(); // 2번째 열의 경도
//
//                    // 위도와 경도를 반환
//                    return new String[]{latitude, longitude};
//                }
//            }
//
//        } catch (IOException | BiffException e) {
//            e.printStackTrace();
//        } finally {
//            if (workbook != null) {
//                workbook.close();
//            }
//        }
//
//        // 해당 지역명을 찾지 못한 경우
//        return null;
//    }
}
