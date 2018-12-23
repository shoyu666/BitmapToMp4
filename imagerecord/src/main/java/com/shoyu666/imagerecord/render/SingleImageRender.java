package com.shoyu666.imagerecord.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.Image;
import android.net.Uri;
import android.view.MotionEvent;

import com.shoyu666.imagerecord.render.banner.BitmapWrap;
import com.shoyu666.imagerecord.util.ImageUtil;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class SingleImageRender implements ISpiritRender {
    public File file;
    public volatile BitmapWrap mBitmapWrap;
    public Paint paint;
    public Context application;
    public volatile boolean bitmapReady;
    public volatile boolean hasDraw;

    public SingleImageRender(Context application, File file) {
        this.file = file;
        paint = new Paint(Paint.DITHER_FLAG);
        paint.setAntiAlias(true);
        this.application = application.getApplicationContext();
    }

    @Override
    public boolean draw(Canvas bitmapCanvas, int width, int height) {
        boolean update = false;
        if (!hasDraw && mBitmapWrap != null) {
            mBitmapWrap.draw(bitmapCanvas, width, height, paint);
            hasDraw = true;
            update = true;
        }
        return update;
    }

    @Override
    public void onDetachedFromWindow() {
        release();
    }

    @Override
    public void surfaceDestroyed(Bitmap bitmap) {
        hasDraw = false;
    }

    @Override
    public void onTouchEvent(@Nullable MotionEvent event) {

    }

    @Override
    public void surfaceCreated() {

    }

    @Override
    public boolean shoudRefresh(long frameTimeNanos, int width, int height) {
        return bitmapReady;
    }

    public void release() {
        hasDraw = false;
        synchronized (this) {
            if (mBitmapWrap != null) {
                mBitmapWrap.recycle();
            }
        }
    }

    @Override
    public void surfaceChanged(final int width, final int height) {
        release();
        Observable.just(file).subscribeOn(Schedulers.newThread()).map(new Function<File, Void>() {
            @Override
            public Void apply(File file) {
                try {
                    RectF rectF = new RectF();
                    BitmapWrap bitmapWrap = new BitmapWrap();
                    int rotate = ImageUtil.getExifRotation(file);
                    Bitmap bitmap = ImageUtil.getSampleBitmap(application, Uri.fromFile(file), width, height,rotate);
                    Bitmap roteBitmap = ImageUtil.rotaingImageView(rotate, bitmap);
                    if (roteBitmap != null) {
                        bitmapWrap.setBitmap(width, height, roteBitmap, rectF, paint, rotate);
                    } else {
                        bitmapWrap.setBitmap(width, height, bitmap, rectF, paint, rotate);
                    }
                    bitmap.recycle();
                    if (roteBitmap != null) {
                        roteBitmap.recycle();
                    }
                    mBitmapWrap = bitmapWrap;
                    bitmapReady = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }).subscribe(new Observer<Void>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Void value) {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }
}
