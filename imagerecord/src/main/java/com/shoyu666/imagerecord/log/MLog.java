package com.shoyu666.imagerecord.log;

import android.util.Log;

public class MLog {
    public static final String TAG="MLog";
    public static void reportThrowable(Exception e) {
        Log.e(TAG, e.getMessage(), e);
    }
    public static void reportThrowable(String tag,Exception e) {
        Log.e(tag, e.getMessage(), e);
    }

    public static void d(String tag, String msg) {
        Log.d(tag, msg);
    }
    public static void reportError(OutOfMemoryError msg) {
        Log.e(TAG, msg.getMessage(), msg);

    }
    public static void reportError(String tag, OutOfMemoryError msg) {
        Log.e(tag, msg.getMessage(), msg);

    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }
}
