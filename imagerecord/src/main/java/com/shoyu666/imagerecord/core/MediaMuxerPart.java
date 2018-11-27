package com.shoyu666.imagerecord.core;

import android.media.MediaMuxer;

import com.shoyu666.imagerecord.doc.MarkVideoFeedThread;

import androidx.annotation.UiThread;


import java.io.File;
import java.io.IOException;

public class MediaMuxerPart {
    public MuxerThread muxerThread;
    public MediaMuxer mediaMuxer;
    public Mp4RecorderX mp4Recorder;

    public MediaMuxerPart(File outputFile, Mp4RecorderX mp4Recorder) throws IOException {
        this.mp4Recorder = mp4Recorder;
        mediaMuxer = new MediaMuxer(outputFile.getPath(),
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        muxerThread = new MuxerThread(mp4Recorder, "MarkMuxerThread");
        muxerThread.start();
        muxerThread.waitHandlerCreate();
    }

    public void startRecord() {

    }

    @MarkVideoFeedThread
    public void frameAvailableSoon() {
        muxerThread.frameAvailableSoon();
    }
    @UiThread
    public void stopRecord() {
        muxerThread.stopRecord();
    }

    public void release() {
        if (mediaMuxer != null) {
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
        }
    }
}
