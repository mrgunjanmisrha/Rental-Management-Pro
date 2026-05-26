package com.gtechapps.ramjankimandir.ui.common;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.gtechapps.ramjankimandir.R;

public class RecordsActivity extends AppCompatActivity {

    private static final String[] TAB_TITLES = {"Room", "Garage", "Rental", "Vehicle"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);

        MaterialToolbar toolbar = findViewById(R.id.recordsToolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        ViewPager2 viewPager2 = findViewById(R.id.recordsViewPager);
        TabLayout tabLayout = findViewById(R.id.recordsTabLayout);
        viewPager2.setAdapter(new RecordsPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager2, (tab, position) -> tab.setText(TAB_TITLES[position])).attach();
    }
}
