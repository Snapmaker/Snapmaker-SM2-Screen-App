package com.snapmaker.fabscreen.modules.factory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.subjects.BehaviorSubject;

public class DragSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private int screenW;
    private int screenH;

    // default x y
    private float cx = 360;
    private float cy = 640;

    private Bitmap mBitmap;
    private SurfaceHolder mSurfaceHolder;

    private Thread thread;
    private BehaviorSubject<Boolean> mIsRunningSubject = BehaviorSubject.createDefault(true);

    public DragSurfaceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mBitmap = getBitmapResources(context, R.drawable.btn_all_axes_normal_64x64);
        thread = new Thread(this);
    }

    public static Bitmap getBitmapResources(Context context, int resId) {
        return BitmapFactory.decodeResource(context.getResources(), resId);
    }

    protected void myDraw() {
        Canvas canvas = mSurfaceHolder.lockCanvas();
        if (canvas == null) return;

        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(mBitmap, cx - mBitmap.getWidth() / 2.0f, cy - mBitmap.getHeight() / 2.0f, null);
        mSurfaceHolder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void run() {
        while (mIsRunningSubject.getValue()) {
            try {
                myDraw();
                Thread.sleep(20);
            } catch (InterruptedException e) {
                LogHelper.log(e);
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                cx = (int) event.getX();
                cy = (int) event.getY();
                break;
        }
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        screenW = getWidth();
        screenH = getHeight();
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mIsRunningSubject.onNext(false);
        doRecycledIfNot(mBitmap);
    }

    public static void doRecycledIfNot(Bitmap bitmap) {
        if (!bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
}
