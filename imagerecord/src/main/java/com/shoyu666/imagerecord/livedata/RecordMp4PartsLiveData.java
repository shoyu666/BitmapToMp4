package com.shoyu666.imagerecord.livedata;

import com.shoyu666.imagerecord.log.MLog;

import java.util.ArrayList;

import androidx.lifecycle.MutableLiveData;

public class RecordMp4PartsLiveData extends MutableLiveData<ArrayList<RecordMp4Part>> {
    public static final String TAG = "RecordMp4PartsLiveData";

    public synchronized void setRecordMp4Part(RecordMp4Part recordMp4Part) {
        ArrayList<RecordMp4Part> mp4Parts = getValue();
        if (mp4Parts == null) {
            mp4Parts = new ArrayList<>();
        }
        mp4Parts.add(recordMp4Part);
        setValue(mp4Parts);
        MLog.d(TAG, "postRecordMp4Part");
    }

    public long getStoredDuration() {
        long duration = 0;
        ArrayList<RecordMp4Part> mp4Parts = getValue();
        if (mp4Parts != null && mp4Parts.size() > 0) {
            for (RecordMp4Part part : mp4Parts) {
                duration = duration + part.durationMs;
            }
        }
        return duration;
    }
}
