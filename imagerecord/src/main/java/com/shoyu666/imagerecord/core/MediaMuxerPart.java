package com.shoyu666.imagerecord.core;

import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;

import com.shoyu666.imagerecord.doc.MarkVideoFeedThread;
import com.shoyu666.imagerecord.event.Mp4RecorderXEvent;
import com.shoyu666.imagerecord.livedata.RecordMp4Part;
import com.shoyu666.imagerecord.log.MLog;

import androidx.annotation.UiThread;


import java.io.File;
import java.io.IOException;

public class MediaMuxerPart {
    public static final String TAG="MediaMuxerPart";
    public MuxerThread muxerThread;
    public MediaMuxer mediaMuxer;
    public Mp4RecorderX mp4Recorder;
    public File outputFile;

    public MediaMuxerPart(File outputFile, Mp4RecorderX mp4Recorder) throws IOException {
        this.outputFile = outputFile;
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

    public void release(boolean workThread) {
        if (mediaMuxer != null) {
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
            if (workThread && outputFile != null && outputFile.exists()) {
                sendMp4File(outputFile.getAbsolutePath());
            }
            outputFile = null;
        }
    }

    public void sendMp4File(String path) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(path);
            String widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            retriever.release();
            RecordMp4Part part = new RecordMp4Part();
            part.width = Long.valueOf(widthStr);
            part.height = Long.valueOf(heightStr);
            part.durationMs = Long.valueOf(durationStr);
            part.path = path;
            MLog.d(TAG,"send After_Muxer_Stop");
            Mp4RecorderXEvent.post(Mp4RecorderXEvent.After_Muxer_Stop, part);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
