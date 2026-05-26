package com.gtechapps.ramjankimandir.ui.settings;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.gtechapps.ramjankimandir.R;
import com.gtechapps.ramjankimandir.data.RentalRepository;
import com.gtechapps.ramjankimandir.model.RecordItem;
import com.gtechapps.ramjankimandir.ui.common.FormActivity;
import com.gtechapps.ramjankimandir.ui.common.MonthManagementActivity;
import com.gtechapps.ramjankimandir.ui.common.PaymentSetupActivity;
import com.gtechapps.ramjankimandir.ui.common.RecordsActivity;
import com.gtechapps.ramjankimandir.ui.user.UserListActivity;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private final RentalRepository repository = new RentalRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.settingsToolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.actionsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new ActionAdapter(buildActions(), this::handleAction));
    }

    private List<RecordItem> buildActions() {
        List<RecordItem> actions = new ArrayList<>();
        actions.add(new RecordItem("create_room", getString(R.string.create_room), "Create a room master record.", "", ""));
        actions.add(new RecordItem("create_garage", getString(R.string.create_garage), "Create a garage master record.", "", ""));
        actions.add(new RecordItem("create_rental", getString(R.string.rental_entry), "Create or update a rental person assignment.", "", ""));
        actions.add(new RecordItem("create_vehicle", getString(R.string.vehicle_entry), "Create or update a vehicle owner assignment.", "", ""));
        actions.add(new RecordItem("view_data", getString(R.string.view_created_data), "Open tabbed room, garage, rental, and vehicle lists.", "", ""));
        actions.add(new RecordItem("month_list", getString(R.string.month_list), "Review created months and add the next billing month.", "", ""));
        actions.add(new RecordItem("user_permission", getString(R.string.user_list), "View app users and manage module permissions.", "", ""));
        actions.add(new RecordItem("setup_payment", getString(R.string.setup_payment), "Prefill monthly room and garage payment nodes.", "", ""));
        return actions;
    }

    private void handleAction(RecordItem action) {
        switch (action.recordId) {
            case "create_room":
                startActivity(FormActivity.createCreateIntent(this, RentalRepository.TYPE_ROOM));
                return;
            case "create_garage":
                startActivity(FormActivity.createCreateIntent(this, RentalRepository.TYPE_GARAGE));
                return;
            case "create_rental":
                startActivity(FormActivity.createCreateIntent(this, RentalRepository.TYPE_RENTAL));
                return;
            case "create_vehicle":
                startActivity(FormActivity.createCreateIntent(this, RentalRepository.TYPE_VEHICLE));
                return;
            case "view_data":
                startActivity(new Intent(this, RecordsActivity.class));
                return;
            case "month_list":
                startActivity(new Intent(this, MonthManagementActivity.class));
                return;
            case "user_permission":
                startActivity(new Intent(this, UserListActivity.class));
                return;
            case "setup_payment":
                startActivity(new Intent(this, PaymentSetupActivity.class));
                return;
            default:
        }
    }
}
