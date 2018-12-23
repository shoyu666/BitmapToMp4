package com.shoyu666.imagerecord.gif;

import android.content.Context;

import android.content.res.AssetManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.gifdecoder.GifHeaderParser;
import com.bumptech.glide.gifdecoder.StandardGifDecoder;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.gif.GifBitmapProvider;
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public abstract class GifLoader {
    public BitmapPool bitmapPool;
    public ArrayPool arrayPool;
    public GifBitmapProvider provider;

    public GifLoader(Context context) {
        bitmapPool = Glide.get(context).getBitmapPool();
        arrayPool = Glide.get(context).getArrayPool();
        provider = new GifBitmapProvider(bitmapPool, arrayPool);
    }

    public GifDecoder init(byte[] data) {
        GifHeaderParser headerParser = new GifHeaderParser();
        headerParser.setData(data);
        GifHeader header = headerParser.parseHeader();
        GifDecoder decoder = new StandardGifDecoder(provider);
        decoder.setData(header, data);
        decoder.advance();
        return decoder;
    }

    public void initResoure(final AssetManager assetManager, String res) throws OutOfMemoryError {
        Observable.just(res).subscribeOn(Schedulers.newThread()).map(new Function<String, Object>() {
            @Override
            public Object apply(String res) throws Exception {
                Object result = null;
                try {
                    InputStream inputStream = assetManager.open(res);
                    byte[] data = ByteStreams.toByteArray(inputStream);
                    result = init(data);
                } catch (OutOfMemoryError e) {
                    e.printStackTrace();
                    result = e;
                } catch (Throwable e) {
                    e.printStackTrace();
                    result = e;
                }
                return result;
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Object>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Object value) {
                if (value instanceof GifDecoder) {
                    onOutNext((GifDecoder) value);
                } else if (value instanceof Throwable) {
                    onOutError((Throwable) value);
                }

            }

            @Override
            public void onError(Throwable e) {
                onOutError(e);
            }

            @Override
            public void onComplete() {
                onOutonComplete();
            }
        });
    }

    public void initResoure(final Context context, File file) throws OutOfMemoryError {
        Observable.just(file).subscribeOn(Schedulers.newThread()).map(new Function<File, Object>() {
            @Override
            public Object apply(File uri) throws Exception {
                Object result = null;
                try {
                    InputStream inputStream = new FileInputStream(uri);
                    byte[] data = ByteStreams.toByteArray(inputStream);
                    result = init(data);
                } catch (OutOfMemoryError e) {
                    e.printStackTrace();
                    result = e;
                } catch (Throwable e) {
                    e.printStackTrace();
                    result = e;
                }
                return result;
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Object>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Object value) {
                if (value instanceof GifDecoder) {
                    onOutNext((GifDecoder) value);
                } else if (value instanceof Throwable) {
                    onOutError((Throwable) value);
                }

            }

            @Override
            public void onError(Throwable e) {
                onOutError(e);
            }

            @Override
            public void onComplete() {
                onOutonComplete();
            }
        });
    }

    public abstract void onOutonComplete();

    public abstract void onOutNext(GifDecoder value);

    public abstract void onOutError(Throwable value);
}