package zzg.klinechart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;

import zzg.klinechart.internal.EntryData;
import zzg.klinechart.internal.Renderer;

/**
 * A K line chart widget.
 * <p>
 * Created by 曾志刚 on 16-3-30.
 */
public class KLineChart extends View {

  private static final int SCROLL_STATE_IDLE = 0;
  private static final int SCROLL_STATE_DRAGGING = 1;
  private static final int SCROLL_STATE_SETTLING = 2;

  private int mScrollState = SCROLL_STATE_IDLE;
  private VelocityTracker mVelocityTracker;
  private int mTouchSlop;
  private float mLastTouchX;
  private float mLastTouchY;
  private int mScrollPointerId = -1;
  private final int mMinFlingVelocity;
  private final int mMaxFlingVelocity;
  private ViewFlinger mViewFlinger = new ViewFlinger();

  private RectF contentRect;
  private float contentMinOffset;

  private Renderer renderer;
  private EntryData mData;

  public KLineChart(Context context) {
    this(context, null);
  }

  public KLineChart(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public KLineChart(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    final ViewConfiguration vc = ViewConfiguration.get(this.getContext());
    mTouchSlop = vc.getScaledTouchSlop();
    mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
    mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();

    contentRect = new RectF();
    contentMinOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, context.getResources().getDisplayMetrics());

    renderer = new Renderer();

    detector.setIsLongpressEnabled(true);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    contentRect.set(contentMinOffset * 2, contentMinOffset, w - contentMinOffset, h - contentMinOffset);
    notifyDataSetChanged(false);
  }

  /**
   * Sets a new data object for the chart. The data object contains all values
   * and information needed for displaying.
   */
  public void setData(EntryData data) {
    mData = data;
  }

  public void notifyDataSetChanged(boolean invalidate) {
    mData.calcMinMax(0, mData.entries.size());
    renderer.setContentRect(contentRect);
    renderer.setData(mData);

    if (invalidate) {
      invalidate();
    }
  }

  boolean onLongPress = false;
  GestureDetector detector = new GestureDetector(this.getContext(), new GestureDetector.SimpleOnGestureListener() {
    @Override
    public void onLongPress(MotionEvent e) {
      onLongPress = true;
      highlight(e);
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
      renderer.zoomOut(e.getX(),e.getY());
      return true;
    }
  });

  private void highlight(MotionEvent e) {
    renderer.enableHighlight(e);
    invalidate();
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent e) {
    detector.onTouchEvent(e);

    final int action = MotionEventCompat.getActionMasked(e);
    switch (action) {
      case MotionEvent.ACTION_MOVE: {
        if (onLongPress) {
          highlight(e);
        }
        break;
      }
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL: {
        onLongPress = false;
        renderer.disableHighlight();
        invalidate();
        break;
      }
    }
    return onLongPress || super.dispatchTouchEvent(e);
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    if (mVelocityTracker == null) {
      mVelocityTracker = VelocityTracker.obtain();
    }
    boolean eventAddedToVelocityTracker = false;

    final MotionEvent vtev = MotionEvent.obtain(e);
    final int action = MotionEventCompat.getActionMasked(e);
    final int actionIndex = MotionEventCompat.getActionIndex(e);

    switch (action) {
      case MotionEvent.ACTION_DOWN: {
        mScrollPointerId = MotionEventCompat.getPointerId(e, 0);
        mLastTouchX = (int) (e.getX() + 0.5f);
        mLastTouchY = (int) (e.getY() + 0.5f);

        if (mScrollState == SCROLL_STATE_SETTLING) {
          setScrollState(SCROLL_STATE_DRAGGING);
        }
        break;
      }
      case MotionEventCompat.ACTION_POINTER_DOWN: {
        mScrollPointerId = MotionEventCompat.getPointerId(e, actionIndex);
        mLastTouchX = (int) (MotionEventCompat.getX(e, actionIndex) + 0.5f);
        mLastTouchY = (int) (MotionEventCompat.getY(e, actionIndex) + 0.5f);
        break;
      }
      case MotionEvent.ACTION_MOVE: {
        final int index = MotionEventCompat.findPointerIndex(e, mScrollPointerId);
        if (index < 0) {
          return false;
        }

        final int x = (int) (MotionEventCompat.getX(e, index) + 0.5f);
        final int y = (int) (MotionEventCompat.getY(e, index) + 0.5f);

        float dx = mLastTouchX - x;
        float dy = mLastTouchY - y;

        if (mScrollState != SCROLL_STATE_DRAGGING) {
          boolean startScroll = false;
          if (Math.abs(dx) > mTouchSlop) {
            if (dx > 0) {
              dx -= mTouchSlop;
            } else {
              dx += mTouchSlop;
            }
            startScroll = true;
          }
          if (Math.abs(dy) > mTouchSlop) {
            if (dy > 0) {
              dy -= mTouchSlop;
            } else {
              dy += mTouchSlop;
            }
            startScroll = true;
          }
          if (startScroll) {
            setScrollState(SCROLL_STATE_DRAGGING);
          }
        }

        if (mScrollState == SCROLL_STATE_DRAGGING) {
          mLastTouchX = x;
          mLastTouchY = y;

          scroll(dx, 0);
        }
        break;
      }
      case MotionEventCompat.ACTION_POINTER_UP: {
        final int lastActionIndex = MotionEventCompat.getActionIndex(e);
        if (MotionEventCompat.getPointerId(e, lastActionIndex) == mScrollPointerId) {
          // Pick a new pointer to pick up the slack.
          final int newIndex = lastActionIndex == 0 ? 1 : 0;
          mScrollPointerId = MotionEventCompat.getPointerId(e, newIndex);
          mLastTouchX = (int) (MotionEventCompat.getX(e, newIndex) + 0.5f);
          mLastTouchY = (int) (MotionEventCompat.getY(e, newIndex) + 0.5f);
        }
        break;
      }
      case MotionEvent.ACTION_UP: {
        mVelocityTracker.addMovement(vtev);
        eventAddedToVelocityTracker = true;
        mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
        final float xvel =
            -VelocityTrackerCompat.getXVelocity(mVelocityTracker, mScrollPointerId);
        final float yvel =
            -VelocityTrackerCompat.getYVelocity(mVelocityTracker, mScrollPointerId);
        if (!((xvel != 0 || yvel != 0) && fling((int) xvel, (int) yvel))) {
          setScrollState(SCROLL_STATE_IDLE);
        }
        resetTouch();
        break;
      }
      case MotionEvent.ACTION_CANCEL: {
        resetTouch();
        setScrollState(SCROLL_STATE_IDLE);
        break;
      }
    }

    if (!eventAddedToVelocityTracker) {
      mVelocityTracker.addMovement(vtev);
    }
    vtev.recycle();

    return true;
  }

