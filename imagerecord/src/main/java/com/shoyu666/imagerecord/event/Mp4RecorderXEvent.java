package com.shoyu666.imagerecord.event;

import org.greenrobot.eventbus.EventBus;

public class Mp4RecorderXEvent {

    public static final int After_Stop = 1;
    public static final int Muxer_Video_PresentationTimeUs = 2;
    public static final int Error = 3;
    public static final int Audio_QueueInputBuffer = 4;

    public int eventId;
    public Object data;

    public static void post(int eventId, Object data) {
        Mp4RecorderXEvent event = new Mp4RecorderXEvent();
        event.eventId = eventId;
        event.data = data;
        EventBus.getDefault().post(event);
    }
}
