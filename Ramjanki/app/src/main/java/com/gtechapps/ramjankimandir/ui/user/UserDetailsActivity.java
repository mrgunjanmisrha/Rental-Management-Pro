package com.gtechapps.ramjankimandir.ui.user;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.gtechapps.ramjankimandir.R;
import com.gtechapps.ramjankimandir.model.AppUser;
import com.gtechapps.ramjankimandir.model.UserRole;

import java.util.HashMap;
import java.util.Map;

public class UserDetailsActivity extends AppCompatActivity {

    private static final String EXTRA_USER_ID = "extra_user_id";

    public static Intent createIntent(Context context, String userId) {
        Intent intent = new Intent(context, UserDetailsActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        return intent;
    }

    private final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

    private String userId;
    private boolean canEditPermissions;
    private AppUser targetUser;

    private TextView nameView;
    private TextView emailView;
    private TextView roleView;
    private TextView userIdView;
    private TextView createdView;
    private TextView adminNoticeView;
    private SwitchCompat switchApproved;
    private SwitchCompat switchSuperAdmin;
    private SwitchCompat switchRoom;
    private SwitchCompat switchGarage;
    private MaterialButton updateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        userId = getIntent().getStringExtra(EXTRA_USER_ID);

        MaterialToolbar toolbar = findViewById(R.id.userDetailsToolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();
        setPermissionControlsEnabled(false);
        updateButton.setOnClickListener(v -> updatePermissions());

        if (isBlank(userId)) {
            showMessage("User id missing.");
            finish();
            return;
        }
        loadCurrentUserAccess();
    }

    private void bindViews() {
        nameView = findViewById(R.id.detailUserName);
        emailView = findViewById(R.id.detailUserEmail);
        roleView = findViewById(R.id.detailUserRole);
        userIdView = findViewById(R.id.detailUserId);
        createdView = findViewById(R.id.detailUserCreated);
        adminNoticeView = findViewById(R.id.adminNotice);
        switchApproved = findViewById(R.id.switchApproved);
        switchSuperAdmin = findViewById(R.id.switchSuperAdmin);
        switchRoom = findViewById(R.id.switchRoomPermission);
        switchGarage = findViewById(R.id.switchGaragePermission);
        updateButton = findViewById(R.id.btnUpdatePermissions);
    }

    private void loadCurrentUserAccess() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            canEditPermissions = false;
            loadTargetUser();
            return;
        }
        usersRef.child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                AppUser currentUser = snapshot.getValue(AppUser.class);
                if (currentUser != null) {
                    currentUser.uid = snapshot.getKey();
                    currentUser.normalize();
                }
                canEditPermissions = currentUser != null && currentUser.isSuperAdmin();
                loadTargetUser();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                canEditPermissions = false;
                loadTargetUser();
            }
        });
    }

    private void loadTargetUser() {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                AppUser user = snapshot.getValue(AppUser.class);
                if (user == null) {
                    showMessage("User not found.");
                    finish();
                    return;
                }
                user.uid = snapshot.getKey();
                user.normalize();
                targetUser = user;
                bindUser(user);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showMessage(error.getMessage());
            }
        });
    }

    private void bindUser(AppUser user) {
        nameView.setText(safeFallback(user.name, "Unknown User"));
        emailView.setText(safeFallback(user.email_id, "-"));
        roleView.setText(user.isSuperAdmin() ? UserRole.SUPER_ADMIN : safeFallback(user.role, UserRole.USER));
        userIdView.setText("User id: " + safeFallback(user.uid, "-"));
        createdView.setText("Created: " + safeFallback(user.createAt, "-"));

        switchApproved.setChecked(user.approved);
        switchSuperAdmin.setChecked(user.isSuperAdmin());
        switchRoom.setChecked(user.room_p);
        switchGarage.setChecked(user.garage_p);
        setPermissionControlsEnabled(canEditPermissions);
        adminNoticeView.setVisibility(canEditPermissions ? View.GONE : View.VISIBLE);
        adminNoticeView.setText("Only super admin can change user permissions.");
    }

    private void setPermissionControlsEnabled(boolean enabled) {
        switchApproved.setEnabled(enabled);
        switchSuperAdmin.setEnabled(enabled);
        switchRoom.setEnabled(enabled);
        switchGarage.setEnabled(enabled);
        updateButton.setEnabled(enabled);
    }

    private void updatePermissions() {
        if (!canEditPermissions) {
            showMessage("Only super admin can update permissions.");
            return;
        }
        boolean superAdmin = switchSuperAdmin.isChecked();
        String role = targetUser == null ? UserRole.USER : safeFallback(targetUser.role, UserRole.USER);
        if (superAdmin) {
            role = UserRole.SUPER_ADMIN;
        } else if (UserRole.SUPER_ADMIN.equals(role)) {
            role = UserRole.USER;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("approved", switchApproved.isChecked());
        updates.put("super_admin", superAdmin);
        updates.put("role", role);
        updates.put("room_p", switchRoom.isChecked());
        updates.put("garage_p", switchGarage.isChecked());
        updates.put("modulePermissions/room", switchRoom.isChecked());
        updates.put("modulePermissions/garage", switchGarage.isChecked());

        usersRef.child(userId).updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    showMessage("Permissions updated successfully.");
                    loadTargetUser();
                })
                .addOnFailureListener(error -> showMessage("Failed to update permissions: " + error.getMessage()));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safeFallback(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
