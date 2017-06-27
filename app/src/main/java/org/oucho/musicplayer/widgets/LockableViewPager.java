package org.oucho.musicplayer.widgets;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;


public class LockableViewPager extends ViewPager {

    private static boolean swipeLocked;

    public LockableViewPager(Context context) {
        super(context);
    }

    public LockableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean getSwipeLocked() {
        return swipeLocked;
    }

    public static void setSwipeLocked(boolean value) {
        //this.swipeLocked = swipeLocked;
        swipeLocked = value;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return !swipeLocked && super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return !swipeLocked && super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        return !swipeLocked && super.canScrollHorizontally(direction);
    }

}