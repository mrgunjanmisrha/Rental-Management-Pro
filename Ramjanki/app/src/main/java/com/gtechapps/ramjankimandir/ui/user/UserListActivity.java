package com.gtechapps.ramjankimandir.ui.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.gtechapps.ramjankimandir.R;
import com.gtechapps.ramjankimandir.model.AppUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class UserListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        MaterialToolbar toolbar = findViewById(R.id.userListToolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.userRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(new ArrayList<>(), user ->
                startActivity(UserDetailsActivity.createIntent(this, user.uid)));
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout = findViewById(R.id.userSwipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(this::loadUsers);

        loadUsers();
    }

    private void loadUsers() {
        swipeRefreshLayout.setRefreshing(true);
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<AppUser> users = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    AppUser user = child.getValue(AppUser.class);
                    if (user != null) {
                        user.uid = child.getKey();
                        user.normalize();
                        users.add(user);
                    }
                }
                Collections.sort(users, Comparator.comparing(user -> safe(user.name).toLowerCase()));
                adapter.updateUsers(users);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private interface UserClickListener {
        void onUserClick(AppUser user);
    }

    private static class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private final List<AppUser> users;
        private final UserClickListener listener;

        UserAdapter(List<AppUser> users, UserClickListener listener) {
            this.users = users;
            this.listener = listener;
        }

        void updateUsers(List<AppUser> newUsers) {
            users.clear();
            users.addAll(newUsers);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            AppUser user = users.get(position);
            user.normalize();
            holder.title.setText(!safe(user.name).isEmpty() ? user.name : "Unknown User");
            holder.subtitle.setText(!safe(user.email_id).isEmpty() ? user.email_id : "-");
            holder.detail.setText("Role: " + safeFallback(user.role, "user")
                    + "\nRoom permission: " + user.room_p
                    + "\nGarage permission: " + user.garage_p
                    + "\nApproved: " + user.approved);
            holder.status.setText(user.isSuperAdmin() ? "super admin" : safeFallback(user.role, "user"));
            holder.editButton.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(v -> listener.onUserClick(user));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        static class UserViewHolder extends RecyclerView.ViewHolder {
            TextView title, subtitle, detail, status;
            View editButton;

            UserViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.recordTitle);
                subtitle = itemView.findViewById(R.id.recordSubtitle);
                detail = itemView.findViewById(R.id.recordDetail);
                status = itemView.findViewById(R.id.recordStatus);
                editButton = itemView.findViewById(R.id.editRecordButton);
            }
        }

        private String safeFallback(String value, String fallback) {
            return safe(value).isEmpty() ? fallback : value;
        }
    }
}