  private void setScrollState(int state) {
    if (state == mScrollState) {
      return;
    }
    mScrollState = state;
    if (state != SCROLL_STATE_SETTLING) {
      stopScrollersInternal();
    }
  }

  private void stopScrollersInternal() {
    mViewFlinger.stop();
  }

  private void resetTouch() {
    if (mVelocityTracker != null) {
      mVelocityTracker.clear();
    }
  }

  private boolean fling(int velocityX, int velocityY) {
    if (Math.abs(velocityX) < mMinFlingVelocity) {
      velocityX = 0;
    }
    if (Math.abs(velocityY) < mMinFlingVelocity) {
      velocityY = 0;
    }
    if (velocityX == 0 && velocityY == 0) {
      // If we don't have any velocity, return false
      return false;
    }

    velocityX = Math.max(-mMaxFlingVelocity, Math.min(velocityX, mMaxFlingVelocity));
    velocityY = Math.max(-mMaxFlingVelocity, Math.min(velocityY, mMaxFlingVelocity));
    mViewFlinger.fling(velocityX, velocityY);
    return true;
  }

  private void scroll(float dx, float dy) {
    renderer.refreshTouchMatrix(dx, dy);
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    renderer.render(canvas);
  }

  private class ViewFlinger implements Runnable {
    private int mLastFlingX;
    private int mLastFlingY;
    private ScrollerCompat mScroller;
    private final Interpolator sQuinticInterpolator = new Interpolator() {
      public float getInterpolation(float t) {
        t -= 1.0f;
        return t * t * t * t * t + 1.0f;
      }
    };

    public ViewFlinger() {
      mScroller = ScrollerCompat.create(getContext(), sQuinticInterpolator);
    }

    @Override
    public void run() {
      final ScrollerCompat scroller = mScroller;
      if (scroller.computeScrollOffset()) {
        final int x = scroller.getCurrX();
        final int y = scroller.getCurrY();
        final int dx = x - mLastFlingX;
        final int dy = y - mLastFlingY;
        mLastFlingX = x;
        mLastFlingY = y;
        int overscrollX = 0, overscrollY = 0;

        scroll(dx, 0);
        if (renderer.canScroll() && !scroller.isFinished()) {
          postOnAnimation();
        }
      }
    }

    void postOnAnimation() {
      removeCallbacks(this);
      ViewCompat.postOnAnimation(KLineChart.this, this);
    }

    public void fling(int velocityX, int velocityY) {
      setScrollState(SCROLL_STATE_SETTLING);
      mLastFlingX = mLastFlingY = 0;
      mScroller.fling(0, 0, velocityX, velocityY,
          Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
      postOnAnimation();
    }

    public void stop() {
      removeCallbacks(this);
      mScroller.abortAnimation();
    }
  }
}
