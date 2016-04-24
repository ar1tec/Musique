package org.oucho.musicplayer.widgets;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.oucho.musicplayer.R;

public class FastScroller extends View {

    private float mHandleY;

    private boolean mScrolling = false;

    private boolean mShowScroller = true;

    private RecyclerView mRecyclerView;

    private float mScrollerAlpha = 0.0F;

    private boolean mScrollerVisible = false;


    private Paint mPaint;
    private int mScrollerColor;

    private int mScrollerBackground;
    private float mHandleWidth;
    private float mHandleHeight;

    private ValueAnimator mScrollerAnimator = null;

    private final ValueAnimator.AnimatorUpdateListener mHandleAnimatorListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mScrollerAlpha = (float) animation.getAnimatedValue();
            invalidate();
        }
    };


    private final Runnable mHideScrollerRunnable = new Runnable() {

        @Override
        public void run() {
            hideScroller();

        }
    };

    private final OnScrollListener mOnScrollListener = new OnScrollListener() {

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            int visibleItems = recyclerView.getChildCount();
            int itemCount = recyclerView.getAdapter().getItemCount();

            if (((float) itemCount) / visibleItems < 2.0F) {
                mShowScroller = false;
                return;
            }
            if (newState == RecyclerView.SCROLL_STATE_IDLE && !mScrolling) {
                postDelayed(mHideScrollerRunnable, 1500);
            } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                removeCallbacks(mHideScrollerRunnable);
                if (!mScrollerVisible) {
                    showScroller();
                }
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

            if (mScrolling || !mShowScroller) {
                return;
            }


            int extent = recyclerView.computeVerticalScrollExtent();


            int offset = recyclerView.computeVerticalScrollOffset();
            int range = recyclerView.computeVerticalScrollRange() - extent;


            float proportion = ((float) offset) / range;

            moveHandleTo(proportion);
        }

    };

    public FastScroller(Context context) {
        super(context);
        init(context, null);
    }

    public FastScroller(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public FastScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.FastScroller, R.attr.fastScrollerStyle, R.style.DefFastScrollerStyle);
            try {
                mHandleWidth = a.getDimension(R.styleable.FastScroller_handleWidth, 0);
                mHandleHeight = a.getDimension(R.styleable.FastScroller_handleHeight, 0);
                mScrollerColor = a.getColor(R.styleable.FastScroller_scrollerColor, 0);

            } finally {
                a.recycle();
            }
        }
        mScrollerBackground = ContextCompat.getColor(context, R.color.transparent);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
    }

    public void setRecyclerView(RecyclerView view) {
        mRecyclerView = view;
        mRecyclerView.addOnScrollListener(mOnScrollListener);
    }

    public void setSectionIndexer(SectionIndexer si) {
    }

    private void moveHandleTo(float proportion) {
        int height = getHeight();

        mHandleY = proportion * (height - mHandleHeight);

        invalidate();
    }


    private void scrollTo(float pos) {
        float proportion = Math.max(0, pos / getHeight());
        int itemCount = mRecyclerView.getAdapter().getItemCount();
        int itemPos = Math.min((int) (proportion * itemCount), itemCount - 1);


        mRecyclerView.scrollToPosition(itemPos);

        float scrollerPos = pos - (mHandleHeight / 2);
        int height = getHeight();
        scrollerPos = Math.max(0,
                Math.min(height - mHandleHeight, scrollerPos));
        mHandleY = scrollerPos;

        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float x = ev.getX();
        float y = ev.getY();

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                float width = getWidth();
                float touchHandleWidth = mHandleWidth*3; // the touchable area is three times wider than the handle

                if (x > width - touchHandleWidth
                        && y > mHandleY && y < mHandleY + mHandleHeight) {
                    mScrolling = true;
                    removeCallbacks(mHideScrollerRunnable);

                    if (!mScrollerVisible) {
                        showScroller();
                    }

                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                scrollTo(y);
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                scrollTo(y);
                mScrolling = false;
                mOnScrollListener.onScrollStateChanged(mRecyclerView,
                        RecyclerView.SCROLL_STATE_IDLE);

                break;

            default:
                break;
        }
        return mScrolling;
    }


    private void showScroller() {
        mScrollerAlpha = 1.0F;
        mScrollerVisible = true;
        invalidate();
    }

    private void hideScroller() {

        if (mScrollerAnimator == null) {
            mScrollerAnimator = ValueAnimator.ofFloat(1.0F, 0.0F);
            mScrollerAnimator.addUpdateListener(mHandleAnimatorListener);
            mScrollerAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mScrollerVisible = false;
                    invalidate();
                }
            });
        } else {
            mScrollerAnimator.cancel();
        }

        mScrollerAnimator.start();

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mScrollerVisible) {
            int width = getWidth();
            int height = getHeight();


            float scrollerX = width - mHandleWidth;
            mPaint.setColor(applyAlpha(mScrollerBackground, mScrollerAlpha));
            canvas.drawRect(scrollerX, 0, width, height, mPaint);

            mPaint.setColor(applyAlpha(mScrollerColor, mScrollerAlpha));
            canvas.drawRect(scrollerX, mHandleY, width, mHandleY + mHandleHeight, mPaint);
        }
    }

    public interface SectionIndexer {
        String getSectionForPosition(int position);
    }

    private static int applyAlpha(int color, float alpha) {
        int colorAlpha  = ((color & 0xFF000000) >> 24) + 256;
        return (color & 0x00FFFFFF) | ((int) (colorAlpha * alpha) << 24);
    }

}