package com.fongmi.android.tv.ui.presenter;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Func;
import com.fongmi.android.tv.databinding.AdapterFuncBinding;
import com.fongmi.android.tv.utils.KeyUtil;

public class FuncPresenter extends Presenter {

    private final OnClickListener listener;

    public FuncPresenter(OnClickListener listener) {
        this.listener = listener;
    }

    public interface OnClickListener {
        void onItemClick(Func item);

        boolean onLongClick(Func item);
    }

    @NonNull
    @Override
    public Presenter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
        return new ViewHolder(AdapterFuncBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Presenter.ViewHolder viewHolder, Object object) {
        Func item = (Func) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.text.setText(item.getText());
        holder.binding.icon.setImageResource(item.getDrawable());
        setOnClickListener(holder, view -> listener.onItemClick(item));
        holder.view.setOnLongClickListener(view -> listener.onLongClick(item));
        holder.view.setOnKeyListener((view, keyCode, event) -> onKeyDown(view, event));
    }

    @Override
    public void onUnbindViewHolder(@NonNull Presenter.ViewHolder viewHolder) {
    }

    private boolean onKeyDown(android.view.View view, KeyEvent event) {
        // 左右到头不再循环：最左让焦点回到源名，最右留在原地，交给系统焦点搜索
        return false;
    }

    private boolean requestFocus(RecyclerView recyclerView, int position) {
        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
        if (holder != null && holder.itemView.requestFocus()) return true;
        recyclerView.scrollToPosition(position);
        recyclerView.post(() -> {
            RecyclerView.ViewHolder next = recyclerView.findViewHolderForAdapterPosition(position);
            if (next != null) next.itemView.requestFocus();
        });
        return true;
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterFuncBinding binding;

        public ViewHolder(@NonNull AdapterFuncBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
