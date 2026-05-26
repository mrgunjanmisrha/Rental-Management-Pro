package com.gtechapps.ramjankimandir.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.gtechapps.ramjankimandir.R;
import com.gtechapps.ramjankimandir.model.AppUser;
import com.gtechapps.ramjankimandir.ui.auth.LoginActivity;
import com.gtechapps.ramjankimandir.ui.settings.SettingsActivity;
import com.gtechapps.ramjankimandir.util.PermissionUtils;

public class MainActivity extends AppCompatActivity {

    private static final String EXTRA_NAME = "extra_name";
    private static final String EXTRA_ROLE = "extra_role";
    private static final String EXTRA_ROOM_P = "extra_room_p";
    private static final String EXTRA_GARAGE_P = "extra_garage_p";
    private static final String EXTRA_APPROVED = "extra_approved";
    private static final String EXTRA_SUPER_ADMIN = "extra_super_admin";

    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigationView;
    private final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
    private AppUser currentUser = new AppUser();

    public static Intent createIntent(Context context, AppUser appUser) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXTRA_NAME, appUser.name);
        intent.putExtra(EXTRA_ROLE, appUser.role);
        intent.putExtra(EXTRA_ROOM_P, appUser.room_p);
        intent.putExtra(EXTRA_GARAGE_P, appUser.garage_p);
        intent.putExtra(EXTRA_APPROVED, appUser.approved);
        intent.putExtra(EXTRA_SUPER_ADMIN, appUser.isSuperAdmin());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.mainToolbar);
        toolbar.setOnMenuItemClickListener(this::onToolbarItemSelected);

        checkUser();


        currentUser = buildFallbackUserFromIntent();
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                updateToolbar(R.string.home_toolbar_subtitle);
                switchFragment(new DashboardFragment());
                return true;
            }
            if (id == R.id.nav_room) {
                if (!canOpenRoom()) {
                    showMessage("Room permission is required.");
                    return false;
                }
                updateToolbar(R.string.room_toolbar_subtitle);
                switchFragment(new RoomFragment());
                return true;
            }
            if (id == R.id.nav_garage) {
                if (!canOpenGarage()) {
                    showMessage("Garage permission is required.");
                    return false;
                }
                updateToolbar(R.string.garage_toolbar_subtitle);
                switchFragment(new GarageFragment());
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
        loadCurrentUserProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCurrentUserProfile();
    }
    private void checkUser(){
        PermissionUtils.checkUserApproval(isApproved -> {
            if (isApproved) {
            } else {
                Toast.makeText(this, "Account not approved. Contact Admin.", Toast.LENGTH_LONG).show();
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
        });
    }

    private boolean onToolbarItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return false;
    }

    private void updateToolbar(int subtitleRes) {
        toolbar.setTitle(subtitleRes);
    }

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.homeFragmentContainer, fragment)
                .commit();
    }

    private void loadCurrentUserProfile() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            applyPermissionMenu();
            return;
        }
        usersRef.child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                AppUser user = snapshot.getValue(AppUser.class);
                if (user != null) {
                    user.uid = snapshot.getKey();
                    user.normalize();
                    currentUser = user;
                }
                applyPermissionMenu();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                applyPermissionMenu();
            }
        });
    }

    private void applyPermissionMenu() {
        if (bottomNavigationView == null) {
            return;
        }
        bottomNavigationView.getMenu().findItem(R.id.nav_room).setVisible(canOpenRoom());
        bottomNavigationView.getMenu().findItem(R.id.nav_garage).setVisible(canOpenGarage());
        int selectedId = bottomNavigationView.getSelectedItemId();
        if ((selectedId == R.id.nav_room && !canOpenRoom())
                || (selectedId == R.id.nav_garage && !canOpenGarage())) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    private boolean canOpenRoom() {
        return currentUser != null
                && (currentUser.isOwnerOrAdmin() || (currentUser.room_p));
    }

    private boolean canOpenGarage() {
        return currentUser != null
                && (currentUser.isOwnerOrAdmin() || (currentUser.garage_p));
    }

    private AppUser buildFallbackUserFromIntent() {
        AppUser appUser = new AppUser();
        appUser.name = getIntent().getStringExtra(EXTRA_NAME);
        appUser.role = getIntent().getStringExtra(EXTRA_ROLE);
        appUser.room_p = getIntent().getBooleanExtra(EXTRA_ROOM_P, false);
        appUser.garage_p = getIntent().getBooleanExtra(EXTRA_GARAGE_P, false);
        appUser.approved = getIntent().getBooleanExtra(EXTRA_APPROVED, true);
        appUser.super_admin = getIntent().getBooleanExtra(EXTRA_SUPER_ADMIN, false);
        appUser.normalize();
        return appUser;
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
