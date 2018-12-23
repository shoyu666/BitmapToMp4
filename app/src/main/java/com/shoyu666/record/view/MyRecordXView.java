package com.shoyu666.record.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.shoyu666.imagerecord.event.Mp4RecorderXEvent;
import com.shoyu666.record.MyMp4Record;
import com.shoyu666.record.R;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MyRecordXView extends LinearLayout implements View.OnClickListener {

    public TextView duration;

    public View start;
    public View pause;

    public MyRecordXView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public MyRecordXView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, null);
    }

    public MyRecordXView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, null);
    }

    public MyRecordXView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, null);
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    public void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        View ui = LayoutInflater.from(context).inflate(R.layout.test_contoller, this, true);
        duration = findViewById(R.id.duration);
        start = findViewById(R.id.RecordXView_StartButton);
        start.setOnClickListener(this);
        start.setVisibility(VISIBLE);
        pause = findViewById(R.id.RecordXView_PauseButton);
        pause.setOnClickListener(this);
        pause.setVisibility(GONE);
    }

    public MyMp4Record model;

    public void setMyMp4RecordViewModel(MyMp4Record model) {
        this.model = model;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.RecordXView_StartButton) {
            if (model.startRecord()) {
                start.setVisibility(GONE);
                pause.setVisibility(VISIBLE);
            }
        }
        if (v.getId() == R.id.RecordXView_PauseButton) {
            if (model.stopRecord()) {
                start.setVisibility(VISIBLE);
                pause.setVisibility(GONE);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMp4RecorderXEvent(Mp4RecorderXEvent event) {
        switch (event.eventId) {
            case Mp4RecorderXEvent.After_Stop:
                start.setVisibility(VISIBLE);
                pause.setVisibility(GONE);
                break;
            case Mp4RecorderXEvent.Muxer_Video_PresentationTimeUs:
                break;
            case Mp4RecorderXEvent.Error:
                break;
        }
    }
}
