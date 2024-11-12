package com.example.captest;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import androidx.appcompat.app.AppCompatActivity;

public class LocationSearchActivity extends AppCompatActivity {
    private EditText searchEditText;
    private ListView resultListView;
    private DBHelper dbHelper;
    private SQLiteDatabase database;
    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_search);

        // UI 구성 요소 초기화
        searchEditText = findViewById(R.id.searchEditText);
        resultListView = findViewById(R.id.resultListView);

        // DBHelper 싱글톤 인스턴스 가져오기
        dbHelper = DBHelper.getInstance(this);
        database = dbHelper.getDatabase();

        // 검색 결과를 표시하기 위한 어댑터 설정
        adapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_2,
                null,
                new String[]{"level_1", "level_2", "level_3"},
                new int[]{android.R.id.text1, android.R.id.text2},
                0
        );
        resultListView.setAdapter(adapter);



        // 검색창에 텍스트 변경 리스너 추가
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchLocation(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    // 사용자 입력을 기반으로 데이터베이스를 검색하는 메서드
    private void searchLocation(String query) {
        String sqlQuery = "SELECT id AS _id, regionName, latitude, longitude FROM region WHERE \" +\n" +
                "        \"regionName LIKE ? OR latitude LIKE ? OR longitude LIKE ?";
        String[] selectionArgs = new String[]{"%" + query + "%", "%" + query + "%", "%" + query + "%"};

        Cursor cursor = database.rawQuery(sqlQuery, selectionArgs);
        adapter.changeCursor(cursor);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (database != null) {
            database.close();
        }
        if (adapter != null && adapter.getCursor() != null) {
            adapter.getCursor().close();
        }
    }
}