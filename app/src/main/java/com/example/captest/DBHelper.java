package com.example.captest;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "region_data_local.db";
    private static final String DOWNLOADABLE_DATABASE_NAME = "region_data_downloadable.db"; // assets에 있는 다운로드할 DB 파일 이름
    private static final int DATABASE_VERSION = 1;
    private Context context;
    private static DBHelper instance;
    private SQLiteDatabase database;
    private boolean isDatabaseCreated = false;

    public DBHelper(Context context) {
        super(context, DOWNLOADABLE_DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        initializeDatabase();
    }

    // DBHelper 싱글톤 인스턴스 반환
    public static synchronized DBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DBHelper(context.getApplicationContext());
        }
        return instance;
    }
    // DBHelper 데이터베이스 싱글톤 반환
    public synchronized SQLiteDatabase getDatabase() {
        if (database == null || !database.isOpen()) {
            database = getWritableDatabase();
        }
        return database;
    }


//    @Override
//    public void onCreate(SQLiteDatabase db) {
//        if (isDatabaseCreated) {
//            Log.d("DBHelper", "데이터베이스가 이미 존재하므로 테이블을 다시 생성하지 않음");
//            return;
//        }
//        // 날씨 데이터를 저장하는 테이블 생성
//        String CREATE_WEATHER_TABLE = "CREATE TABLE weather (" +
//                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                "dateTime TEXT, " +
//                "weather TEXT, " +
//                "temperature TEXT, " +
//                "windSpeed TEXT, " +
//                "rainProbability TEXT, " +
//                "humidity TEXT)";
//        db.execSQL(CREATE_WEATHER_TABLE);
//
//        // 지역명, 위도, 경도 정보를 저장하는 테이블 생성
//        String CREATE_REGION_TABLE = "CREATE TABLE region (" +
//                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                "regionName TEXT, " +
//                "latitude TEXT, " +
//                "longitude TEXT)";
//        db.execSQL(CREATE_REGION_TABLE);
//
//        // EditText의 내용을 저장하는 테이블 생성
//        String CREATE_EDITTEXT_TABLE = "CREATE TABLE editTextData (" +
//                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                "inputText TEXT)";
//        db.execSQL(CREATE_EDITTEXT_TABLE);
//    }
@Override
public void onCreate(SQLiteDatabase db) {
    if (!isDatabaseCreated) {
        Log.d("DBHelper", "데이터베이스 테이블 생성 시작");

        String CREATE_WEATHER_TABLE = "CREATE TABLE IF NOT EXISTS weather (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "dateTime TEXT, " +
                "weather TEXT, " +
                "temperature TEXT, " +
                "windSpeed TEXT, " +
                "rainProbability TEXT, " +
                "humidity TEXT)";
        db.execSQL(CREATE_WEATHER_TABLE);

        String CREATE_REGION_TABLE = "CREATE TABLE IF NOT EXISTS region (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "administrative_code TEXT, " +
                "level_1 TEXT, " +
                "level_2 TEXT, " +
                "level_3 TEXT, " +
                "grid_x INTEGER, " +
                "grid_y INTEGER, " +
                "longitude_degree INTEGER, " +
                "longitude_minute INTEGER, " +
                "longitude_second REAL, " +
                "latitude_degree INTEGER, " +
                "latitude_minute INTEGER, " +
                "latitude_second REAL, " +
                "longitude_decimal REAL, " +
                "latitude_decimal REAL)";
        db.execSQL(CREATE_REGION_TABLE);

        String CREATE_EDITTEXT_TABLE = "CREATE TABLE IF NOT EXISTS editTextData (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "inputText TEXT)";
        db.execSQL(CREATE_EDITTEXT_TABLE);

        Log.d("DBHelper", "데이터베이스 테이블 생성 완료");

        isDatabaseCreated = true;
    }
}


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 데이터베이스 버전이 변경될 때 기존 테이블을 삭제하고 새로 생성
        db.execSQL("DROP TABLE IF EXISTS weather");
        db.execSQL("DROP TABLE IF EXISTS region");
        db.execSQL("DROP TABLE IF EXISTS editTextData");
        onCreate(db);
    }


    // 데이터베이스 파일이 로컬 저장소에 존재하는지 확인하여 초기화
    // 데이터베이스 파일이 존재하지 않는 경우, assets 폴더에서 내부 저장소로 복사
    // 이를 통해 앱에서 사용할 수 있는 쓰기 가능한 버전의 데이터베이스를 확보할수있다.
    private void initializeDatabase() {
        String dbPath = context.getDatabasePath(DOWNLOADABLE_DATABASE_NAME).getPath();
        File dbFile = new File(dbPath);
        if (!dbFile.exists()) {
            try {
                Log.d("DBHelper", "데이터베이스가 존재하지 않음, 복사 시도");
                copyDatabaseFromAssets();
                Log.d("DBHelper", "데이터베이스 복사 완료");
            } catch (IOException e) {
                Log.e("DBHelper", "데이터베이스 복사에 실패했습니다.", e);
                throw new RuntimeException("데이터베이스 복사에 실패했습니다.");
            }
        } else {
            Log.d("DBHelper", "데이터베이스가 이미 존재합니다.");
        }
    }

    /**
     * assets 폴더에 있는 데이터베이스 파일을 로컬 데이터베이스로 복사하는 메서드
     * 필요한 조건:
     * - 매개변수: 없음 (클래스 생성 시 Context가 필요)
     * - 권한: 저장소에 접근하기 위한 권한 필요 (데이터베이스를 내부 저장소로 복사하기 위해)
     *
     * 이 메서드는 앱의 assets 폴더에 있는 데이터베이스 파일을 내부 저장소로 복사하여 사용 가능하게 합니다.
     * 결과적으로, 데이터베이스를 수정 가능한 형태로 내부 저장소에 복사하게 됩니다.
     *
     * @throws IOException 파일 복사 중 오류가 발생할 경우
     */
