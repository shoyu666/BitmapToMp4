package com.shoyu666.imagerecord.util;

public class TimeUtil {
    public static boolean isOver(long nano, long offset) {
        boolean isOver = (System.nanoTime() - nano) > offset;
        return isOver;
    }
}
