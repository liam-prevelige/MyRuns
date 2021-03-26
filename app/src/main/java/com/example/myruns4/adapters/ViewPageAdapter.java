package com.example.myruns4.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.myruns4.fragments.HistoryFragment;
import com.example.myruns4.fragments.StartFragment;

/**
 * Start and history fragment adapter for MainActivity ViewPager2
 */
public class ViewPageAdapter extends FragmentStateAdapter {
    private static final int START_FRAG_POSITION = 0;
    private static final int NUMBER_FRAGMENTS = 2;

    public ViewPageAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if(position == START_FRAG_POSITION) return StartFragment.newInstance(String.valueOf(position));
        else return HistoryFragment.newInstance(String.valueOf(position));
    }

    @Override
    public int getItemCount() {
        return NUMBER_FRAGMENTS;
    }
}
