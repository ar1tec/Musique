package org.oucho.musicplayer.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.utils.ToolbarDrawerToggle;

import java.util.HashMap;
import java.util.Map;


public class LibraryFragment extends BaseFragment {


    private static SectionsPagerAdapter mSectionsPagerAdapter;
    private static boolean lock = false;

    public static LibraryFragment newInstance() {

        return new LibraryFragment();
    }


    @SuppressLint("StaticFieldLeak")
    private static ViewPager mViewPager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mSectionsPagerAdapter = new SectionsPagerAdapter( getChildFragmentManager());

        mViewPager = (ViewPager) rootView.findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        MainActivity activity = (MainActivity) getActivity();

        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);

        DrawerLayout drawerLayout = activity.getDrawerLayout();

        activity.setSupportActionBar(toolbar);

        ToolbarDrawerToggle drawerToggle = new ToolbarDrawerToggle(activity,drawerLayout,toolbar, new int[]{Gravity.START});
        drawerLayout.addDrawerListener(drawerToggle);

        return rootView;

    }


    public static void backToPrevious() {
        mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
    }


    private boolean getLock() {
        return lock;
    }

    public static void  setLock(boolean value) {
        lock = value;
        mSectionsPagerAdapter.notifyDataSetChanged();

    }


    @Override
    public void load() {
        int fragmentCount = mSectionsPagerAdapter.getCount();
        for(int pos = 0; pos < fragmentCount; pos++)
        {
            BaseFragment fragment = (BaseFragment) mSectionsPagerAdapter.getFragment(pos);
            if(fragment != null)
            {
                Log.d("frag1", fragment.getClass().getCanonicalName());

                fragment.load();
            }
        }
    }


    private class SectionsPagerAdapter extends FragmentPagerAdapter {

        private final Map<Integer, String> mFragmentTags;

        @SuppressLint("UseSparseArrays")
        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            mFragmentTags = new HashMap<>();

        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return AlbumListFragment.newInstance();
                //case 1:
                //    return ArtistListFragment.newInstance();
                case 1:
                    return SongListFragment.newInstance();
                case 2:
                    return PlaylistListFragment.newInstance();
                default: //do nothing
                    break;
            }
            return null;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object obj = super.instantiateItem(container, position);
            if (obj instanceof Fragment) {
                Fragment f = (Fragment) obj;
                String tag = f.getTag();
                mFragmentTags.put(position, tag);
                Log.d("fragtag", tag);

            }
            return obj;
        }

        Fragment getFragment(int position) {
            String tag = mFragmentTags.get(position);
            if (tag == null)
                return null;
            return getChildFragmentManager().findFragmentByTag(tag);
        }

        @Override
        public int getCount() {

            if (!getLock()) {
                return 3;
        } else {
                return 1;
            }

        }

    }
}
