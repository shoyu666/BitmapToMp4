package com.shoyu666.imagerecord.core;

import android.graphics.Rect;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import androidx.annotation.UiThread;

import android.view.Choreographer;
import android.view.Surface;

import com.shoyu666.imagerecord.doc.MarkMuxerThread;
import com.shoyu666.imagerecord.log.MLog;

import java.io.IOException;


public class VideoPart implements Choreographer.FrameCallback {
    public static final String TAG = "VideoPart";
    public static final String MIME_TYPE = "video/avc";
    public VideoFeedThread videoFeedThread;
    public volatile Surface surface;
    public Object surfaceLock = new Object();
    public MediaCodec mMediaCodec;
    public Rect mVideoRect;
    public volatile Surface surfaceCopy;
    public Mp4RecorderX mp4Recorder;
    //
    public MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    public int videoTrackId = -1;
    public volatile long presentationTimeUs = 0;

    //
    @UiThread
    public VideoPart(int width, int height, Mp4RecorderX mp4Recorder) throws IOException {
        this.mp4Recorder = mp4Recorder;
        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        String formatInfo = null;
        try {
            MediaCodecInfo info = mMediaCodec.getCodecInfo();
            MediaCodecInfo.CodecCapabilities capabilities = info.getCapabilitiesForType(MIME_TYPE);
            int[] colorFormats = capabilities.colorFormats;
            for (int i : colorFormats) {
                MLog.d(TAG, "colorFormats " + i);
                formatInfo = formatInfo + i + "#";
            }
        } catch (Exception e) {
            MLog.reportThrowable(e);
            MLog.e(TAG, "Exception " + e.getMessage());
        }
        mVideoRect = new Rect();
        mVideoRect.set(0, 0, width, height);
        try {
            configAndCreateSurface();
        } catch (IllegalStateException e) {
            MLog.reportThrowable(new Exception(formatInfo));
            throw e;
        }
        mMediaCodec.start();
    }

    @UiThread
    public void startRecord() {
        if (videoFeedThread == null) {
            videoFeedThread = new VideoFeedThread("VideoFeedThread", mp4Recorder);
            videoFeedThread.start();
            videoFeedThread.waitHandlerCreate();
        }
        //pauseRecord 会remove
        Choreographer.getInstance().postFrameCallback(this);
        synchronized (surfaceLock) {
            surfaceCopy = surface;
        }
    }


    @Override
    public void doFrame(long frameTimeNanos) {
        synchronized (surfaceLock) {
            if (surfaceCopy != null && mp4Recorder.mStageView != null && mp4Recorder.mStageView.stageBitmap != null) {
                Choreographer.getInstance().postFrameCallback(this);
                videoFeedThread.sendMessage(VideoFeedThread.VideoFeedThreadHandler.VideoFeedFrameMsg, (int) (frameTimeNanos >> 32), (int) frameTimeNanos);

            }
        }
    }

//    public Bitmap viewToBitmap(View view) {
//        if (view == null) {
//            return null;
//        }
//        int w = view.getWidth();
//        int h = view.getHeight();
//        if (w <= 0) {
//            return null;
//        }
//        Bitmap cache = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(cache);
//        view.draw(canvas);
//        return cache;
//    }

    @UiThread
    public void pauseRecord() {
        if (videoFeedThread != null) {
            videoFeedThread.hanlder.removeMessages(VideoFeedThread.VideoFeedThreadHandler.VideoFeedFrameMsg);
        }
        synchronized (surfaceLock) {
            surfaceCopy = null;
        }
//        mMediaCodec.stop();
    }

    @MarkMuxerThread
    public void release() {
        if (mMediaCodec != null) {
            try {
                mMediaCodec.stop();
            } catch (Exception e) {
                MLog.reportThrowable(e);
            }
            try {
                mMediaCodec.release();
            } catch (Exception e) {
                MLog.reportThrowable(e);
            }
            mMediaCodec = null;
        }
        if (videoFeedThread != null) {
            videoFeedThread.release();
            videoFeedThread.quit();
            videoFeedThread = null;
        }
        videoTrackId = -1;
        presentationTimeUs = 0;
    }

    @UiThread
    public void configAndCreateSurface() {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mVideoRect.width(), mVideoRect.height());
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 2500000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        synchronized (surfaceLock) {
            surface = mMediaCodec.createInputSurface();
        }
    }
}
