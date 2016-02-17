package com.jackpf.kitchensync;

import com.jackpf.kitchensync.CInterface.CInterface;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import sun.awt.Mutex;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * Created by jackfarrelly on 26/01/2016.
 */
public class AnalyserService extends Service<Float> {
    private final File TMP_FILE = new File("/tmp/tmp.wav");

    private static final Mutex mutex = new Mutex();

    private CInterface cInterface = new CInterface();

    private Executor ffmpeg;

    private final Info trackInfo;

    public AnalyserService(Info trackInfo) throws Exception {
        ffmpeg = new Executor(
            URLDecoder.decode(ClassLoader.getSystemClassLoader().getResource("ffmpeg").getPath(), "UTF-8")
        );
        this.trackInfo = trackInfo;
    }

    @Override
    protected Task<Float> createTask() {
        return new Task<Float>() {
            @Override
            protected Float call() throws Exception {
                // Needs to be smart mutex!
                mutex.lock();

                ffmpeg.run(new String[]{"-y", "-i", trackInfo.getFile().getAbsolutePath(), TMP_FILE.getAbsolutePath()});

                if (!TMP_FILE.exists()) {
                    throw new IOException("File " + TMP_FILE.getAbsolutePath() + " does not exist");
                }

                float bpm = cInterface.getBpm(TMP_FILE.getAbsolutePath());

                TMP_FILE.delete();

                mutex.unlock();

                return bpm;
            }
        };
    }
}
