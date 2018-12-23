package com.shoyu666.imagerecord.core;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class DefaultBitmapFeeder extends BitmapFeeder {
    Bitmap defaultBit;
    public Canvas canvas;
    public Paint paint;

    @Override
    public Bitmap getOneFrame(Rect mVideoRect, long timeStampNanos) {
        if (defaultBit == null || defaultBit.isRecycled()) {
            defaultBit = Bitmap.createBitmap(mVideoRect.width(), mVideoRect.height(), Bitmap.Config.RGB_565);
            canvas = new Canvas(defaultBit);
            paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setTextSize(30);
        }
        defaultBit.eraseColor(Color.RED);
        canvas.drawText("" + (timeStampNanos), 30, 500, paint);
        return defaultBit;
    }
}
