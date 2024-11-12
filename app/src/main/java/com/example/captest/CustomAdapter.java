package com.example.captest;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.CustomViewHolder> {
    private final ArrayList<CustomDomain> items;
    private final Context context;
    private OnTimeChangeClickListener listener; // 클릭 리스너 인터페이스
    private SharedPreferences sharedPreferences;
    private DBDataAccessObject DBDao;

    public CustomAdapter(Context context, ArrayList<CustomDomain> items) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_custom, parent, false);
        return new CustomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
        if (position >= items.size()) return; // 방어 로직

        CustomDomain item = items.get(position);
        holder.hourTxt.setText(item.getHour());
        holder.tempTxt.setText(item.getTemp() + "°");
        holder.rainTxt.setText("\uD83D\uDCA7 " + item.getRain() + "%");

        int drawableResourceId = context.getResources().getIdentifier(
                item.getPicPath(), "drawable", context.getPackageName());

        Glide.with(context)
                .load(drawableResourceId)
                .into(holder.pic);

    }

    // 생성자를 통해 Context를 전달받음
    public void MyPreferences(Context context) {
        this.sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
    }

    public void saveData(String key, String value) {
        sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public String getData(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    @Override
    public int getItemCount() {
        return Math.min(items.size(), 2); // 최대 2개만 반환
    }

    private String showTimePickerDialog(TextView hourTxt,TextView tempTxt, TextView rainTxt, ImageView pic, int position) {
        // 현재 시간을 가져옵니다.
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        AtomicReference<String> temp = null;

        // TimePickerDialog 생성 및 표시
        TimePickerDialog timePickerDialog = new TimePickerDialog(context, (view, hourOfDay, minute) -> {

            // "HH:00" 형식으로 변환 (UI용)
            String displayTime = String.format(Locale.getDefault(), "%02d:00", hourOfDay);
            hourTxt.setText(displayTime); // 사용자가 보는 TextView 업데이트            hourTxt.setText(selectedTime); // 선택된 시간 설정



            // "YYYYMMddHH00" 형식으로 변환 (로직용)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH00", Locale.getDefault());
            String formattedTime = sdf.format(calendar.getTime());
            temp.set(formattedTime);
            DBDao = new DBDataAccessObject(context);
            Map<String, String> updatedWD = DBDao.getWeatherDataForTime(formattedTime);
            tempTxt.setText(updatedWD.get("temperature") + "°");
            rainTxt.setText(updatedWD.get("rainProbability") + "%");


            if(position == 0){
                saveData("startTime", formattedTime);
            }else if(position == 1){
                saveData("endTime", formattedTime);
            }
            saveData("hour_" + position , formattedTime);
            saveData("clock_" + position, displayTime);

        }, hour, 0, true);

        timePickerDialog.show();

        return temp.get();
    }

    public static class CustomViewHolder extends RecyclerView.ViewHolder {
        TextView hourTxt, tempTxt, rainTxt;
        ImageView pic;
        ImageButton btnSetTime;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            hourTxt = itemView.findViewById(R.id.hourTxt);
            tempTxt = itemView.findViewById(R.id.tempTxt);
            pic = itemView.findViewById(R.id.pic);
            rainTxt = itemView.findViewById(R.id.rainTxt);
        }

    }

    // 클릭 리스너 인터페이스
    public interface OnTimeChangeClickListener {
        void onTimeChangeClick(int position, String time);
    }

}
