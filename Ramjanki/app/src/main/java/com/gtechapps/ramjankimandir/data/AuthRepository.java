package com.gtechapps.ramjankimandir.data;

import androidx.annotation.NonNull;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.gtechapps.ramjankimandir.model.ActionPermission;
import com.gtechapps.ramjankimandir.model.AppUser;
import com.gtechapps.ramjankimandir.model.ModuleScope;
import com.gtechapps.ramjankimandir.model.UserRole;
import com.gtechapps.ramjankimandir.util.TimeUtils;

import java.util.UUID;

public class AuthRepository {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void signInWithEmail(String email, String password, RepositoryCallback<FirebaseUser> callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onSuccess(result.getUser()))
                .addOnFailureListener(error -> callback.onError(error.getMessage()));
    }

    public void signInWithGoogle(String idToken, RepositoryCallback<FirebaseUser> callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> callback.onSuccess(result.getUser()))
                .addOnFailureListener(error -> callback.onError(error.getMessage()));
    }

    public void ensureUserProfile(FirebaseUser firebaseUser, RepositoryCallback<AppUser> callback) {
        if (firebaseUser == null) {
            callback.onError("No signed-in user found.");
            return;
        }
        usersRef.child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                AppUser appUser = snapshot.getValue(AppUser.class);
                if (appUser == null) {
                    appUser = buildNewUser(firebaseUser);
                } else {
                    appUser.normalize();
                    appUser.uid = firebaseUser.getUid();
                    appUser.name = blankSafe(appUser.name, firebaseUser.getDisplayName());
                    appUser.displayName = blankSafe(appUser.displayName, firebaseUser.getDisplayName());
                    appUser.email_id = blankSafe(appUser.email_id, firebaseUser.getEmail());
                    appUser.email = blankSafe(appUser.email, firebaseUser.getEmail());
                    appUser.session_id = UUID.randomUUID().toString();
                    if (appUser.createAt == null || appUser.createAt.trim().isEmpty()) {
                        appUser.createAt = TimeUtils.nowIsoTimestamp();
                    }
                }
                AppUser profile = appUser;
                usersRef.child(firebaseUser.getUid()).setValue(profile)
                        .addOnSuccessListener(unused -> callback.onSuccess(profile))
                        .addOnFailureListener(error -> callback.onError(error.getMessage()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void signOut() {
        auth.signOut();
    }

    private AppUser buildNewUser(FirebaseUser firebaseUser) {
        AppUser appUser = new AppUser();
        appUser.uid = firebaseUser.getUid();
        appUser.name = blankSafe(firebaseUser.getDisplayName(), "User");
        appUser.displayName = appUser.name;
        appUser.email_id = blankSafe(firebaseUser.getEmail(), "");
        appUser.email = appUser.email_id;
        appUser.role = UserRole.USER;
        appUser.room_p = false;
        appUser.garage_p = false;
        appUser.super_admin = false;
        appUser.approved = false;
        appUser.createAt = TimeUtils.nowIsoTimestamp();
        appUser.session_id = UUID.randomUUID().toString();
        appUser.modulePermissions.put(ModuleScope.ROOM, false);
        appUser.modulePermissions.put(ModuleScope.GARAGE, false);
        appUser.modulePermissions.put(ModuleScope.SETTINGS, false);
        appUser.actionPermissions.put(ActionPermission.CREATE, false);
        appUser.actionPermissions.put(ActionPermission.UPDATE, false);
        appUser.actionPermissions.put(ActionPermission.DELETE, false);
        appUser.actionPermissions.put(ActionPermission.COLLECT_PAYMENT, false);
        return appUser;
    }

    private String blankSafe(String primary, String fallback) {
        if (primary != null && !primary.trim().isEmpty()) {
            return primary;
        }
        return fallback == null ? "" : fallback;
    }
}
