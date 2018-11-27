package com.shoyu666.record;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.shoyu666.imagerecord.render.ISpiritRender;

public class TestRender implements ISpiritRender {
    public Paint mPaint;

    public TestRender() {
        mPaint = new Paint();
        mPaint.setColor(Color.BLACK);
        mPaint.setTextSize(40);
    }

    @Override
    public boolean draw(Canvas bitmapCanvas, float w, float h) {
        bitmapCanvas.drawColor(Color.RED);
        bitmapCanvas.drawText(System.currentTimeMillis() + "", 20, 40, mPaint);
        return true;
    }
}
