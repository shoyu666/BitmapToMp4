package com.shoyu666.imagerecord.core;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;


import androidx.annotation.UiThread;

import com.shoyu666.imagerecord.BuildConfig;
import com.shoyu666.imagerecord.doc.MarkMuxerThread;
import com.shoyu666.imagerecord.event.Mp4RecorderXEvent;
import com.shoyu666.imagerecord.log.MLog;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static android.media.AudioRecord.READ_NON_BLOCKING;
import static android.media.AudioRecord.RECORDSTATE_RECORDING;
import static com.shoyu666.imagerecord.event.Mp4RecorderXEvent.Audio_QueueInputBuffer;

public class AudioPart {
    public static final String TAG = "AudioPart";
    public static final String MIME_TYPE = "audio/mp4a-latm";
    public AudioRecord audioRecord;
    public MediaCodec mMediaCodec;
    public static final int SAMPLE_RATE = 44100;
    public static final int BIT_RATE = 128000;
    public static final int ChannelConfig = AudioFormat.CHANNEL_IN_MONO;
    public static final int PCM_BIT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int ChannelCount = 1;
    private byte[] mBuffer;
    private int bufferSize;
    public Mp4RecorderX mp4Recorder;
    //
    public MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    public int audioTrackId = -1;
    public volatile long presentationTimeUs = 0;

    //
    public AudioPart(Mp4RecorderX mp4Recorder) throws IOException {
        this.mp4Recorder = mp4Recorder;
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, ChannelConfig, PCM_BIT);
        mBuffer = new byte[bufferSize];
        audioRecord = new AudioRecord(getCompatibleAudioSource(), SAMPLE_RATE, ChannelConfig, PCM_BIT, bufferSize);
        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        config();
        mMediaCodec.start();
    }

    private int getCompatibleAudioSource() {
        int audioSoure = MediaRecorder.AudioSource.MIC;
        return audioSoure;
    }

    @UiThread
    public void startRecord() {
        if (audioRecord != null) {
            audioRecord.startRecording();
        }
    }

    @UiThread
    public void pauseRecord() {
        audioRecord.stop();
    }

    @MarkMuxerThread
    public void release() {
        try {
            if (audioRecord != null) {
                audioRecord.release();
                audioRecord = null;
            }
        } catch (Exception e) {
            MLog.reportThrowable(e);
        }
        try {
            if (mMediaCodec != null) {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            }
        } catch (Exception e) {
            MLog.reportThrowable(e);
        }
        audioTrackId = -1;
        presentationTimeUs = -1;
    }


    public void config() {
        MediaFormat createAudioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, ChannelCount);
        createAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, 2);
        createAudioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, ChannelConfig);
        createAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        createAudioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);
        mMediaCodec.configure(createAudioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }


    @MarkMuxerThread
    public void feed(long offset) {
        if (audioRecord != null) {
            int readSize = audioRecord.read(mBuffer, 0, bufferSize);
            if (readSize > 0) {
                long nanoTime = (System.nanoTime() / 1000) - offset;
                int dequeueInputBuffer = mMediaCodec.dequeueInputBuffer(-1);
                if (dequeueInputBuffer != -1) {
                    ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(dequeueInputBuffer);
                    inputBuffer.clear();
                    inputBuffer.put(mBuffer);
                    mMediaCodec.queueInputBuffer(dequeueInputBuffer, 0, readSize, nanoTime, 0);
                    mp4Recorder.drainAudioEncoder(false);
                    Mp4RecorderXEvent.post(Audio_QueueInputBuffer, mBuffer);
                }
            }
        }
    }


    public boolean isAudioRecording() {
        return audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED && audioRecord.getRecordingState() == RECORDSTATE_RECORDING;
    }
}
