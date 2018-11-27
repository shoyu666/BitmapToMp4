package com.shoyu666.record;

import android.os.Environment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.shoyu666.imagerecord.core.Mp4RecorderX;

import java.io.File;

public class AudioRecordXViewModel extends ViewModel {
    MutableLiveData statusLiveData = new MutableLiveData<Integer>();

    public void startRecord() {
        imagerecord.startRecord();
    }

    File outputFile = null;

    public void pauseRecord() {
        imagerecord.startRecord();
    }

    public Mp4RecorderX imagerecord;

    public AudioRecordXViewModel() throws Exception {
        initViewMp4Recorder();
    }

    public void initViewMp4Recorder() {
        if (outputFile == null) {
            outputFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        }
        if (imagerecord == null) {
            imagerecord = Mp4RecorderX.create(720, 1080, new File(outputFile, System.currentTimeMillis() + ".mp4"));
        }
    }

    public void releaseRecordX() {
        imagerecord.release();
    }
}
