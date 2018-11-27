package com.shoyu666.imagerecord.core;


import android.media.MediaCodec;
import android.media.MediaFormat;

import androidx.annotation.UiThread;

import com.shoyu666.imagerecord.doc.MarkMuxerThread;
import com.shoyu666.imagerecord.event.Mp4RecorderXEvent;
import com.shoyu666.imagerecord.log.MLog;
import com.shoyu666.imagerecord.stage.StageView;


import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
import static android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM;
import static android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED;

public class Mp4RecorderX {
    public static final String TAG = "Mp4RecorderX";
    public VideoPart videoPart;
    public AudioPart audioPart;
    public MediaMuxerPart mediaMuxerPart;
    public volatile long offset = 0;
    public volatile long lastOffset = 0;
    public volatile long mPauseTime = 0;
    public StageView mStageView;
    //

    public File outputFile;

    private Mp4RecorderX(int video_width, int video_height, File outputFile) throws IOException {
        videoPart = new VideoPart(video_width, video_height, this);
        audioPart = new AudioPart(this);
        //MarkMuxerThread start
        mediaMuxerPart = new MediaMuxerPart(outputFile, this);
    }

    @UiThread
    public static Mp4RecorderX create(int video_width, int video_height, File outputFile) {
        Mp4RecorderX recorder = null;
        try {
            recorder = new Mp4RecorderX(video_width, video_height, outputFile);
            recorder.outputFile = outputFile;
        } catch (Exception e) {
            MLog.reportThrowable(e);
        }
        return recorder;
    }

    @UiThread
    public void startRecord() {
        try {
            this.lastOffset = this.offset;
            this.offset += (System.nanoTime() / 1000) - this.mPauseTime;
            videoPart.startRecord();
            audioPart.startRecord();
            mediaMuxerPart.startRecord();
        } catch (Exception e) {
            Mp4RecorderXEvent.post(Mp4RecorderXEvent.Error, e);
            MLog.reportThrowable(e);
        }
    }

    @UiThread
    public void stopRecord() {
        try {
            videoPart.pauseRecord();
            audioPart.pauseRecord();
            mediaMuxerPart.stopRecord();
        } catch (Exception e) {
            Mp4RecorderXEvent.post(Mp4RecorderXEvent.Error, e);
            MLog.reportThrowable(e);
        }
    }


    @MarkMuxerThread
    public void drainVideoEncoder(boolean endOfStream) {
        if (endOfStream) {
            videoPart.mMediaCodec.signalEndOfInputStream();
            MLog.d(TAG, "drainVideoEncoder  endOfStream=" + endOfStream);
        } else {
            MLog.d(TAG, "drainVideoEncoder");
        }
        while (true) {
            int dequeueOutputBuffer = this.videoPart.mMediaCodec.dequeueOutputBuffer(this.videoPart.mBufferInfo, (long) (this.videoPart.videoTrackId == -1 ? -1 : 0));
            if (dequeueOutputBuffer == INFO_OUTPUT_FORMAT_CHANGED) {
                this.videoPart.videoTrackId = this.mediaMuxerPart.mediaMuxer.addTrack(this.videoPart.mMediaCodec.getOutputFormat());
                MLog.d(TAG, "drainVideoEncoder INFO_OUTPUT_FORMAT_CHANGED");
                return;
            } else if (this.audioPart.audioTrackId == -1) {
                break;
            } else if (this.videoPart.mBufferInfo.flags == BUFFER_FLAG_END_OF_STREAM) {
                MLog.d(TAG, "drainVideoEncoder BUFFER_FLAG_END_OF_STREAM");
                return;
            } else {
                if (dequeueOutputBuffer >= 0) {
                    ByteBuffer outputBuffer = this.videoPart.mMediaCodec.getOutputBuffer(dequeueOutputBuffer);
//                    Assert.assertNotNull(outputBuffer);
                    //
                    MediaCodec.BufferInfo bufferInfo;
                    if (this.videoPart.mBufferInfo.presentationTimeUs > this.mPauseTime) {
                        bufferInfo = this.videoPart.mBufferInfo;
                        bufferInfo.presentationTimeUs -= this.offset;
                    } else {
                        bufferInfo = this.videoPart.mBufferInfo;
                        bufferInfo.presentationTimeUs -= this.lastOffset;
                    }
                    if (this.videoPart.mBufferInfo.presentationTimeUs < this.videoPart.presentationTimeUs) {
                        this.videoPart.mBufferInfo.presentationTimeUs = this.videoPart.presentationTimeUs + 1000;
                    }
                    this.videoPart.presentationTimeUs = this.videoPart.mBufferInfo.presentationTimeUs;

                    if (this.videoPart.mBufferInfo.flags != BUFFER_FLAG_CODEC_CONFIG) {
                        this.mediaMuxerPart.mediaMuxer.writeSampleData(this.videoPart.videoTrackId, outputBuffer, this.videoPart.mBufferInfo);
                        MLog.d(TAG, "drainVideoEncoder writeSampleData video  endOfStream= " + this.audioPart.mBufferInfo.presentationTimeUs + endOfStream);
                        //notify ui record ms
                        Mp4RecorderXEvent.post(Mp4RecorderXEvent.Muxer_Video_PresentationTimeUs, this.videoPart.presentationTimeUs);
                    }
                    MLog.d(TAG, "drainVideoEncoder releaseOutputBuffer");
                    this.videoPart.mMediaCodec.releaseOutputBuffer(dequeueOutputBuffer, false);
                }
                if (endOfStream) {
                    endOfStream = true;
                } else {
                    MLog.d(TAG, "drainVideoEncoder return");
                    return;
                }
            }
        }
    }

