package com.shoyu666.imagerecord.render;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import com.bumptech.glide.Glide;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.gifdecoder.GifHeaderParser;
import com.bumptech.glide.gifdecoder.StandardGifDecoder;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.gif.GifBitmapProvider;
import com.google.common.io.ByteStreams;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import java.io.InputStream;

public class GifRender implements ISpiritRender {

    GifDecoder decoder;

    public BitmapPool bitmapPool;

    public ArrayPool arrayPool;

    public GifBitmapProvider provider;

    public Paint bitmapPaint;

    public RectF rect = new RectF();

    public Bitmap frame;

    public long preShowTime;

    public int nextDelay = 0;

    public GifRender(Context context) {
        bitmapPool = Glide.get(context).getBitmapPool();
        arrayPool = Glide.get(context).getArrayPool();
        provider = new GifBitmapProvider(bitmapPool, arrayPool);
        bitmapPaint = new Paint();
    }

    @Override
    public boolean draw(Canvas canvas, float width, float height) {
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
            float rote = width / height;
            float bitmapW = frame.getWidth();
            float bitmapH = frame.getHeight();
            float bitmapRote = bitmapW / bitmapH;
            if (bitmapRote > rote) {
                rect.left = 0;
                rect.right = width;
                float fixH = width / bitmapRote;
                rect.top = (height - fixH) / 2;
                rect.bottom = rect.top + fixH;
            } else {
                float fixW = height * bitmapRote;
                rect.left = (width - fixW) / 2;
                rect.right = rect.left + fixW;
                rect.top = 0;
                rect.bottom = height;
            }
            canvas.drawBitmap(frame, null, rect, bitmapPaint);
        }
        return true;
    }

    public void setAssetRes(final AssetManager assetManager, String res) {
        Observable.just(res).subscribeOn(Schedulers.io()).map(new Function<String, GifDecoder>() {
            @Override
            public GifDecoder apply(String s) throws Exception {
                InputStream inputStream = assetManager.open(s);
                byte[] data = ByteStreams.toByteArray(inputStream);
                inputStream.close();
                GifHeaderParser headerParser = new GifHeaderParser();
                headerParser.setData(data);
                GifHeader header = headerParser.parseHeader();
                GifDecoder decoder = new StandardGifDecoder(provider);
                decoder.setData(header, data);
                decoder.advance();
                return decoder;
            }
        }).subscribe(new Observer<GifDecoder>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(GifDecoder value) {
                decoder = value;
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
