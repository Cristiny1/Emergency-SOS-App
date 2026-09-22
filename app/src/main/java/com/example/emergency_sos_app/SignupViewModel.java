package com.example.emergency_sos_app;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SignupViewModel extends ViewModel {

    // ==========================================
    // STEP 1 - PERSONAL INFORMATION
    // ==========================================

    public final MutableLiveData<String> fullName =
            new MutableLiveData<>("");

    public final MutableLiveData<String> username =
            new MutableLiveData<>("");

    public final MutableLiveData<String> dob =
            new MutableLiveData<>("");

    public final MutableLiveData<String> gender =
            new MutableLiveData<>("");

    // ==========================================
    // STEP 2 - CONTACT INFORMATION
    // ==========================================

    public final MutableLiveData<String> email =
            new MutableLiveData<>("");

    public final MutableLiveData<String> phone =
            new MutableLiveData<>("");

    public final MutableLiveData<Boolean> isVerified =
            new MutableLiveData<>(false);

    // ==========================================
    // STEP 3 - SECURITY
    // ==========================================

    public final MutableLiveData<String> password =
            new MutableLiveData<>("");

    public final MutableLiveData<String> confirmPassword =
            new MutableLiveData<>("");

    public final MutableLiveData<Boolean> termsAgreed =
            new MutableLiveData<>(false);
}