    @MarkMuxerThread
    public void drainAudioEncoder(boolean endOfStream) {
        if (endOfStream) {
            long nanoTime = (System.nanoTime() / 1000);
            int dequeueInputBuffer = audioPart.mMediaCodec.dequeueInputBuffer(-1);
            audioPart.mMediaCodec.queueInputBuffer(dequeueInputBuffer, 0, 0, nanoTime, BUFFER_FLAG_END_OF_STREAM);
        }
        int dequeueOutputBuffer = this.audioPart.mMediaCodec.dequeueOutputBuffer(audioPart.mBufferInfo, 100);
        if (dequeueOutputBuffer == INFO_OUTPUT_FORMAT_CHANGED) {
            this.audioPart.audioTrackId = this.mediaMuxerPart.mediaMuxer.addTrack(this.audioPart.mMediaCodec.getOutputFormat());
            this.mediaMuxerPart.mediaMuxer.start();
            //
            MediaFormat outputFormat = this.videoPart.mMediaCodec.getOutputFormat();
            ByteBuffer byteBuffer1 = outputFormat.getByteBuffer("csd-0");
            if (byteBuffer1 != null) {
                this.mediaMuxerPart.mediaMuxer.writeSampleData(this.videoPart.videoTrackId, byteBuffer1, this.videoPart.mBufferInfo);
            }
            ByteBuffer byteBuffer2 = outputFormat.getByteBuffer("csd-1");
            if (byteBuffer2 != null) {
                this.mediaMuxerPart.mediaMuxer.writeSampleData(this.videoPart.videoTrackId, byteBuffer1, this.videoPart.mBufferInfo);
            }
            //
        }
        while (true) {
            if (dequeueOutputBuffer >= 0 || endOfStream) {
                if (this.audioPart.mBufferInfo.flags == BUFFER_FLAG_END_OF_STREAM) {
                    break;
                }
                if (dequeueOutputBuffer >= 0) {
                    ByteBuffer outputBuffer = this.audioPart.mMediaCodec.getOutputBuffer(dequeueOutputBuffer);
                    if (this.audioPart.mBufferInfo.flags != BUFFER_FLAG_CODEC_CONFIG && this.audioPart.mBufferInfo.presentationTimeUs > this.audioPart.presentationTimeUs) {
                        this.mediaMuxerPart.mediaMuxer.writeSampleData(this.audioPart.audioTrackId, outputBuffer, this.audioPart.mBufferInfo);
                        MLog.d(TAG, "writeSampleData audio  endOfStream= " + this.audioPart.mBufferInfo.presentationTimeUs + endOfStream);
                        this.audioPart.presentationTimeUs = this.audioPart.mBufferInfo.presentationTimeUs;
                    }
                    this.audioPart.mMediaCodec.releaseOutputBuffer(dequeueOutputBuffer, false);
                }
                dequeueOutputBuffer = this.audioPart.mMediaCodec.dequeueOutputBuffer(this.audioPart.mBufferInfo, 100);
            } else {
                return;
            }
        }
        release();
    }

    @MarkMuxerThread
    public void release() {
        try {
            videoPart.release();
        } catch (Exception e) {
            MLog.reportThrowable(e);
        }
        try {
            audioPart.release();
        } catch (Exception e) {
            MLog.reportThrowable(e);
        }
        try {
            mediaMuxerPart.release();
        } catch (Exception e) {
            MLog.reportThrowable(e);
        }
        if (mStageView != null) {
            mStageView = null;
        }
    }

    public boolean isRecording() {
        return audioPart.isAudioRecording();
    }
}
