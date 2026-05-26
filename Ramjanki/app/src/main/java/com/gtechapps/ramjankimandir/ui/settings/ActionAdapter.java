package com.gtechapps.ramjankimandir.ui.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gtechapps.ramjankimandir.R;
import com.gtechapps.ramjankimandir.model.RecordItem;

import java.util.List;

public class ActionAdapter extends RecyclerView.Adapter<ActionAdapter.ActionViewHolder> {

    public interface ActionClickListener {
        void onActionClicked(RecordItem item);
    }

    private final List<RecordItem> items;
    private final ActionClickListener listener;

    public ActionAdapter(List<RecordItem> items, ActionClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ActionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_action, parent, false);
        return new ActionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActionViewHolder holder, int position) {
        RecordItem item = items.get(position);
        holder.titleView.setText(item.title);
        holder.subtitleView.setText(item.subtitle);
        holder.itemView.setOnClickListener(v -> listener.onActionClicked(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ActionViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView subtitleView;

        ActionViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.actionTitle);
            subtitleView = itemView.findViewById(R.id.actionSubtitle);
        }
    }
}
