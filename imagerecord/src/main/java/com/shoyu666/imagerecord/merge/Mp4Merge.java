package com.shoyu666.imagerecord.merge;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.text.TextUtils;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.shoyu666.imagerecord.livedata.RecordMp4Part;
import com.shoyu666.imagerecord.log.MLog;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Mp4Merge {

    public static void doMerge(File mergeResult, ArrayList<RecordMp4Part> parts) throws IOException {
        List<Movie> inMovies = new ArrayList<>();
        for (RecordMp4Part part : parts) {
            inMovies.add(MovieCreator.build(part.path));
        }
        List<Track> videoTracks = new LinkedList<>();
        List<Track> audioTracks = new LinkedList<>();
        for (Movie m : inMovies) {
            for (Track t : m.getTracks()) {
                if (t.getHandler().equals("soun")) {
                    audioTracks.add(t);
                }
                if (t.getHandler().equals("vide")) {
                    videoTracks.add(t);
                }
            }
        }
        Movie result = new Movie();
        if (!audioTracks.isEmpty()) {
            result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
        }
        if (!videoTracks.isEmpty()) {
            result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
        }
        Container out = new DefaultMp4Builder().build(result);
        FileChannel fc = null;
        try {
            fc = new RandomAccessFile(mergeResult, "rw").getChannel();
            out.writeContainer(fc);
        } catch (IOException e) {
            MLog.reportThrowable(e);
        } finally {
            try {
                if (fc != null) {
                    fc.close();
                }
            } catch (IOException e) {
                MLog.reportThrowable(e);
            }
        }
    }

    public static long getMergeMp4Duration(Context context, File mergeResult) {
        long duration = 0;
        if (mergeResult != null && mergeResult.exists() && mergeResult.canRead()) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(context, Uri.fromFile(mergeResult));
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            duration = !TextUtils.isEmpty(time) ? Long.valueOf(time) : 0;
        }
        return duration;
    }
}
