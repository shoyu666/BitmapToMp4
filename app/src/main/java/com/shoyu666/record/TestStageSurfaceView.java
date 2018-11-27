package com.shoyu666.record;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.SurfaceHolder;
import com.shoyu666.imagerecord.log.MLog;
import com.shoyu666.imagerecord.render.GifRender;
import com.shoyu666.imagerecord.stage.StageView;

public class TestStageSurfaceView extends StageView implements Choreographer.FrameCallback {
    public GifRender render;
    public Rect canvasRect;
    Paint mBitmapPaint;

    public TestStageSurfaceView(Context context) {
        super(context);
    }

    public TestStageSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TestStageSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TestStageSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void init() {
        super.init();
        render = new GifRender(getContext());
        render.setAssetRes(getContext().getAssets(), "20181127123806.gif");
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        super.surfaceChanged(holder, format, width, height);
        canvasRect = new Rect(0, 0, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        render.draw(stageBitmapCanvas, getMeasuredWidth(), getMeasuredHeight());
        drawView();
        Choreographer.getInstance().postFrameCallback(this);
    }

    private void drawView() {
        if (stageBitmap == null) {
            return;
        }
        Canvas canvas = getHolder().lockCanvas();
        if (canvas == null) {
            return;
        }
        try {
            canvas.drawBitmap(stageBitmap, null, canvasRect, mBitmapPaint);
        } catch (Exception e) {
            MLog.reportThrowable(e);
        }
        getHolder().unlockCanvasAndPost(canvas);
    }
}
