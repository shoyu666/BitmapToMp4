package com.shoyu666.imagerecord.render.banner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.view.MotionEvent;

import com.shoyu666.imagerecord.log.MLog;
import com.shoyu666.imagerecord.render.ISpiritRender;
import com.shoyu666.imagerecord.util.ImageUtil;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class BannerRender2 implements ISpiritRender {
    public static final String TAG = "BannerRender";
    public ArrayList<File> autoSwichImages;
    public List<BitmapWrap> bitmapWraps = Collections.synchronizedList(new ArrayList<BitmapWrap>());
    public Context application;
    public volatile boolean bitmapReady;
    long pageWaitMs;
    public Paint paint;
    public volatile int currentIndex = 0;
    public volatile long lastMoveEndMS = 0;
    public volatile boolean start = false;

    public BannerRender2(Context application, ArrayList<File> autoSwichImages) {
        this.application = application.getApplicationContext();
        this.autoSwichImages = autoSwichImages;
        this.bitmapReady = false;
        this.pageWaitMs = 4000;
        paint = new Paint(Paint.DITHER_FLAG);
        paint.setAntiAlias(true);
    }

    @Override
    public boolean draw(Canvas bitmapCanvas, int width, int height) {
        MLog.d(TAG, "draw");
        long currentMS = System.currentTimeMillis();
        if (lastMoveEndMS == 0) {
            lastMoveEndMS = currentMS;
        }
        if (start) {
            long elapsedMs = currentMS - lastMoveEndMS;
            if (elapsedMs >= pageWaitMs) {
                currentIndex++;
                lastMoveEndMS = currentMS;
            }
        } else {
            lastMoveEndMS = currentMS;
        }
        drawFirst(bitmapCanvas, width, height);
        return true;
    }


    @Override
    public void onDetachedFromWindow() {
        release();
    }

    private void drawFirst(Canvas bitmapCanvas, int width, int height) {
        BitmapWrap bitmapWrap = getBitmapWrap(currentIndex, true);
        if (bitmapWrap == null) {
            MLog.d(TAG, "drawFirst return bitmapWrap == null ");
            return;
        }
        MLog.d(TAG, "drawFirst");
        bitmapWrap.draw(bitmapCanvas, width, height, paint);
    }


    @Override
    public void surfaceDestroyed(Bitmap bitmap) {
        MLog.d(TAG, "surfaceDestroyed");
    }

    public void release() {
        bitmapReady = false;
        if (bitmapWraps.size() > 0) {
            for (BitmapWrap bitmap : bitmapWraps) {
                bitmap.recycle();
            }
        }
        bitmapWraps.clear();
    }

    @Override
    public void onTouchEvent(@Nullable MotionEvent event) {

    }

    @Override
    public void surfaceCreated() {
        MLog.d(TAG, "surfaceCreated");
    }

    @Override
    public boolean shoudRefresh(long frameTimeNanos, int width, int height) {
        MLog.d(TAG, "bitmapReady" + bitmapReady);
        return bitmapReady;
    }


    public BitmapWrap getBitmapWrap(int index, boolean current) {
        int count = bitmapWraps.size();
        if (count == 0) {
            return null;
        }
        if (index >= count) {
            index = 0;
        }
        if (current) {
            currentIndex = index;
        }
        return bitmapWraps.get(index);
    }

    @Override
    public void surfaceChanged(final int width, final int height) {
        if (bitmapReady) {
            return;
        }
        Observable.just(autoSwichImages).subscribeOn(Schedulers.newThread()).map(new Function<ArrayList<File>, Void>() {
            @Override
            public Void apply(ArrayList<File> uris) {
                int count = uris.size();
                RectF rectF = new RectF();
                for (int i = 0; i < count; i++) {
                    try {
                        BitmapWrap bitmapWrap = new BitmapWrap();
                        File oneImage = uris.get(i);
                        int rotate = ImageUtil.getExifRotation(oneImage);
                        Bitmap bitmap = ImageUtil.getSampleBitmap(application, Uri.fromFile(oneImage), width, height, rotate);
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
                        bitmapWraps.add(bitmapWrap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                bitmapReady = true;
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

