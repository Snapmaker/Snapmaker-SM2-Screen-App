package fabscreen.libraries.legacy.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.ListView;

import java.util.ArrayList;

import fabscreen.libraries.legacy.view.SwipeItemLayout.Mode;

public class SwipeListView extends ListView {
    private SwipeItemLayout mCaptureItem;
    private float mLastMotionX;
    private float mLastMotionY;
    private VelocityTracker mVelocityTracker;

    private int mActivePointerId;

    private int mTouchSlop;
    private int mMaximumVelocity;

    private boolean mDragHandleBySuper;
    private boolean mDragHandleByThis;

    private boolean mIsCancelEvent;

    private ArrayList<Integer> mDisableLists;

    public SwipeListView(Context context) {
        this(context, null);
    }

    public SwipeListView(Context context, AttributeSet attrs) {
        super(context, attrs);

        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mActivePointerId = -1;
        mDragHandleBySuper = false;
        mDragHandleByThis = false;
        mIsCancelEvent = false;

        mDisableLists = new ArrayList<>();
    }

    @Override
    public boolean canScrollVertically(int direction) {
        return mDragHandleByThis
                || (mCaptureItem != null && mCaptureItem.isOpen())
                || mIsCancelEvent
                || super.canScrollVertically(direction);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();

        if (mIsCancelEvent && action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL) {
            return true;
        } else if (mIsCancelEvent) {
            cancel();
            return true;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mIsCancelEvent = false;
                mActivePointerId = ev.getPointerId(0);
                final float x = ev.getX(0);
                final float y = ev.getY(0);
                mLastMotionX = x;
                mLastMotionY = y;

                final int itemIndex = getTouchItemIndex((int) x, (int) y);

                boolean pointOther = false;
                SwipeItemLayout pointItem = null;

                View pointView = SwipeItemLayout.findTopChildUnder(this, (int) x, (int) y);
                if (pointView == null || !(pointView instanceof SwipeItemLayout)) {
                    pointOther = true;
                } else if (!mDisableLists.contains(itemIndex)) {
                    pointItem = (SwipeItemLayout) pointView;
                }

                if (!pointOther && (mCaptureItem == null || mCaptureItem != pointItem)) {
                    pointOther = true;
                }

                if (pointOther) {
                    if (mCaptureItem != null && mCaptureItem.isOpen()) {
                        mCaptureItem.close();
                        mIsCancelEvent = true;
                        return true;
                    }

                    if (pointItem != null) {
                        mCaptureItem = pointItem;
                        mCaptureItem.setTouchMode(Mode.TAP);
                    }
                } else if (!mDisableLists.contains(itemIndex)) {
                    Mode touchMode = mCaptureItem.getTouchMode();
                    boolean disallowIntercept = false;

                    if (touchMode == Mode.FLING) {
                        mCaptureItem.setTouchMode(Mode.DRAG);
                        disallowIntercept = true;
                        mDragHandleByThis = true;
                    } else {
                        mCaptureItem.setTouchMode(Mode.TAP);
                        if (mCaptureItem.isOpen()) {
                            disallowIntercept = true;
                        }
                    }

                    if (disallowIntercept) {
                        final ViewParent parent = getParent();
                        if (parent != null) {
                            parent.requestDisallowInterceptTouchEvent(true);
                        }
                    }
                }

                if (!mDragHandleByThis) {
                    mDragHandleBySuper = super.onInterceptTouchEvent(ev);
                }

                return mDragHandleByThis || mDragHandleBySuper;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int actionIndex = ev.getActionIndex();
                final int pointerId = ev.getPointerId(actionIndex);

                if (pointerId == mActivePointerId) {
                    final int newIndex = (actionIndex == 0) ? 1 : 0;
                    mActivePointerId = ev.getPointerId(newIndex);

                    mLastMotionX = ev.getX(newIndex);
                    mLastMotionY = ev.getY(newIndex);
                }

                return super.onInterceptTouchEvent(ev);
            }

            case MotionEvent.ACTION_MOVE: {
                if (mDragHandleBySuper) {
                    if (mCaptureItem != null) {
                        mCaptureItem.close();
                    }
                    return super.onInterceptTouchEvent(ev);
                }

                final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1) {
                    break;
                }

                final int x = (int) (ev.getX(activePointerIndex) + 0.5f);
                final int y = (int) (ev.getY(activePointerIndex) + 0.5f);

                int deltaX = (int) (x - mLastMotionX);
                int deltaY = (int) (y - mLastMotionY);
                final int xDiff = Math.abs(deltaX);
                final int yDiff = Math.abs(deltaY);

                if (mCaptureItem != null) {
                    Mode touchMode = mCaptureItem.getTouchMode();

                    if (touchMode == Mode.TAP) {
                        if (xDiff > mTouchSlop && xDiff > yDiff) {
                            mDragHandleByThis = true;
                            mCaptureItem.setTouchMode(Mode.DRAG);
                            final ViewParent parent = getParent();
                            parent.requestDisallowInterceptTouchEvent(true);

                            deltaX = (deltaX > 0) ? deltaX - mTouchSlop : deltaX + mTouchSlop;
                        } else {
                            mDragHandleBySuper = super.onInterceptTouchEvent(ev);
                        }
                    }

                    touchMode = mCaptureItem.getTouchMode();
                    if (touchMode == Mode.DRAG) {
                        mLastMotionX = x;
                        mLastMotionY = y;

                        // drag item out
                        mCaptureItem.trackMotionScroll(deltaX);
                    }
                } else {
                    mDragHandleBySuper = super.onInterceptTouchEvent(ev);
                }

                if (mDragHandleBySuper && mCaptureItem != null) {
                    mCaptureItem.close();
                }
                return mDragHandleByThis || mDragHandleBySuper;
            }
            case MotionEvent.ACTION_UP: {
                boolean ret = false;
                final int x = (int) ev.getX();
                final int y = (int) ev.getY();
                final int itemIndex = getTouchItemIndex(x, y);

                if (mDragHandleByThis && mCaptureItem != null) {
                    Mode touchMode = mCaptureItem.getTouchMode();
                    if (touchMode == Mode.DRAG && !mDisableLists.contains(itemIndex)) {
                        final VelocityTracker velocityTracker = mVelocityTracker;
                        velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                        int xVel = (int) velocityTracker.getXVelocity(mActivePointerId);
                        mCaptureItem.fling(xVel);
                        ret = true;
                    }
                } else {
                    ret = super.onInterceptTouchEvent(ev);
                }

                cancel();
                return ret;
            }
            case MotionEvent.ACTION_CANCEL: {
                if (mCaptureItem != null) {
                    mCaptureItem.revise();
                }
                super.onInterceptTouchEvent(ev);
                cancel();
                break;
            }
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        final int actionIndex = ev.getActionIndex();

