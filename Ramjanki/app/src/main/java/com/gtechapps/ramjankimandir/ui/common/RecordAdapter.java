package com.gtechapps.ramjankimandir.ui.common;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.gtechapps.ramjankimandir.R;
import com.gtechapps.ramjankimandir.model.RecordItem;

import java.util.ArrayList;
import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> {

    public interface RecordClickListener {
        void onRecordClicked(RecordItem item);
    }

    private final List<RecordItem> items = new ArrayList<>();
    private final RecordClickListener clickListener;
    private final String buttonLabel;

    public RecordAdapter(RecordClickListener clickListener, String buttonLabel) {
        this.clickListener = clickListener;
        this.buttonLabel = buttonLabel;
    }

    public void submitItems(List<RecordItem> freshItems) {
        items.clear();
        items.addAll(freshItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecordViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        RecordItem item = items.get(position);
        holder.titleView.setText(item.title);
        holder.subtitleView.setText(item.subtitle);
        holder.detailView.setText(item.detail);
        holder.statusView.setText(item.status);
        if (TextUtils.isEmpty(buttonLabel) || clickListener == null) {
            holder.editButton.setVisibility(View.GONE);
        } else {
            holder.editButton.setVisibility(View.VISIBLE);
            holder.editButton.setText(buttonLabel);
            holder.editButton.setOnClickListener(v -> clickListener.onRecordClicked(item));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView subtitleView;
        private final TextView detailView;
        private final TextView statusView;
        private final MaterialButton editButton;

        RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.recordTitle);
            subtitleView = itemView.findViewById(R.id.recordSubtitle);
            detailView = itemView.findViewById(R.id.recordDetail);
            statusView = itemView.findViewById(R.id.recordStatus);
            editButton = itemView.findViewById(R.id.editRecordButton);
        }
    }
}
