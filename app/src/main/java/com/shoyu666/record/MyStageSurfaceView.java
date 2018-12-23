package com.shoyu666.record;

import android.content.Context;
import android.util.AttributeSet;
import com.shoyu666.imagerecord.stage.BitmapProvider;

public class MyStageSurfaceView extends BitmapProvider {


    public MyStageSurfaceView(Context context) {
        super(context);
    }

    public MyStageSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyStageSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public MyStageSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void init() {
        super.init();
    }
}
