package com.example.captest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

public class LocationSelectActivity extends AppCompatActivity {
    private List<String> regionList;  //지역명을 저장하는 리스트
    private List<String> coordinatesList;  //위도와 경도를 저장하는 리스트
    private String currentRegion;  // 선택된 지역의 위도와 경도가 저장될 변수
    private DBHelper dbHelper;
    private ArrayAdapter<String> adapter;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_select);  // 레이아웃 파일 설정

        listView = findViewById(R.id.regionListView);  // 리스트뷰 참조
        dbHelper = DBHelper.getInstance(this);

        Log.d("액티비티","locationSelectActivity Start");
        dbHelper = new DBHelper(this);
        // 엑셀에서 지역명과 위도, 경도 데이터를 로드
        //loadLocationData();
        loadLevel1Regions();

        // 리스트뷰에 지역명을 표시하기 위한 어댑터 설정
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, regionList);
        listView.setAdapter(adapter);

        loadLevel1Regions();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedRegion = regionList.get(position);
                loadLevel2Regions(selectedRegion);
            }
        });
    }


    /**
     * loadLevel1Regions 메서드는 level_1 지역 데이터를 중복 없이 가져와 리스트뷰에 표시합니다.
     */
    private void loadLevel1Regions() {
        regionList = dbHelper.getUniqueLevel1Regions();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, regionList);
        listView.setAdapter(adapter);
    }

    /**
     * loadLevel2Regions 메서드는 사용자가 선택한 level_1 지역에 해당하는 level_2 지역 데이터를 중복 없이 가져와 리스트뷰를 업데이트합니다.
     * @param level1 선택한 level_1 지역명
     */
    private void loadLevel2Regions(String level1) {
        regionList = dbHelper.getUniqueLevel2Regions(level1);
        adapter.clear();
        adapter.addAll(regionList);
        adapter.notifyDataSetChanged();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedLevel2Region = regionList.get(position);
                Log.d("LocationSelectActivity_loadLevel2Regions","getGridCoordinates(level1, selectedLevel2Region) 매개변수 " + level1 + " " + selectedLevel2Region);
                String[] gridCoordinates = dbHelper.getGridCoordinates(level1, selectedLevel2Region);

                String gridX = gridCoordinates[0];
                String gridY = gridCoordinates[1];
                Log.d("locationSelect" , "아이템 클릭 후 반환할 데이터 - X : " + gridX + " Y : " + gridY);

                Intent resultIntent = new Intent();
                resultIntent.putExtra("selectedRegion", selectedLevel2Region);
                resultIntent.putExtra("gridX", gridX);
                resultIntent.putExtra("gridY", gridY);

                // Log로 선택된 정보 확인
                Log.d("LocationSelectActivity", "Selected Region: " + selectedLevel2Region);
                Log.d("LocationSelectActivity", "Grid X: " + gridX);
                Log.d("LocationSelectActivity", "Grid Y: " + gridY);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }


}
