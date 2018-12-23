package com.shoyu666.imagerecord.stage;

import org.greenrobot.eventbus.EventBus;

public class ChoreographerEvent {
    public long frameTimeNanos;

    public static void post(long frameTimeNanos) {
        ChoreographerEvent event = new ChoreographerEvent();
        event.frameTimeNanos = frameTimeNanos;
        EventBus.getDefault().post(event);
    }
}
