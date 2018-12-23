package com.shoyu666.imagerecord.core;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.shoyu666.imagerecord.doc.MarkVideoFeedThread;
import com.shoyu666.imagerecord.event.Mp4RecorderXEvent;
import com.shoyu666.imagerecord.log.MLog;

import androidx.annotation.UiThread;

import static com.shoyu666.imagerecord.core.MuxerThread.MuxerThreadHanlder.DrainMsg;


public class MuxerThread extends HandlerThread {
    public static final String TAG = "MuxerThread";
    public MuxerThreadHanlder hanlder;
    public Mp4RecorderX mp4Recorder;

    public MuxerThread(Mp4RecorderX mp4Recorder, String name) {
        super(name);
        this.mp4Recorder = mp4Recorder;
    }

    public void waitHandlerCreate() {
        hanlder = new MuxerThreadHanlder(this.getLooper());
    }

    @MarkVideoFeedThread
    public void frameAvailableSoon() {
        MLog.d(TAG, "frameAvailableSoon");
        hanlder.sendEmptyMessage(DrainMsg);
        MLog.d(TAG, "frameAvailableSoon end");
    }

    @UiThread
    public void stopRecord() {
        MLog.d(TAG, "stopRecord");
        hanlder.removeMessages(DrainMsg);
        MLog.d(TAG, "Msg_StopRecord");
        hanlder.sendEmptyMessage(MuxerThreadHanlder.Msg_StopRecord);
    }

    public class MuxerThreadHanlder extends Handler {
        public static final int DrainMsg = 1;
        public static final int Msg_StopRecord = 2;

        public MuxerThreadHanlder(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case DrainMsg:
                    try {
                        MLog.d(TAG, "#####DrainMsg");
                        if (mp4Recorder != null) {
                            mp4Recorder.drainVideoEncoder(false);
                            MLog.d(TAG, "#####DrainMsg 1");
                            mp4Recorder.audioPart.feed(mp4Recorder.offset);
                            MLog.d(TAG, "#####DrainMsg 2");
                        }
                        MLog.d(TAG, "#####DrainMsg  end");
                    } catch (Exception e) {
                        error(e);
                    }
                    break;
                case Msg_StopRecord:
                    try {
                        removeMessages(DrainMsg);
                        if (mp4Recorder != null) {
                            mp4Recorder.drainVideoEncoder(true);
                            mp4Recorder.drainAudioEncoder(true);
                            Mp4RecorderXEvent.post(Mp4RecorderXEvent.After_Stop, null);
                        }
                        quitSafely();
                        mp4Recorder = null;
                    } catch (Exception e) {
                        error(e);
                    }
                    break;
            }
        }
    }

    public void error(Exception e) {
        Mp4RecorderXEvent.post(Mp4RecorderXEvent.Error, e);
        MLog.reportThrowable(e);
    }
}
