package com.shoyu666.imagerecord.core;

import android.graphics.Rect;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.android.grafika.gles.EglCore;
import com.android.grafika.gles.WindowSurface;
import com.shoyu666.imagerecord.doc.MarkVideoFeedThread;
import com.shoyu666.imagerecord.log.MLog;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import androidx.annotation.UiThread;

public class VideoDispathDrawThread extends HandlerThread {
    public static final String TAG = "VideoDispathDrawThread";
    public Mp4RecorderX mp4Recorder;
    public VideoFeedThreadHandler hanlder;
    public WindowSurface codeWindowSurface;
    private EglCore mEglCore;

    public BitmapFeeder defaultFeeder = new DefaultBitmapFeeder();

    @UiThread
    public VideoDispathDrawThread(String name, Mp4RecorderX mp4Recorder) {
        super(name);
        this.mp4Recorder = mp4Recorder;
        mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);
    }

    public void release() {
        if (codeWindowSurface != null) {
            try {
                codeWindowSurface.release();
            } catch (Exception e) {
                MLog.reportThrowable(e);
            }
        }
        releaseDrawers();
        if (mEglCore != null) {
            try {
                mEglCore.release();
            } catch (Exception e) {
                MLog.reportThrowable(e);
            }
        }
    }

    private void releaseDrawers() {
        Set<IVideoFeeder> drawers = mp4Recorder.getAllVideoFeed();
        if (drawers == null || drawers.size() == 0) {
            return;
        }
        Iterator<IVideoFeeder> iterator = drawers.iterator();
        while (iterator.hasNext()) {
            IVideoFeeder drawer = iterator.next();
            drawer.relese();
        }
    }

    @UiThread
    public void waitHandlerCreate() {
        hanlder = new VideoFeedThreadHandler(this.getLooper());
    }


    @UiThread
    public void doFrame(int what, int arg1, int arg2) {
        hanlder.sendMessage(hanlder.obtainMessage(what, arg1, arg2));
    }

    public class VideoFeedThreadHandler extends Handler {

        public static final int VideoDoFrameMsg = 1;
        public static final int SendStopToMuxer = 2;

        public VideoFeedThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case VideoDoFrameMsg:
                    long timeStampNanos = (((long) msg.arg1) << 32) |
                            (((long) msg.arg2) & 0xffffffffL);
                    if (drop(timeStampNanos)) {
                        return;
                    }
                    try {
                        long frameTime = System.nanoTime();
                        tryDispatchDraw(frameTime);
                    } catch (Exception e) {
                        MLog.reportThrowable(e);
                    } catch (OutOfMemoryError outOfMemoryError) {
                        MLog.reportError(outOfMemoryError);
                    }
                    break;
                case SendStopToMuxer:
                    break;
            }
        }
    }


    public boolean drop(long timeStampNanos) {
        boolean pass = false;
        long diff = (System.nanoTime() - timeStampNanos) / 1000000;
        if (diff > 15) {
            pass = true;
        }
        return pass;
    }

    @MarkVideoFeedThread
    public void tryDispatchDraw(long timeStampNanos) {
        MLog.d(TAG, "#####feed");
        if (mp4Recorder != null && mp4Recorder.videoPart != null) {
            synchronized (mp4Recorder.videoPart.surfaceLock) {
                if (mp4Recorder.videoPart.surfaceCopy == null) {
                    return;
                }
                if (codeWindowSurface == null) {
                    codeWindowSurface = new WindowSurface(mEglCore, mp4Recorder.videoPart.surfaceCopy, false);
                    codeWindowSurface.makeCurrent();
                }
                if (codeWindowSurface != null) {
                    dispatchDraw(timeStampNanos);
                }
            }
        }
        MLog.d(TAG, "#####feed end");
    }

    @MarkVideoFeedThread
    private void dispatchDraw(long timeStampNanos) {
        Rect mVideoRect = mp4Recorder.videoPart.mVideoRect;
        mp4Recorder.mediaMuxerPart.frameAvailableSoon();
        codeWindowSurface.makeCurrent();
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glViewport(mVideoRect.left, mVideoRect.top,
                mVideoRect.width(), mVideoRect.height());
        boolean hasDraw = false;
        Set<IVideoFeeder> drawers = mp4Recorder.getAllVideoFeed();
        if (drawers != null && drawers.size() > 0) {
            Iterator<IVideoFeeder> iterator = drawers.iterator();
            while (iterator.hasNext()) {
                IVideoFeeder drawer = iterator.next();
                boolean drawed = drawer.draw(mVideoRect, timeStampNanos);
                hasDraw = drawed ? drawed : hasDraw;
            }
        }
        if (!hasDraw) {
            defaultFeeder.draw(mVideoRect, timeStampNanos);
        }
        codeWindowSurface.setPresentationTime(timeStampNanos);
        codeWindowSurface.swapBuffers();
    }
}
