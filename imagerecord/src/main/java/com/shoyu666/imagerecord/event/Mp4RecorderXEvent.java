package com.shoyu666.imagerecord.event;

import org.greenrobot.eventbus.EventBus;

public class Mp4RecorderXEvent {
    public static final int Before_Start = 1;
    public static final int Before_Stop = 2;
    public static final int After_Stop = 3;
    public static final int Muxer_Video_PresentationTimeUs = 4;
    public static final int Error = 5;
    public static final int Audio_QueueInputBuffer = 6;
    public static final int After_Muxer_Stop = 7;

    public int eventId;
    public Object data;

    public static void post(int eventId, Object data) {
        Mp4RecorderXEvent event = new Mp4RecorderXEvent();
        event.eventId = eventId;
        event.data = data;
        EventBus.getDefault().post(event);
    }
}
