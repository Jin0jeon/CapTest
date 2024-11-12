//package com.example.captest;
//
//
//import android.content.Context;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageButton;
//import android.widget.TextView;
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//import java.util.List;
//
//public class SetTimeAdapter extends RecyclerView.Adapter<SetTimeAdapter.ViewHolder> {
//
//    private List<SetTimeDomain> timeList;
//    private Context context;
//
//
//    public SetTimeAdapter(Context context, List<SetTimeDomain> timeList) {
//        this.context = context;
//        this.timeList = timeList;
//    }
//
//    @NonNull
//    @Override
//    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(context).inflate(R.layout.item_select_time, parent, false);
//        return new ViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//        SetTimeDomain item = timeList.get(position);
//
//        holder.tvSetTime.setText(item.getTime());
//
//        holder.btnSetTime.setOnClickListener(v -> {
//            // 버튼 눌렀을때 실행할 코드
//        });
//    }
//
//    @Override
//    public int getItemCount() {
//        return timeList.size();
//    }
//
//    public static class ViewHolder extends RecyclerView.ViewHolder {
//        TextView tvSetTime;
//        ImageButton btnSetTime;
//
//        public ViewHolder(@NonNull View itemView) {
//            super(itemView);
//            tvSetTime = itemView.findViewById(R.id.tvSetTime);
//            btnSetTime = itemView.findViewById(R.id.btnSetTime);
//        }
//    }
//}