//    public void copyDatabaseFromAssets() throws IOException {
//        String dbPath = context.getDatabasePath(DATABASE_NAME).getPath();
//
//        InputStream input = context.getAssets().open(DATABASE_NAME);
//        OutputStream output = new FileOutputStream(dbPath);
//
//        byte[] buffer = new byte[1024];
//        int length;
//        while ((length = input.read(buffer)) > 0) {
//            output.write(buffer, 0, length);
//        }
//
//        output.flush();
//        output.close();
//        input.close();
//    }
    public void copyDatabaseFromAssets() throws IOException {
        String dbPath = context.getDatabasePath(DOWNLOADABLE_DATABASE_NAME).getPath();
        InputStream input = null;
        OutputStream output = null;

        try {
            Log.d("DBHelper", "데이터베이스 파일 복사 시작");
            input = context.getAssets().open(DOWNLOADABLE_DATABASE_NAME);
            output = new FileOutputStream(dbPath);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            Log.d("DBHelper", "데이터베이스 파일 복사 중");
        } catch (IOException e) {
            Log.e("DBHelper", "데이터베이스 파일 복사 중 오류 발생", e);
            throw new RuntimeException("데이터베이스 복사에 실패했습니다.");
        } finally {
            if (output != null) {
                output.flush();
                output.close();
            }
            if (input != null) {
                input.close();
            }
            Log.d("DBHelper", "데이터베이스 파일 복사 종료");
        }
    }

    // 지역 정보를 데이터베이스에 삽입하는 메서드
    public void insertRegionData(String regionName, String latitude, String longitude) {
        SQLiteDatabase db = this.getWritableDatabase();
        String INSERT_REGION_DATA = "INSERT INTO region (regionName, latitude, longitude) VALUES ('"
                + regionName + "', '" + latitude + "', '" + longitude + "')";
        db.execSQL(INSERT_REGION_DATA);
    }

    /**
     * getUniqueLevel1Regions 메서드는 level_1 컬럼의 데이터를 중복 없이 가져와 리스트로 반환합니다.
     * @return 중복되지 않은 level_1 지역 목록
     */
    @SuppressLint("Range")
    public ArrayList<String> getUniqueLevel1Regions() {
        ArrayList<String> level1Regions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT level_1 FROM region", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                level1Regions.add(cursor.getString(cursor.getColumnIndex("level_1")));
            }
            cursor.close();
        }
        return level1Regions;
    }

    /**
     * getUniqueLevel2Regions 메서드는 특정 level_1에 해당하는 level_2 컬럼의 데이터를 중복 없이 가져와 리스트로 반환합니다.
     * @param level1 선택한 level_1 지역명
     * @return 중복되지 않은 level_2 지역 목록
     */
    @SuppressLint("Range")
    public ArrayList<String> getUniqueLevel2Regions(String level1) {
        ArrayList<String> level2Regions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT level_2 FROM region WHERE level_1 = ?", new String[]{level1});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                level2Regions.add(cursor.getString(cursor.getColumnIndex("level_2")));
            }
            cursor.close();
        }
        return level2Regions;
    }

    /**
     * getUniqueLevel3Regions 메서드는 특정 level_1과 level_2에 해당하는 level_3 컬럼의 데이터를 중복 없이 가져와 리스트로 반환합니다.
     * @param level1 선택한 level_1 지역명
     * @param level2 선택한 level_2 지역명
     * @return 중복되지 않은 level_3 지역 목록
     */

    @SuppressLint("Range")
    public ArrayList<String> getUniqueLevel3Regions(String level1, String level2) {
        ArrayList<String> level3Regions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT level_3 FROM region WHERE level_1 = ? AND level_2 = ?", new String[]{level1, level2});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                level3Regions.add(cursor.getString(cursor.getColumnIndex("level_3")));
            }
            cursor.close();
        }
        return level3Regions;
    }


    public String[] getGridCoordinates(String level1, String level2) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT grid_x, grid_y FROM region WHERE level_1 = ? AND level_2 = ? LIMIT 1";
        String[] selectionArgs = {level1, level2};
        Cursor cursor = db.rawQuery(query, selectionArgs);

        String[] coordinates = new String[2];
        if (cursor.moveToFirst()) {

            int gridXIndex = cursor.getColumnIndex("grid_x");
            int gridYIndex = cursor.getColumnIndex("grid_y");

            if (gridXIndex != -1 && gridYIndex != -1) {
                int gridX = cursor.getInt(gridXIndex);
                int gridY = cursor.getInt(gridYIndex);

                coordinates[0] = String.valueOf(gridX);
                Log.d("DBHelper_getGridCorrdinates_선택한 지역 x : ", String.valueOf(gridX));
                coordinates[1] = String.valueOf(gridY);
                Log.d("DBHelper_getGridCorrdinates_선택한 지역 y : ", String.valueOf(gridY));
            }
        }

        cursor.close();
        return coordinates;
    }

    public List<String> getLastNEditTextData(int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT value FROM editTextData ORDER BY id DESC LIMIT ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(limit)});

        List<String> results = new ArrayList<>();
        while (cursor.moveToNext()) {
            results.add(cursor.getString(0));
        }
        cursor.close();
        return results;
    }

}