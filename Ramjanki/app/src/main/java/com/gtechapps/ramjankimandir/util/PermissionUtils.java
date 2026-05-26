package com.gtechapps.ramjankimandir.util;

import com.gtechapps.ramjankimandir.model.ActionPermission;
import com.gtechapps.ramjankimandir.model.AppUser;
import com.gtechapps.ramjankimandir.model.ModuleScope;
import com.gtechapps.ramjankimandir.model.UserRole;

import java.util.ArrayList;
import java.util.List;

public final class PermissionUtils {

    private PermissionUtils() {
    }

    public static String roleLabel(AppUser appUser) {
        if (appUser == null) {
            return "User";
        }
        if (UserRole.SUPER_ADMIN.equals(appUser.role)) {
            return "Super Admin";
        }
        if (UserRole.ADMIN.equals(appUser.role)) {
            return "Admin";
        }
        if (UserRole.MANAGER.equals(appUser.role)) {
            return "Manager";
        }
        return "User";
    }

    // PermissionUtils.java mein ye static method add karein

    public interface ApprovalCallback {
        void onResult(boolean isApproved);
    }

    /**
     * Ye function check karega ki current user approved hai ya nahi.
     * Ise aap kahi bhi use kar sakte hain.
     */
    public static void checkUserApproval(ApprovalCallback callback) {
        com.google.firebase.auth.FirebaseUser firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null) {
            callback.onResult(false);
            return;
        }

        com.google.firebase.database.DatabaseReference userRef =
                com.google.firebase.database.FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(firebaseUser.getUid());

        userRef.child("approved").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                Boolean approved = snapshot.getValue(Boolean.class);
                // Agar database mein field nahi hai to default false maan rahe hain
                callback.onResult(approved != null && approved);
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                callback.onResult(false);
            }
        });
    }

    public static String permissionsSummary(AppUser appUser) {
        if (appUser == null) {
            return "No access assigned.";
        }
        appUser.normalize();
        List<String> items = new ArrayList<>();
        if (appUser.canAccessModule(ModuleScope.ROOM)) {
            items.add("Room module");
        }
        if (appUser.canAccessModule(ModuleScope.GARAGE)) {
            items.add("Garage module");
        }
        if (appUser.canAccessModule(ModuleScope.SETTINGS)) {
            items.add("Settings");
        }
        if (appUser.canPerformAction(ActionPermission.CREATE)) {
            items.add("Create records");
        }
        if (appUser.canPerformAction(ActionPermission.UPDATE)) {
            items.add("Update records");
        }
        if (appUser.canPerformAction(ActionPermission.COLLECT_PAYMENT)) {
            items.add("Collect payments");
        }
        if (items.isEmpty()) {
            return "No access assigned.";
        }
        return String.join(" - ", items);
    }
}
