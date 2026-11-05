package com.bestrecharge.app.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bestrecharge.app.R;
import com.bestrecharge.app.data.local.entities.SimProfileEntity;
import com.bestrecharge.app.data.repository.SimRepository;
import com.bestrecharge.app.databinding.DialogChangeNumberBinding;

public class ChangeNumberDialog extends Dialog {

    public interface OnNumberSelectedListener {
        void onNumberSelected(SimProfileEntity simProfile);
    }

    private DialogChangeNumberBinding binding;
    private final SimProfileEntity currentSim;
    private final OnNumberSelectedListener listener;

    private static final String[] CIRCLES = {
            "Delhi NCR", "Mumbai", "Maharashtra & Goa", "Karnataka",
            "Tamil Nadu", "Andhra Pradesh & Telangana", "Kerala", "Gujarat",
            "Punjab", "West Bengal", "Kolkata", "UP East", "UP West",
            "Bihar & Jharkhand", "Rajasthan", "Madhya Pradesh", "Haryana"
    };

    public ChangeNumberDialog(@NonNull Context context, SimProfileEntity currentSim, OnNumberSelectedListener listener) {
        super(context);
        this.currentSim = currentSim;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DialogChangeNumberBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getWindow() != null) {
            getWindow().setLayout(
                    (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // Setup Spinner
        ArrayAdapter<String> circleAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_dropdown_item, CIRCLES
        );
        binding.spinnerDialogCircle.setAdapter(circleAdapter);

        // Pre-fill existing data if available
        if (currentSim != null) {
            if (currentSim.mobileNumber != null && !currentSim.mobileNumber.isEmpty()) {
                binding.etDialogMobileNumber.setText(currentSim.mobileNumber);
            }
            if ("JIO".equalsIgnoreCase(currentSim.operatorCode)) {
                binding.rbDialogJio.setChecked(true);
            } else if ("AIRTEL".equalsIgnoreCase(currentSim.operatorCode)) {
                binding.rbDialogAirtel.setChecked(true);
            } else if ("BSNL".equalsIgnoreCase(currentSim.operatorCode)) {
                binding.rbDialogBsnl.setChecked(true);
            } else {
                binding.rbDialogVi.setChecked(true);
            }
        }

        binding.btnDialogCancel.setOnClickListener(v -> dismiss());

        binding.btnDialogApply.setOnClickListener(v -> {
            String number = binding.etDialogMobileNumber.getText().toString().trim();
            if (number.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a valid mobile number", Toast.LENGTH_SHORT).show();
                return;
            }

            String opCode = "VI";
            String opName = "Vodafone Idea (Vi)";
            int checkedId = binding.rgDialogOperators.getCheckedRadioButtonId();
            if (checkedId == R.id.rb_dialog_jio) {
                opCode = "JIO";
                opName = "Reliance Jio";
            } else if (checkedId == R.id.rb_dialog_airtel) {
                opCode = "AIRTEL";
                opName = "Bharti Airtel";
            } else if (checkedId == R.id.rb_dialog_bsnl) {
                opCode = "BSNL";
                opName = "BSNL";
            }

            String circle = (String) binding.spinnerDialogCircle.getSelectedItem();
            if (circle == null) circle = "Delhi NCR";

            SimProfileEntity updatedSim = new SimProfileEntity(
                    "sim_active_custom",
                    number,
                    opCode,
                    opName,
                    "DL",
                    circle,
                    "ACTIVE RECHARGE",
                    true,
                    true
            );

            // Persist to database
            new SimRepository((android.app.Application) getContext().getApplicationContext())
                    .insertOrUpdateSim(updatedSim);

            if (listener != null) {
                listener.onNumberSelected(updatedSim);
            }

            dismiss();
        });
    }
}
