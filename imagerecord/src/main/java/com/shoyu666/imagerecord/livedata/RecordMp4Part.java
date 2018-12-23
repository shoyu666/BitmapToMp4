package com.shoyu666.imagerecord.livedata;

import java.io.Serializable;

public class RecordMp4Part implements Serializable {
    public long width;
    public long height;
    public long durationMs;
    public String path;
}
