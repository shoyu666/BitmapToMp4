package com.shoyu666.imagerecord.model;

import com.shoyu666.imagerecord.core.IVideoFeeder;
import com.shoyu666.imagerecord.core.Mp4RecorderX;
import com.shoyu666.imagerecord.event.Mp4RecorderXEvent;
import com.shoyu666.imagerecord.log.MLog;
import com.shoyu666.imagerecord.util.TimeUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModel;

public abstract class Mp4RecorderXLifecycleViewModel extends ViewModel implements LifecycleObserver {

    public Set<IVideoFeeder> feeders = Collections.synchronizedSet(new HashSet<IVideoFeeder>());

    public synchronized void addVideoFeeder(IVideoFeeder drawer) {
        synchronized (feeders) {
            if (!feeders.contains(drawer)) {
                feeders.add(drawer);
            }
        }
    }

    public synchronized void removeVideoFeeder(IVideoFeeder drawer) {
        synchronized (feeders) {
            feeders.remove(drawer);
            drawer.relese();
        }
    }

    public Set<IVideoFeeder> getAllVideoFeeder() {
        synchronized (feeders) {
            return feeders;
        }
    }

    public boolean hasVideoFeeder() {
        synchronized (feeders) {
            return feeders.size() > 0;
        }
    }

    volatile Mp4RecorderX viewMp4Recorder;
    public long limit = System.nanoTime();
    /**
     * 流控时间
     */
    long limitOffset = 1000_000_000l;

    /**
     * 开始录制
     */
    public boolean startRecord() {
        initViewMp4Recorder();
        if (viewMp4Recorder == null) {
            MLog.reportThrowable(new NullPointerException("viewMp4Recorder=null"));
            return false;
        }
        if (TimeUtil.isOver(limit, limitOffset)) {
            limit = System.nanoTime();
            viewMp4Recorder.startRecord();
            return true;
        }
        return false;
    }

    /**
     * 结束录制
     */
    public boolean stopRecord() {
        if (TimeUtil.isOver(limit, limitOffset) && viewMp4Recorder != null) {
            if (!viewMp4Recorder.isRecording()) {
                return false;
            }
            limit = System.nanoTime();
            viewMp4Recorder.stopRecord();
            return true;
        }
        return false;
    }

    public boolean isRecording() {
        if (viewMp4Recorder != null) {
            return viewMp4Recorder.isRecording();
        }
        return false;
    }

    public long getCurrentDuration() {
        long duration = 0;
        if (viewMp4Recorder != null) {
            duration = viewMp4Recorder.getCurrentDuration();
        }
        return duration;
    }


    public void initViewMp4Recorder() {
        if (viewMp4Recorder == null) {
            synchronized (Mp4RecorderX.class) {
                viewMp4Recorder = Mp4RecorderX.create(this);
            }
        }
    }

    public void releaseAll() {
        synchronized (Mp4RecorderX.class) {
            if (viewMp4Recorder != null) {
                viewMp4Recorder.release(false);
                viewMp4Recorder = null;
            }
        }
    }

    //---------------------------------------------
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    void onStart(LifecycleOwner activity) {
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    void onStop(LifecycleOwner activity) {
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    void onPause(LifecycleOwner activity) {
        stopRecord();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    void onDestroy(LifecycleOwner activity) {
        releaseAll();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        releaseAll();
    }
    //----------------------------------------------------------------


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMp4RecorderXEvent(Mp4RecorderXEvent event) {
        switch (event.eventId) {
            case Mp4RecorderXEvent.After_Stop:
                break;
            case Mp4RecorderXEvent.Muxer_Video_PresentationTimeUs:
                break;
            case Mp4RecorderXEvent.Error:
                break;
            case Mp4RecorderXEvent.After_Muxer_Stop:
                releaseAll();
                break;
        }
        notifyMp4RecorderXEvent(event);
    }


    public abstract void notifyMp4RecorderXEvent(Mp4RecorderXEvent event);

    public abstract int getVideoHeight();

    public abstract int getVideoWidth();

    public abstract File getMp4File();

}
