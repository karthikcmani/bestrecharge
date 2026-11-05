package com.bestrecharge.app.ui.savings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bestrecharge.app.data.local.entities.SavingsRecordEntity;
import com.bestrecharge.app.databinding.FragmentSavingsBinding;

import java.util.List;

public class SavingsFragment extends Fragment implements SavingsRecordAdapter.OnRecordActionListener {

    private FragmentSavingsBinding binding;
    private SavingsViewModel viewModel;
    private SavingsRecordAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSavingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SavingsViewModel.class);

        adapter = new SavingsRecordAdapter(this);
        binding.rvSavingsRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvSavingsRecords.setAdapter(adapter);

        viewModel.getAllRecords().observe(getViewLifecycleOwner(), records -> {
            if (records != null) {
                adapter.setRecords(records);
                updateSummary(records);
            }
        });
    }

    private void updateSummary(List<SavingsRecordEntity> records) {
        double totalSpent = 0;
        double totalDiscounts = 0;
        double totalReceived = 0;
        double totalPending = 0;

        for (SavingsRecordEntity r : records) {
            totalSpent += r.amountPaid;
            totalDiscounts += r.operatorDiscount;
            if ("RECEIVED".equalsIgnoreCase(r.status)) {
                totalReceived += r.receivedCashback;
            } else if ("PENDING".equalsIgnoreCase(r.status) || "EXPECTED".equalsIgnoreCase(r.status)) {
                totalPending += r.expectedCashback;
            }
        }

        double totalSavings = totalDiscounts + totalReceived;
        binding.tvTotalSavingsAggregate.setText("₹" + (int) totalSavings);
        binding.tvTotalSpentVal.setText("₹" + (int) totalSpent);
        binding.tvTotalReceivedVal.setText("₹" + (int) totalReceived);
        binding.tvTotalPendingVal.setText("₹" + (int) totalPending);
    }

    @Override
    public void onUpdateStatus(SavingsRecordEntity record) {
        if (getContext() == null) return;
        String[] options = {"Mark as RECEIVED", "Mark as PENDING", "Mark as REJECTED", "Mark as EXPIRED"};

        new AlertDialog.Builder(getContext())
                .setTitle("Update Cashback Status")
                .setItems(options, (dialog, which) -> {
                    String selectedStatus = "PENDING";
                    double received = 0;
                    double effective = record.amountPaid;

                    if (which == 0) {
                        selectedStatus = "RECEIVED";
                        received = record.expectedCashback > 0 ? record.expectedCashback : 50.0;
                        effective = Math.max(0, record.amountPaid - received);
                    } else if (which == 1) {
                        selectedStatus = "PENDING";
                    } else if (which == 2) {
                        selectedStatus = "REJECTED";
                    } else if (which == 3) {
                        selectedStatus = "EXPIRED";
                    }

                    viewModel.updateStatus(record.id, selectedStatus, received, effective);
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
