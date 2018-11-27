package com.shoyu666.imagerecord.core;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.android.grafika.gles.EglCore;
import com.android.grafika.gles.WindowSurface;
import com.shoyu666.imagerecord.log.MLog;

public class VideoFeedThread extends HandlerThread {
    public static final String TAG = "VideoFeedThread";
    public Mp4RecorderX mp4Recorder;
    public VideoFeedThreadHandler hanlder;
    public WindowSurface codeWindowSurface;
    private EglCore mEglCore;
    public GLBitmap glBitmap;

    public VideoFeedThread(String name, Mp4RecorderX mp4Recorder) {
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
        if (glBitmap != null) {
            glBitmap.release();
        }
        if (mEglCore != null) {
            try {
                mEglCore.release();
            } catch (Exception e) {
                MLog.reportThrowable(e);
            }
        }
    }

    public void waitHandlerCreate() {
        hanlder = new VideoFeedThreadHandler(this.getLooper());
    }

    public void sendMessage(int what, int arg1, int arg2) {
        hanlder.sendMessage(hanlder.obtainMessage(what, arg1, arg2));
    }

    public class VideoFeedThreadHandler extends Handler {

        public static final int VideoFeedFrameMsg = 1;

        public VideoFeedThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case VideoFeedFrameMsg:
                    long timeStampNanos = (((long) msg.arg1) << 32) |
                            (((long) msg.arg2) & 0xffffffffL);
                    if (drop(timeStampNanos)) {
                        return;
                    }
                    try {
                        feed();
                    } catch (Exception e) {
                        MLog.reportThrowable(e);
                    } catch (OutOfMemoryError outOfMemoryError) {
                        MLog.reportError(outOfMemoryError);
                    }
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

    public void feed() {
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
                    drawBitmapToCode();
                }
            }
        }
        MLog.d(TAG, "#####feed end");
    }

    private void drawBitmapToCode() {
        Bitmap snap = mp4Recorder.mStageView.stageBitmap;
        Rect mVideoRect = mp4Recorder.videoPart.mVideoRect;
        long timeStampNanos = System.nanoTime();
        if (snap == null || snap.isRecycled()) {
            MLog.e(TAG, "inisValide");
            return;
        }
        int viewWidth = snap.getWidth();
        int viewHeight = snap.getHeight();
        if (glBitmap == null) {
            glBitmap = new GLBitmap();
            glBitmap.fix(snap, mVideoRect.width(), mVideoRect.height());
        }
        mp4Recorder.mediaMuxerPart.frameAvailableSoon();
        codeWindowSurface.makeCurrent();
        GLES20.glViewport(0, 0, viewWidth, viewHeight);
        //
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glViewport(mVideoRect.left, mVideoRect.top,
                mVideoRect.width(), mVideoRect.height());
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(mVideoRect.left, mVideoRect.top,
                mVideoRect.width(), mVideoRect.height());
        glBitmap.draw(snap, timeStampNanos);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
        codeWindowSurface.setPresentationTime(timeStampNanos);
        codeWindowSurface.swapBuffers();
//        feedBitmap.recycle();
    }
}
