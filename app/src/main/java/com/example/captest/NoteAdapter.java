package com.example.captest;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class NoteAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_NOTE = 0; // 일반 노트 뷰
    private static final int VIEW_TYPE_ADD_BUTTON = 1; // 추가 버튼 뷰

    private ArrayList<String> notes; // 데이터 리스트
    private OnAddButtonClickListener onAddButtonClickListener; // 버튼 클릭 리스너

    public NoteAdapter(ArrayList<String> notes, OnAddButtonClickListener listener) {
        this.notes = notes;
        this.onAddButtonClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        // 마지막 아이템이면 추가 버튼 뷰 타입 반환
        if (position == notes.size()) {
            return VIEW_TYPE_ADD_BUTTON;
        }
        return VIEW_TYPE_NOTE; // 일반 노트 뷰 타입
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_NOTE) {
            // 일반 노트 뷰홀더 생성
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
            return new NoteViewHolder(view);
        } else {
            // 추가 버튼 뷰홀더 생성
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_edit, parent, false);
            return new AddButtonViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof NoteViewHolder) {
            // 일반 노트 데이터 처리
            NoteViewHolder noteHolder = (NoteViewHolder) holder;
            String note = position < notes.size() ? notes.get(position) : "";
            noteHolder.editText.setText(note);

            noteHolder.imageButton.setOnClickListener(v -> {
                notes.remove(holder.getAdapterPosition());
                notifyItemRemoved(holder.getAdapterPosition());
            });
        } else if (holder instanceof AddButtonViewHolder) {
            AddButtonViewHolder buttonHolder = (AddButtonViewHolder) holder;

            buttonHolder.addButton.setOnClickListener(v -> {
                notes.add("");
                notifyItemInserted(notes.size() - 1);
            });
        }
    }

    @Override
    public int getItemCount() {
        return notes.size() + 1; // 추가 버튼을 포함한 총 개수
    }

    // 일반 노트 뷰홀더
    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        EditText editText;
        ImageButton imageButton;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            editText = itemView.findViewById(R.id.edtNote);
            imageButton = itemView.findViewById(R.id.btnDelete);
        }
    }

    // 추가 버튼 뷰홀더
    public static class AddButtonViewHolder extends RecyclerView.ViewHolder {
        ImageButton addButton;

        public AddButtonViewHolder(@NonNull View itemView) {
            super(itemView);
            addButton = itemView.findViewById(R.id.btnAddEdit); // ImageButton 참조
        }
    }

    // 추가 버튼 클릭 리스너 인터페이스
    public interface OnAddButtonClickListener {
        void onAddButtonClick();
    }
    public ArrayList<String> getNotes(){
        return notes;
    }

}