package com.bestrecharge.app.ui.savings;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bestrecharge.app.data.local.entities.SavingsRecordEntity;
import com.bestrecharge.app.databinding.ItemSavingsRecordBinding;

import java.util.ArrayList;
import java.util.List;

public class SavingsRecordAdapter extends RecyclerView.Adapter<SavingsRecordAdapter.RecordViewHolder> {

    public interface OnRecordActionListener {
        void onUpdateStatus(SavingsRecordEntity record);
    }

    private List<SavingsRecordEntity> records = new ArrayList<>();
    private final OnRecordActionListener listener;

    public SavingsRecordAdapter(OnRecordActionListener listener) {
        this.listener = listener;
    }

    public void setRecords(List<SavingsRecordEntity> newRecords) {
        this.records = newRecords != null ? newRecords : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSavingsRecordBinding binding = ItemSavingsRecordBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new RecordViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        holder.bind(records.get(position));
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    class RecordViewHolder extends RecyclerView.ViewHolder {
        private final ItemSavingsRecordBinding binding;

        public RecordViewHolder(ItemSavingsRecordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(SavingsRecordEntity record) {
            binding.tvRecPlan.setText(record.planName);
            binding.tvRecStatus.setText(record.status);
            binding.tvRecDate.setText(record.transactionDate + " • " + record.paymentProvider);
            binding.tvRecPaid.setText("₹" + (int) record.amountPaid);
            binding.tvRecCashback.setText("+₹" + (int) (record.receivedCashback > 0 ? record.receivedCashback : record.expectedCashback));
            binding.tvRecEffective.setText("₹" + (int) record.effectiveCost);

            binding.btnUpdateStatus.setOnClickListener(v -> {
                if (listener != null) listener.onUpdateStatus(record);
            });
        }
    }
}
