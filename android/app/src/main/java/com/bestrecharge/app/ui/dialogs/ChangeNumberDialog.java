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
                String cleaned = currentSim.mobileNumber.replaceAll("[^0-9]", "");
                binding.etDialogMobileNumber.setText(cleaned);
                binding.etDialogMobileNumber.setSelection(cleaned.length());

                // Auto-detect allocated operator and circle
                com.bestrecharge.app.utils.TelecomNumberSeriesHelper.AllocationInfo alloc =
                        com.bestrecharge.app.utils.TelecomNumberSeriesHelper.getAllocation(cleaned);
                if (alloc != null) {
                    selectOperatorRadio(alloc.operatorCode);
                    selectCircleInSpinner(alloc.circleName);
                } else {
                    selectOperatorRadio(currentSim.operatorCode);
                    selectCircleInSpinner(currentSim.circleName);
                }
            } else {
                selectOperatorRadio(currentSim.operatorCode);
                selectCircleInSpinner(currentSim.circleName);
            }
        }

        binding.etDialogMobileNumber.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.length() >= 4) {
                    com.bestrecharge.app.utils.TelecomNumberSeriesHelper.AllocationInfo alloc =
                            com.bestrecharge.app.utils.TelecomNumberSeriesHelper.getAllocation(s.toString());
                    if (alloc != null) {
                        selectOperatorRadio(alloc.operatorCode);
                        selectCircleInSpinner(alloc.circleName);
                    }
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        binding.btnDialogCancel.setOnClickListener(v -> dismiss());

        binding.btnDialogApply.setOnClickListener(v -> {
            String rawInput = binding.etDialogMobileNumber.getText() != null ?
                    binding.etDialogMobileNumber.getText().toString().trim() : "";
            String number = rawInput.replaceAll("[^0-9]", "");
            if (number.length() > 10) {
                number = number.substring(number.length() - 10);
            }

            if (number.length() != 10 || !number.matches("^[6-9]\\d{9}$")) {
                Toast.makeText(getContext(), "Please enter a valid 10-digit mobile number (starts with 6-9)", Toast.LENGTH_LONG).show();
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

            String targetSimId = (currentSim != null && currentSim.id != null) ? currentSim.id : "sim_1";

            SimProfileEntity updatedSim = new SimProfileEntity(
                    targetSimId,
                    number,
                    opCode,
                    opName,
                    "DL",
                    circle,
                    "SIM 1",
                    true,
                    true,
                    currentSim != null ? currentSim.subscriptionId : -1,
                    opName,
                    currentSim != null ? currentSim.mccMnc : "",
                    "in",
                    "READY",
                    SimProfileEntity.STATE_USER_CONFIRMED,
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

    private void selectOperatorRadio(String opCode) {
        if ("JIO".equalsIgnoreCase(opCode)) {
            binding.rbDialogJio.setChecked(true);
        } else if ("AIRTEL".equalsIgnoreCase(opCode)) {
            binding.rbDialogAirtel.setChecked(true);
        } else if ("BSNL".equalsIgnoreCase(opCode)) {
            binding.rbDialogBsnl.setChecked(true);
        } else {
            binding.rbDialogVi.setChecked(true);
        }
    }

    private void selectCircleInSpinner(String circleName) {
        if (circleName == null) return;
        for (int i = 0; i < CIRCLES.length; i++) {
            if (CIRCLES[i].equalsIgnoreCase(circleName)) {
                binding.spinnerDialogCircle.setSelection(i);
                break;
            }
        }
    }
}
