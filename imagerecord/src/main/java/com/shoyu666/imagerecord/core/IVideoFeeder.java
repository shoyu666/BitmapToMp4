package com.shoyu666.imagerecord.core;

import android.graphics.Rect;

public interface IVideoFeeder {
    boolean draw(Rect mVideoRect,long timeStampNanos);

    void relese();
}
