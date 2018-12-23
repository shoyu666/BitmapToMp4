package com.shoyu666.imagerecord.core;


import android.media.MediaCodec;
import android.media.MediaFormat;

import com.shoyu666.imagerecord.doc.MarkMuxerThread;
import com.shoyu666.imagerecord.event.Mp4RecorderXEvent;
import com.shoyu666.imagerecord.log.MLog;
import com.shoyu666.imagerecord.model.Mp4RecorderXLifecycleViewModel;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

import androidx.annotation.UiThread;

import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
import static android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM;
import static android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED;

public class Mp4RecorderX {
    public static final String TAG = "Mp4RecorderX";
    public VideoPart videoPart;
    public AudioPart audioPart;
    public MediaMuxerPart mediaMuxerPart;
    public volatile long offset = 0;

    public File outputFile;

    public volatile Mp4RecorderXLifecycleViewModel lifecycleMp4RecorderX;

    private Mp4RecorderX(int video_width, int video_height, File outputFile) throws IOException {
        videoPart = new VideoPart(video_width, video_height, this);
        audioPart = new AudioPart(this);
        mediaMuxerPart = new MediaMuxerPart(outputFile, this);
    }

    @UiThread
    public static Mp4RecorderX create(Mp4RecorderXLifecycleViewModel lifecycleMp4RecorderX) {
        Mp4RecorderX recorder = null;
        try {
            File outFile = lifecycleMp4RecorderX.getMp4File();
            recorder = new Mp4RecorderX(lifecycleMp4RecorderX.getVideoWidth(), lifecycleMp4RecorderX.getVideoHeight(), outFile);
            recorder.outputFile = outFile;
            recorder.lifecycleMp4RecorderX = lifecycleMp4RecorderX;
        } catch (Exception e) {
            MLog.reportThrowable(e);
        }
        return recorder;
    }

    @UiThread
    public void startRecord() {
        try {
            Mp4RecorderXEvent.post(Mp4RecorderXEvent.Before_Start, null);
            this.offset = (System.nanoTime() / 1000);
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
            Mp4RecorderXEvent.post(Mp4RecorderXEvent.Before_Stop, null);
            videoPart.pauseRecord();
            audioPart.pauseRecord();
            mediaMuxerPart.stopRecord();
//            videoPart.sendeStopToMuxer();
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
        while (true&&this.videoPart.mMediaCodec!=null) {
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
                    MediaCodec.BufferInfo videoInfo = this.videoPart.mBufferInfo;
                    if (videoInfo.size != 0) {
                        outputBuffer.position(videoInfo.offset);
                        outputBuffer.limit(videoInfo.offset + videoInfo.size);
                    }
                    MediaCodec.BufferInfo bufferInfo = this.videoPart.mBufferInfo;
                    bufferInfo.presentationTimeUs -= this.offset;
                    //
                    if (this.videoPart.mBufferInfo.presentationTimeUs < this.videoPart.presentationTimeUs) {
                        this.videoPart.mBufferInfo.presentationTimeUs = this.videoPart.presentationTimeUs + 1000;
                    }
                    this.videoPart.presentationTimeUs = this.videoPart.mBufferInfo.presentationTimeUs;

                    if (this.videoPart.mBufferInfo.flags != BUFFER_FLAG_CODEC_CONFIG) {
                        this.mediaMuxerPart.mediaMuxer.writeSampleData(this.videoPart.videoTrackId, outputBuffer, this.videoPart.mBufferInfo);
                        MLog.e(TAG, "video  write endOfStream= " + this.audioPart.mBufferInfo.presentationTimeUs + "  " + endOfStream);
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
            MediaFormat audioFormat = this.audioPart.mMediaCodec.getOutputFormat();
            this.audioPart.audioTrackId = this.mediaMuxerPart.mediaMuxer.addTrack(audioFormat);
            this.mediaMuxerPart.mediaMuxer.start();
            //
            ByteBuffer byteBuffer11 = audioFormat.getByteBuffer("csd-0");
            if (byteBuffer11 != null) {
                this.mediaMuxerPart.mediaMuxer.writeSampleData(this.audioPart.audioTrackId, byteBuffer11, this.audioPart.mBufferInfo);
            }
            ByteBuffer byteBuffer22 = audioFormat.getByteBuffer("csd-1");
            if (byteBuffer22 != null) {
                this.mediaMuxerPart.mediaMuxer.writeSampleData(this.audioPart.audioTrackId, byteBuffer22, this.audioPart.mBufferInfo);
            }
            //
            //
            MediaFormat videoFormat = this.videoPart.mMediaCodec.getOutputFormat();
            ByteBuffer byteBuffer1 = videoFormat.getByteBuffer("csd-0");
            if (byteBuffer1 != null) {
                this.mediaMuxerPart.mediaMuxer.writeSampleData(this.videoPart.videoTrackId, byteBuffer1, this.videoPart.mBufferInfo);
            }
            ByteBuffer byteBuffer2 = videoFormat.getByteBuffer("csd-1");
            if (byteBuffer2 != null) {
                this.mediaMuxerPart.mediaMuxer.writeSampleData(this.videoPart.videoTrackId, byteBuffer2, this.videoPart.mBufferInfo);
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
                    MediaCodec.BufferInfo audioInfo = this.audioPart.mBufferInfo;
                    if (audioInfo.size != 0) {
                        outputBuffer.position(audioInfo.offset);
                        outputBuffer.limit(audioInfo.offset + audioInfo.size);
                    }
                    if (audioInfo.flags != BUFFER_FLAG_CODEC_CONFIG && audioInfo.presentationTimeUs > this.audioPart.presentationTimeUs) {
                        this.mediaMuxerPart.mediaMuxer.writeSampleData(this.audioPart.audioTrackId, outputBuffer, this.audioPart.mBufferInfo);
                        MLog.d(TAG, "audio  write endOfStream= " + this.audioPart.mBufferInfo.presentationTimeUs + "   " + endOfStream);
                        this.audioPart.presentationTimeUs = this.audioPart.mBufferInfo.presentationTimeUs;
                    }
                    this.audioPart.mMediaCodec.releaseOutputBuffer(dequeueOutputBuffer, false);
                }
                dequeueOutputBuffer = this.audioPart.mMediaCodec.dequeueOutputBuffer(this.audioPart.mBufferInfo, 100);
            } else {
                return;
            }
        }
        release(true);
    }

    @MarkMuxerThread
    public void release(boolean workThread) {
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
            mediaMuxerPart.release(workThread);
        } catch (Exception e) {
            MLog.reportThrowable(e);
        }
    }

    public boolean isRecording() {
        return audioPart.isAudioRecording();
    }

    public long getCurrentDuration() {
        long duration = 0;
        if (audioPart != null && videoPart != null) {
            long a = audioPart.presentationTimeUs;
            long v = videoPart.presentationTimeUs;
            long biger = Math.max(a, v);
            duration = biger / 1000;
        }
        return duration;
    }

    public synchronized Set<IVideoFeeder> getAllVideoFeed() {
        return lifecycleMp4RecorderX.getAllVideoFeeder();
    }
}
