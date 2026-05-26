package com.gtechapps.ramjankimandir.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.gtechapps.ramjankimandir.R;
import com.gtechapps.ramjankimandir.data.RentalRepository;
import com.gtechapps.ramjankimandir.data.RepositoryCallback;
import com.gtechapps.ramjankimandir.model.Garage;
import com.gtechapps.ramjankimandir.model.RecordItem;
import com.gtechapps.ramjankimandir.model.RentalPerson;
import com.gtechapps.ramjankimandir.model.Room;
import com.gtechapps.ramjankimandir.model.VehicleEntry;

import java.util.List;

public class RecordListFragment extends Fragment {

    private static final String ARG_TYPE = "type";

    public static RecordListFragment newInstance(String type) {
        RecordListFragment fragment = new RecordListFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_TYPE, type);
        fragment.setArguments(bundle);
        return fragment;
    }

    private final RentalRepository repository = new RentalRepository();
    private RecordAdapter adapter;
    private TextView emptyState;
    private SwipeRefreshLayout refreshLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record_list, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.listRecyclerView);
        emptyState = view.findViewById(R.id.listEmptyState);
        refreshLayout = view.findViewById(R.id.listRefresh);
        adapter = new RecordAdapter(item ->
                startActivity(FormActivity.createEditIntent(requireContext(), requireType(), item.recordId)), getString(R.string.edit));
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        refreshLayout.setOnRefreshListener(this::loadData);
        loadData();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        adapter = null;
        emptyState = null;
        refreshLayout = null;
    }

    private String requireType() {
        Bundle args = getArguments();
        return args == null ? RentalRepository.TYPE_ROOM : args.getString(ARG_TYPE, RentalRepository.TYPE_ROOM);
    }

    private void loadData() {
        if (!isUiReady()) {
            return;
        }
        refreshLayout.setRefreshing(true);
        String type = requireType();
        if (RentalRepository.TYPE_ROOM.equals(type)) {
            repository.loadRooms(new RepositoryCallback<List<Room>>() {
                @Override
                public void onSuccess(List<Room> data) {
                    if (!isUiReady()) {
                        return;
                    }
                    showItems(repository.toRoomItems(data));
                }

                @Override
                public void onError(String message) {
                    if (!isUiReady()) {
                        return;
                    }
                    showEmpty(message);
                }
            });
            return;
        }
        if (RentalRepository.TYPE_GARAGE.equals(type)) {
            repository.loadGarages(new RepositoryCallback<List<Garage>>() {
                @Override
                public void onSuccess(List<Garage> data) {
                    if (!isUiReady()) {
                        return;
                    }
                    showItems(repository.toGarageItems(data));
                }

                @Override
                public void onError(String message) {
                    if (!isUiReady()) {
                        return;
                    }
                    showEmpty(message);
                }
            });
            return;
        }
        if (RentalRepository.TYPE_RENTAL.equals(type)) {
            repository.loadRentals(new RepositoryCallback<List<RentalPerson>>() {
                @Override
                public void onSuccess(List<RentalPerson> data) {
                    if (!isUiReady()) {
                        return;
                    }
                    showItems(repository.toRentalItems(data));
                }

                @Override
                public void onError(String message) {
                    if (!isUiReady()) {
                        return;
                    }
                    showEmpty(message);
                }
            });
            return;
        }
        repository.loadVehicles(new RepositoryCallback<List<VehicleEntry>>() {
            @Override
            public void onSuccess(List<VehicleEntry> data) {
                if (!isUiReady()) {
                    return;
                }
                showItems(repository.toVehicleItems(data));
            }

            @Override
            public void onError(String message) {
                if (!isUiReady()) {
                    return;
                }
                showEmpty(message);
            }
        });
    }

    private void showItems(List<RecordItem> items) {
        refreshLayout.setRefreshing(false);
        emptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.submitItems(items);
    }

    private void showEmpty(String message) {
        refreshLayout.setRefreshing(false);
        emptyState.setVisibility(View.VISIBLE);
        emptyState.setText(message);
        adapter.submitItems(java.util.Collections.emptyList());
    }

    private boolean isUiReady() {
        return isAdded() && getView() != null && adapter != null && emptyState != null && refreshLayout != null;
    }
}
