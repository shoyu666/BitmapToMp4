package com.shoyu666.record.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.shoyu666.imagerecord.R;
import com.shoyu666.imagerecord.core.Mp4RecorderX;

public class RecordXView extends FrameLayout implements View.OnClickListener {

    public View start;
    public View pause;

    public Mp4RecorderX recordX;
    public static final long Delay = 1000;
    public long lastAciton = 0;

    public void setRecordX(Mp4RecorderX recordX) {
        this.recordX = recordX;
    }

    public RecordXView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public RecordXView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RecordXView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public RecordXView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.RecordXView);
            int resourceId = typedArray.getResourceId(R.styleable.RecordXView_controller, -1);
            LayoutInflater.from(context).inflate(resourceId, this, true);
            start = findViewById(R.id.RecordXView_StartButton);
            start.setOnClickListener(this);
            start.setVisibility(VISIBLE);
            pause = findViewById(R.id.RecordXView_PauseButton);
            pause.setOnClickListener(this);
            pause.setVisibility(GONE);
        }
    }

    @Override
    public void onClick(View v) {
        long current = System.currentTimeMillis();
        if (current - lastAciton > Delay) {
            lastAciton = current;
            if (v.getId() == R.id.RecordXView_StartButton) {
                start.setVisibility(GONE);
                pause.setVisibility(VISIBLE);
                recordX.startRecord();
            }
            if (v.getId() == R.id.RecordXView_PauseButton) {
                start.setVisibility(VISIBLE);
                pause.setVisibility(GONE);
                recordX.stopRecord();
            }
        }
    }
}
