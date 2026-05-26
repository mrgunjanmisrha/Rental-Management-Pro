package com.gtechapps.ramjankimandir.model;

import java.util.HashMap;
import java.util.Map;

public class AppUser {

    public String uid = "";
    public String name = "";
    public String email_id = "";
    public String role = UserRole.USER;
    public boolean room_p = false;
    public boolean garage_p = false;
    public boolean super_admin = false;
    public String createAt = "";
    public String session_id = "";

    public String email = "";
    public String displayName = "";
    public String phone = "";
    public boolean approved = true;
    public Map<String, Boolean> modulePermissions = new HashMap<>();
    public Map<String, Boolean> actionPermissions = new HashMap<>();

    public void normalize() {
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = name;
        }
        if (name == null || name.trim().isEmpty()) {
            name = displayName;
        }
        if (email == null || email.trim().isEmpty()) {
            email = email_id;
        }
        if (email_id == null || email_id.trim().isEmpty()) {
            email_id = email;
        }
        if (modulePermissions == null) {
            modulePermissions = new HashMap<>();
        }
        if (actionPermissions == null) {
            actionPermissions = new HashMap<>();
        }
        super_admin = super_admin || UserRole.SUPER_ADMIN.equals(role);
        modulePermissions.put(ModuleScope.ROOM, room_p || isOwnerOrAdmin() || boolValue(modulePermissions.get(ModuleScope.ROOM)));
        modulePermissions.put(ModuleScope.GARAGE, garage_p || isOwnerOrAdmin() || boolValue(modulePermissions.get(ModuleScope.GARAGE)));
        modulePermissions.put(ModuleScope.SETTINGS, isOwnerOrAdmin() || boolValue(modulePermissions.get(ModuleScope.SETTINGS)));
        actionPermissions.put(ActionPermission.CREATE, isOwnerOrAdmin() || boolValue(actionPermissions.get(ActionPermission.CREATE)));
        actionPermissions.put(ActionPermission.UPDATE, isOwnerOrAdmin() || boolValue(actionPermissions.get(ActionPermission.UPDATE)));
        actionPermissions.put(ActionPermission.DELETE, isOwnerOrAdmin() || boolValue(actionPermissions.get(ActionPermission.DELETE)));
        actionPermissions.put(ActionPermission.COLLECT_PAYMENT, isOwnerOrAdmin() || boolValue(actionPermissions.get(ActionPermission.COLLECT_PAYMENT)));
    }

    public boolean canAccessModule(String module) {
        normalize();
        if (isOwnerOrAdmin()) {
            return true;
        }
        return boolValue(modulePermissions.get(module));
    }

    public boolean canPerformAction(String action) {
        normalize();
        if (isOwnerOrAdmin()) {
            return true;
        }
        return boolValue(actionPermissions.get(action));
    }

    public boolean isSuperAdmin() {
        return super_admin || UserRole.SUPER_ADMIN.equals(role);
    }

    public boolean isOwnerOrAdmin() {
        return isSuperAdmin() || UserRole.ADMIN.equals(role);
    }

    private boolean boolValue(Boolean value) {
        return value != null && value;
    }
}
