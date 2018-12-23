package com.shoyu666.imagerecord.render;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.MotionEvent;

import org.jetbrains.annotations.Nullable;

public interface ISpiritRender {
    boolean draw(Canvas bitmapCanvas, int width, int height);

    void onDetachedFromWindow();

    void surfaceDestroyed(Bitmap bitmap);

    void onTouchEvent(@Nullable MotionEvent event);

    void surfaceCreated();

    boolean shoudRefresh(long frameTimeNanos, int width, int height);

    void surfaceChanged(int width, int height);
}
