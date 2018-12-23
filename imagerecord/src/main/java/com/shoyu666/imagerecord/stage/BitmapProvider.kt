package com.shoyu666.imagerecord.stage

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.*
import com.shoyu666.imagerecord.BuildConfig
import com.shoyu666.imagerecord.core.BitmapFeeder
import com.shoyu666.imagerecord.log.MLog
import com.shoyu666.imagerecord.render.ISpiritRender
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.HashSet

open class BitmapProvider : SurfaceView, SurfaceHolder.Callback, Choreographer.FrameCallback {

    @Volatile
    var stageBitmap: Bitmap? = null
    lateinit var stageBitmapCanvas: Canvas
    lateinit var mBitmapPaint: Paint
    lateinit var debugPainnt: Paint
    lateinit var canvasRect: Rect
    var renders: MutableSet<ISpiritRender> = Collections.synchronizedSet(HashSet<ISpiritRender>())
    @Volatile
    var surfaceCreate: Boolean = false

    @Synchronized
    fun addRenders(vararg renders: ISpiritRender) {
        if (renders != null && renders.isNotEmpty()) {
            for (item: ISpiritRender in renders) {
                this.renders.add(item)
                if (surfaceCreate) {
                    item.surfaceCreated()
                    var rect: Rect = holder.surfaceFrame
                    if (rect != null) {
                        item.surfaceChanged(rect.width(), rect.height())
                    }
                }
            }
        }
    }

    @Synchronized
    fun removeRender(render: ISpiritRender) {
        loopRender({
            it == render
        }, { _, iterator ->
            iterator?.remove()
        })
    }

    @Synchronized
    fun cleanRenders() {
        renders.clear()
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    open fun init() {
        mBitmapPaint = Paint(Paint.DITHER_FLAG)
        mBitmapPaint.setAntiAlias(true)
        debugPainnt = Paint()
        debugPainnt.color = Color.BLACK
        debugPainnt.textSize = 30f
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceCreate = true
        loopRender { iSpiritRender, mutableIterator ->
            iSpiritRender?.surfaceCreated()
        }
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        canvasRect = Rect(0, 0, width, height)
        stageBitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
            }
        }
        stageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        stageBitmap?.setHasAlpha(false)
        stageBitmapCanvas = Canvas(stageBitmap)
        loopRender(action = { isp, _ ->
            run {
                isp?.surfaceChanged(width, height)
            }
        })
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceCreate = false
        Choreographer.getInstance().removeFrameCallback(this)
        loopRender { iSpiritRender, mutableIterator ->
            iSpiritRender?.surfaceDestroyed(stageBitmap)
        }
        stageBitmap?.let {
            if (it.isRecycled) {
                it.recycle()
            }
        }
//        renders.clear()
    }


    override fun doFrame(frameTimeNanos: Long) {
        Choreographer.getInstance().postFrameCallback(this)
        var shouldRefresh = false
        stageBitmap?.let {
            loopRender { iSpiritRender, mutableIterator ->
                var renderRefresh: Boolean = false
                try {
                    renderRefresh = (iSpiritRender?.shoudRefresh(frameTimeNanos, it.width, it.height))
                            ?: false
                } catch (e: Exception) {
                }
                if (renderRefresh) {
                    shouldRefresh = true
                }
            }
        }
        if (shouldRefresh) {
            ChoreographerEvent.post(frameTimeNanos)
        }
    }


    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onChoreographerEvent(event: ChoreographerEvent) {
        var shouldRefresh = false
        stageBitmap?.let {
            //            it.eraseColor(Color.TRANSPARENT)
            loopRender { iSpiritRender, mutableIterator ->
                var renderRefresh: Boolean = false
                try {
                    renderRefresh = (iSpiritRender?.draw(stageBitmapCanvas, it.width, it.height))
                            ?: false
                } catch (e: Exception) {
                }
                if (renderRefresh) {
                    shouldRefresh = true
                }
            }
        }
        if (shouldRefresh) {
            drawBitmapToView()
        }
    }

    fun drawBitmapToView() {
        if (stageBitmap == null || canvasRect == null) {
            return
        }
        val canvas = holder.lockCanvas() ?: return
        try {
            if (BuildConfig.DEBUG) {
                // canvas.drawColor(Color.RED)
                //canvas.drawText(  "${System.currentTimeMillis()}", 200f, 600f, debugPainnt)
            }
            canvas.drawBitmap(stageBitmap, null, canvasRect, mBitmapPaint)
        } catch (e: Exception) {
            MLog.reportThrowable(TAG, e)
        }

        holder.unlockCanvasAndPost(canvas)
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        loopRender { iSpiritRender, mutableIterator ->
            iSpiritRender?.onTouchEvent(event)
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w = View.MeasureSpec.getSize(widthMeasureSpec)
        val h = View.MeasureSpec.getSize(heightMeasureSpec)
        if (w > 0 && h > 0) {
            val widthSpec = View.MeasureSpec.makeMeasureSpec(w / 2 * 2, View.MeasureSpec.getMode(widthMeasureSpec))
            val heightSpec = View.MeasureSpec.makeMeasureSpec(h / 2 * 2, View.MeasureSpec.getMode(heightMeasureSpec))
            super.onMeasure(widthSpec, heightSpec)
            return
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }


    fun loopRender(filter: (ISpiritRender?) -> Boolean = {
        true
    }, action: (ISpiritRender?, MutableIterator<ISpiritRender>?) -> Unit) {
        val weakiterator: MutableIterator<ISpiritRender> = renders.iterator()
        while (weakiterator.hasNext()) {
            val render = weakiterator.next()
            if (filter(render)) {
                action?.let {
                    it(render, weakiterator)
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        Choreographer.getInstance().removeFrameCallback(this)
        loopRender { iSpiritRender, mutableIterator ->
            iSpiritRender?.onDetachedFromWindow()
        }
    }

    var videoFeeder: BitmapFeeder = object : BitmapFeeder() {
        override fun getOneFrame(mVideoRect: Rect, timeStampNanos: Long): Bitmap? {
            return stageBitmap
        }
    }

    companion object {
        const val TAG = "BitmapProvider";
    }
}
