package com.gtechapps.ramjankimandir;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.gtechapps.ramjankimandir.data.AuthRepository;
import com.gtechapps.ramjankimandir.data.RepositoryCallback;
import com.gtechapps.ramjankimandir.model.AppUser;
import com.gtechapps.ramjankimandir.ui.auth.LoginActivity;
import com.gtechapps.ramjankimandir.ui.home.MainActivity;

public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AuthRepository authRepository = new AuthRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        handler.postDelayed(this::routeNext, 1200L);
    }

    private void routeNext() {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            openLogin();
            return;
        }
        authRepository.ensureUserProfile(currentUser, new RepositoryCallback<AppUser>() {
            @Override
            public void onSuccess(AppUser data) {
                openHome(data);
            }

            @Override
            public void onError(String message) {
                openLogin();
            }
        });
    }

    private void openLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void openHome(AppUser appUser) {
        Intent intent = MainActivity.createIntent(this, appUser);
        startActivity(intent);
        finish();
    }
}