        if (mIsCancelEvent && action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL) {
            return true;
        } else if (mIsCancelEvent) {
            cancel();
            return true;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                return super.onTouchEvent(ev);
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                mActivePointerId = ev.getPointerId(actionIndex);

                mLastMotionX = ev.getX(actionIndex);
                mLastMotionY = ev.getY(actionIndex);
                return super.onTouchEvent(ev);
            }
            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerId = ev.getPointerId(actionIndex);
                if (pointerId == mActivePointerId) {
                    final int newIndex = (actionIndex == 0) ? 1 : 0;
                    mActivePointerId = ev.getPointerId(newIndex);

                    mLastMotionX = ev.getX(newIndex);
                    mLastMotionY = ev.getY(newIndex);
                }
                return super.onTouchEvent(ev);
            }
            case MotionEvent.ACTION_MOVE: {
                if (mDragHandleBySuper) {
                    if (mCaptureItem != null) {
                        mCaptureItem.close();
                    }
                    return super.onTouchEvent(ev);
                }

                final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1) {
                    break;
                }

                final int x = (int) (ev.getX(activePointerIndex) + 0.5f);
                final int y = (int) ((int) ev.getY(activePointerIndex) + 0.5f);

                int deltaX = (int) (x - mLastMotionX);
                int deltaY = (int) (y - mLastMotionY);
                final int xDiff = Math.abs(deltaX);
                final int yDiff = Math.abs(deltaY);

                if (mCaptureItem != null) {
                    Mode touchMode = mCaptureItem.getTouchMode();

                    if (touchMode == Mode.TAP) {
                        if (xDiff > mTouchSlop && xDiff > yDiff) {
                            mDragHandleByThis = true;
                            mCaptureItem.setTouchMode(Mode.DRAG);
                            final ViewParent parent = getParent();
                            parent.requestDisallowInterceptTouchEvent(true);

                            deltaX = deltaX > 0 ? deltaX - mTouchSlop : deltaX + mTouchSlop;
                        } else if (yDiff > mTouchSlop) {
                            mDragHandleBySuper = true;
                            super.onTouchEvent(ev);
                        }
                    }

                    touchMode = mCaptureItem.getTouchMode();
                    if (touchMode == Mode.DRAG) {
                        mLastMotionX = x;
                        mLastMotionY = y;

                        mCaptureItem.trackMotionScroll(deltaX);
                    }
                } else {
                    mDragHandleBySuper = super.onTouchEvent(ev);
                }

                if (mDragHandleBySuper && mCaptureItem != null) {
                    mCaptureItem.close();
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                final int x = (int) ev.getX();
                final int y = (int) ev.getY();
                final int itemIndex = getTouchItemIndex(x, y);

                if (mDragHandleByThis && mCaptureItem != null) {
                    Mode touchMode = mCaptureItem.getTouchMode();
                    if (touchMode == Mode.DRAG && !mDisableLists.contains(itemIndex)) {
                        final VelocityTracker velocityTracker = mVelocityTracker;
                        velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                        int xVel = (int) velocityTracker.getXVelocity(mActivePointerId);
                        mCaptureItem.fling(xVel);
                    }
                } else {
                    super.onTouchEvent(ev);
                }

                cancel();
                return true;
            }
            case MotionEvent.ACTION_CANCEL: {
                if (mCaptureItem != null) {
                    mCaptureItem.revise();
                }
                super.onTouchEvent(ev);
                cancel();
                return true;
            }
        }

        return true;
    }

    void cancel() {
        mDragHandleBySuper = false;
        mDragHandleByThis = false;
        mIsCancelEvent = false;
        mActivePointerId = -1;

        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    public void closeAllItems() {
        if (mCaptureItem != null && mCaptureItem.isOpen())
            mCaptureItem.close();
    }

    private int getTouchItemIndex(int x, int y) {
        Rect rect = new Rect();
        View child;
        for (int i = 0; i < this.getChildCount(); i++) {
            child = this.getChildAt(i);
            child.getHitRect(rect);
            if (rect.contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    public void disableSwipeFromIndex(int index) {
        if (!mDisableLists.contains(index)) {
            mDisableLists.add(index);
        }
    }

    public void enableSwipeFromIndex(int index) {
        if (mDisableLists.contains(index)) {
            mDisableLists.remove(index);
        }
    }
}

