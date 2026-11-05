package com.bestrecharge.app.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bestrecharge.app.R;
import com.bestrecharge.app.data.local.entities.SimProfileEntity;
import com.bestrecharge.app.data.repository.SimRepository;
import com.bestrecharge.app.databinding.ActivityOnboardingBinding;
import com.bestrecharge.app.ui.main.MainActivity;

import java.util.UUID;

public class OnboardingActivity extends AppCompatActivity {

    private ActivityOnboardingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnOnboardingContinue.setOnClickListener(v -> {
            String mobile = binding.etOnboardingMobile.getText().toString().trim();
            if (mobile.isEmpty()) {
                mobile = "98478 37607";
            }

            String opCode = "VI";
            String opName = "Vodafone Idea (Vi)";
            int checkedId = binding.rgOperator.getCheckedRadioButtonId();
            if (checkedId == R.id.rb_jio) {
                opCode = "JIO";
                opName = "Reliance Jio";
            } else if (checkedId == R.id.rb_airtel) {
                opCode = "AIRTEL";
                opName = "Bharti Airtel";
            } else if (checkedId == R.id.rb_bsnl) {
                opCode = "BSNL";
                opName = "BSNL";
            }

            SimProfileEntity sim = new SimProfileEntity(
                    "sim_" + UUID.randomUUID().toString().substring(0, 8),
                    mobile,
                    opCode,
                    opName,
                    "DL",
                    "Delhi NCR",
                    "SIM 1",
                    true,
                    true
            );

            new SimRepository(getApplication()).insertOrUpdateSim(sim);

            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
