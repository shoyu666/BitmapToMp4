package com.shoyu666.imagerecord.render.banner;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.shoyu666.imagerecord.render.GifRender;

public class BitmapWrap {
    public Bitmap bitmap;
    public Rect src = new Rect();

    public void recycle() {
        if (bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    public boolean draw(Canvas bitmapCanvas, int width, int height, Paint paint) {
        src.top = 0;
        src.bottom = height;
        src.left = 0;
        src.right = width;
        bitmapCanvas.drawBitmap(bitmap, null, src, paint);
        return true;
    }

    public void setBitmap(int width, int height, Bitmap rawBitmap, RectF rectf, Paint paint, int rotate) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setHasAlpha(false);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLACK);
        GifRender.drawBitmapToCenter(canvas, paint, rectf, width, height, rawBitmap);
        this.bitmap = bitmap;
    }
}
