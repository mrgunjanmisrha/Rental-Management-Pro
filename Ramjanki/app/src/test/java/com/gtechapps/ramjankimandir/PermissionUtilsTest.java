package com.gtechapps.ramjankimandir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.gtechapps.ramjankimandir.model.ActionPermission;
import com.gtechapps.ramjankimandir.model.AppUser;
import com.gtechapps.ramjankimandir.model.ModuleScope;
import com.gtechapps.ramjankimandir.model.UserRole;
import com.gtechapps.ramjankimandir.util.PermissionUtils;

import org.junit.Test;

public class PermissionUtilsTest {

    @Test
    public void roleLabel_returnsSuperAdminForOwnerRole() {
        AppUser appUser = new AppUser();
        appUser.role = UserRole.SUPER_ADMIN;

        assertEquals("Super Admin", PermissionUtils.roleLabel(appUser));
        assertTrue(appUser.canAccessModule(ModuleScope.ROOM));
        assertTrue(appUser.canPerformAction(ActionPermission.DELETE));
    }

    @Test
    public void permissionsSummary_listsEnabledManagerAccess() {
        AppUser appUser = new AppUser();
        appUser.role = UserRole.MANAGER;
        appUser.modulePermissions.put(ModuleScope.ROOM, true);
        appUser.actionPermissions.put(ActionPermission.COLLECT_PAYMENT, true);

        String summary = PermissionUtils.permissionsSummary(appUser);

        assertTrue(summary.contains("Room module"));
        assertTrue(summary.contains("Collect payments"));
    }
}
