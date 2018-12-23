package com.shoyu666.record;

import android.os.Environment;
import com.shoyu666.imagerecord.event.Mp4RecorderXEvent;
import com.shoyu666.imagerecord.model.Mp4RecorderXLifecycleViewModel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyMp4Record extends Mp4RecorderXLifecycleViewModel {
    @Override
    public void notifyMp4RecorderXEvent(Mp4RecorderXEvent event) {

    }

    @Override
    public int getVideoHeight() {
        return 720;
    }

    @Override
    public int getVideoWidth() {
        return 1080;
    }

    @Override
    public File getMp4File() {
        SimpleDateFormat HMM = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
        Date date = new Date(System.currentTimeMillis());
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), HMM.format(date) + ".mp4");
        return file;
    }
}
