package com.shoyu666.imagerecord.stage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.shoyu666.imagerecord.core.Mp4RecorderX;

public class StageView extends SurfaceView implements SurfaceHolder.Callback {

    public volatile Bitmap stageBitmap;
    public Canvas stageBitmapCanvas;

    public StageView(Context context) {
        super(context);
        init();
    }

    public StageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public StageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void init() {
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        stageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        stageBitmapCanvas = new Canvas(stageBitmap);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void setRecordX(Mp4RecorderX imagerecord) {
        imagerecord.mStageView = this;
    }
}
