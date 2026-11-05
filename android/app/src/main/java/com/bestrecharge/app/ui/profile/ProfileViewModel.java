package com.bestrecharge.app.ui.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.bestrecharge.app.data.local.entities.SimProfileEntity;
import com.bestrecharge.app.data.repository.SimRepository;

import java.util.List;

public class ProfileViewModel extends AndroidViewModel {
    private final SimRepository repository;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        this.repository = new SimRepository(application);
    }

    public LiveData<List<SimProfileEntity>> getAllSims() {
        return repository.getAllSimProfiles();
    }
}
