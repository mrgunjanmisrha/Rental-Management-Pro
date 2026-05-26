package com.gtechapps.ramjankimandir.ui.common;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.gtechapps.ramjankimandir.data.RentalRepository;

public class RecordsPagerAdapter extends FragmentStateAdapter {

    private static final String[] TYPES = {
            RentalRepository.TYPE_ROOM,
            RentalRepository.TYPE_GARAGE,
            RentalRepository.TYPE_RENTAL,
            RentalRepository.TYPE_VEHICLE
    };

    public RecordsPagerAdapter(@NonNull AppCompatActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return RecordListFragment.newInstance(TYPES[position]);
    }

    @Override
    public int getItemCount() {
        return TYPES.length;
    }
}
