package com.shoyu666.imagerecord.render;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.bumptech.glide.gifdecoder.GifDecoder;

import org.jetbrains.annotations.Nullable;

import java.io.File;

public class GifRender implements ISpiritRender {

    public volatile GifDecoder decoder;


    public Paint bitmapPaint;

    public RectF rect = new RectF();

    public Bitmap frame;

    public long preShowTime;

    public int nextDelay = 0;


    public GifRender(GifDecoder decoder) {
        bitmapPaint = new Paint();
        this.decoder = decoder;
    }

    @Override
    public boolean draw(Canvas canvas, int width, int height) {
        if (decoder != null) {
            long current = System.currentTimeMillis();
            if (current > (preShowTime + nextDelay)) {
                frame = decoder.getNextFrame();
                nextDelay = decoder.getNextDelay();
                decoder.advance();
                preShowTime = System.currentTimeMillis();
            }
            if (frame == null) {
                return false;
            }
            drawBitmapToCenter(canvas, bitmapPaint, rect, width, height, frame);
        }
        return true;
    }

    @Override
    public void onDetachedFromWindow() {
        if(decoder!=null){
            decoder.clear();
        }
    }


    public static void drawBitmapToCenter(Canvas canvas, Paint paint, RectF rectf, int viewPortW, int viewPortH, Bitmap forDraw) {
        float rote = viewPortW / viewPortH;
        float bitmapW = forDraw.getWidth();
        float bitmapH = forDraw.getHeight();
        float bitmapRote = bitmapW / bitmapH;
        if (bitmapRote > rote) {
            rectf.left = 0;
            rectf.right = viewPortW;
            float fixH = viewPortW / bitmapRote;
            rectf.top = (viewPortH - fixH) / 2;
            rectf.bottom = rectf.top + fixH;
        }
        if (bitmapRote < rote) {
            float fixW = viewPortH * bitmapRote;
            rectf.left = (viewPortW - fixW) / 2;
            rectf.right = rectf.left + fixW;
            rectf.top = 0;
            rectf.bottom = viewPortH;
        }
        if (bitmapRote == rote) {
            rectf.left = 0;
            rectf.right = viewPortW;
            rectf.top = 0;
            rectf.bottom = viewPortH;
        }
        canvas.drawBitmap(forDraw, null, rectf, paint);
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
    public boolean shoudRefresh(long frameTimeNanos, int w, int h) {
        return decoder != null;
    }

    @Override
    public void surfaceChanged(int width, int height) {

    }


}
