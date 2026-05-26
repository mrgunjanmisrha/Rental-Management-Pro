package com.gtechapps.ramjankimandir.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
import com.gtechapps.ramjankimandir.R;
import com.gtechapps.ramjankimandir.data.AuthRepository;
import com.gtechapps.ramjankimandir.data.RepositoryCallback;
import com.gtechapps.ramjankimandir.model.AppUser;
import com.gtechapps.ramjankimandir.ui.home.MainActivity;
import com.gtechapps.ramjankimandir.util.FirebaseSetup;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private ProgressBar progressBar;
    private TextView setupStatusText;
    private MaterialButton googleLoginButton;

    private final AuthRepository authRepository = new AuthRepository();
    @Nullable
    private GoogleSignInClient googleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() == null) {
                    setLoading(false);
                    return;
                }
                try {
                    GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                            .getResult(ApiException.class);
                    if (account == null || account.getIdToken() == null) {
                        setLoading(false);
                        showMessage("Google sign-in token not available.");
                        return;
                    }
                    signInWithGoogleToken(account.getIdToken());
                } catch (ApiException exception) {
                    setLoading(false);
                    showMessage(exception.getMessage() == null ? "Google sign-in failed." : exception.getMessage());
                }
            });

    private final RepositoryCallback<FirebaseUser> authCallback = new RepositoryCallback<FirebaseUser>() {
        @Override
        public void onSuccess(FirebaseUser data) {
            authRepository.ensureUserProfile(data, new RepositoryCallback<AppUser>() {
                @Override
                public void onSuccess(AppUser user) {
                    setLoading(false);
                    startActivity(MainActivity.createIntent(LoginActivity.this, user));
                    finish();
                }

                @Override
                public void onError(String message) {
                    setLoading(false);
                    showMessage(message);
                }
            });
        }

        @Override
        public void onError(String message) {
            setLoading(false);
            showMessage(message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        progressBar = findViewById(R.id.loginProgress);
        setupStatusText = findViewById(R.id.setupStatusText);
        googleLoginButton = findViewById(R.id.googleLoginButton);

        findViewById(R.id.emailLoginButton).setOnClickListener(v -> handleEmailLogin());
        googleLoginButton.setOnClickListener(v -> handleGoogleLogin());
        syncSetupState();
    }

    private void handleEmailLogin() {

        String email = valueOf(emailEditText);
        String password = valueOf(passwordEditText);
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showMessage("Email and password are required.");
            return;
        }
        setLoading(true);
        authRepository.signInWithEmail(email, password, authCallback);
    }

    private void handleGoogleLogin() {
        String webClientId = FirebaseSetup.defaultWebClientId(this);
        if (TextUtils.isEmpty(webClientId)) {
            showMessage("default_web_client_id is missing from Firebase config.");
            return;
        }
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(webClientId)
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, options);
        setLoading(true);
        googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
    }

    private void signInWithGoogleToken(String idToken) {
        authRepository.signInWithGoogle(idToken, authCallback);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void syncSetupState() {
        boolean configured = FirebaseSetup.isConfigured() && !TextUtils.isEmpty(FirebaseSetup.defaultWebClientId(this));
        setupStatusText.setText(configured ? R.string.firebase_setup_ready : R.string.firebase_setup_missing);
        googleLoginButton.setEnabled(configured);
    }

    private void showMessage(String message) {
        Snackbar.make(findViewById(R.id.loginRoot), message, Snackbar.LENGTH_LONG).show();
    }

    private String valueOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
