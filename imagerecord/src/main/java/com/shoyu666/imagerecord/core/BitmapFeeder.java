package com.shoyu666.imagerecord.core;

import android.graphics.Bitmap;
import android.graphics.Rect;

public abstract class BitmapFeeder implements IVideoFeeder {
    public volatile  GLBitmap glBitmap;

    @Override
    public boolean draw(Rect mVideoRect, long timeStampNanos) {
        Bitmap oneFrame = getOneFrame(mVideoRect, timeStampNanos);
        if (oneFrame == null || oneFrame.isRecycled()) {
            return false;
        }
        if (glBitmap == null) {
            glBitmap = new GLBitmap();
            glBitmap.fix(oneFrame, mVideoRect.width(), mVideoRect.height());
        }
        glBitmap.draw(oneFrame, timeStampNanos);
        return true;
    }

    public abstract Bitmap getOneFrame(Rect mVideoRect, long timeStampNanos);

    @Override
    public void relese() {
        if (glBitmap != null) {
            glBitmap.release();
            glBitmap=null;
        }
    }
}
