package com.shoyu666.record;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import com.shoyu666.imagerecord.render.ISpiritRender;
import org.jetbrains.annotations.Nullable;

public class TestRender implements ISpiritRender {
    public Paint mPaint;

    public TestRender() {
        mPaint = new Paint();
        mPaint.setColor(Color.BLACK);
        mPaint.setTextSize(40);
    }

    @Override
    public boolean draw(Canvas bitmapCanvas, int width, int height) {
        bitmapCanvas.drawColor(Color.RED);
        bitmapCanvas.drawText(System.currentTimeMillis() + "", 20, 40, mPaint);
        return true;
    }

    @Override
    public void onDetachedFromWindow() {

    }

    @Override
    public void surfaceDestroyed(Bitmap bitmap) {

    }

    @Override
    public void onTouchEvent(@Nullable MotionEvent event) {

    }

    @Override
    public void surfaceCreated() {

    }

    @Override
    public boolean shoudRefresh(long frameTimeNanos, int width, int height) {
        return true;
    }

    @Override
    public void surfaceChanged(int width, int height) {

    }
}
