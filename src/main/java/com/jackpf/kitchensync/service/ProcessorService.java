package com.jackpf.kitchensync.service;

import com.jackpf.kitchensync.CInterface.CInterface;
import com.jackpf.kitchensync.Executor;
import com.jackpf.kitchensync.TagWriter;
import com.jackpf.kitchensync.entity.Info;
import com.jackpf.kitchensync.executor.FFMPEGExecutor;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import sun.awt.Mutex;

import java.io.File;

/**
 * Created by jackfarrelly on 26/01/2016.
 */
public class ProcessorService extends Service<Info> {
    private static final Mutex mutex = new Mutex();

    private CInterface cInterface = new CInterface();

    private Executor ffmpeg;

    private final Info trackInfo;

    private final float targetBpm;

    private final File outputDir;

    public ProcessorService(Info trackInfo, float targetBpm, File outputDir) throws Exception {
        this.trackInfo = trackInfo;
        this.targetBpm = targetBpm;
        this.outputDir = outputDir;

        ffmpeg = new FFMPEGExecutor();
    }

    @Override
    protected Task<Info> createTask() {
        return new Task<Info>() {
            @Override
            protected Info call() throws Exception {
                mutex.lock();

                try {
                    String filename;

                    if (cInterface.hasDecoderFor(trackInfo.getFile().getAbsolutePath())) {
                        filename = trackInfo.getFile().getAbsolutePath();
                    } else {
                        ffmpeg.run(new String[]{"-i", trackInfo.getFile().getAbsolutePath(), trackInfo.getTmpFile().getAbsolutePath()});
                        filename = trackInfo.getTmpFile().getAbsolutePath();
                    }

                    cInterface.setBpm(
                        filename,
                        trackInfo.getTmpFile2().getAbsolutePath(),
                        Float.parseFloat(trackInfo.getBpm()),
                        targetBpm
                    );

                    TagWriter.Tags tags = new TagWriter(trackInfo).getTags();

                    ffmpeg.run(new String[]{
                        "-i",
                        trackInfo.getTmpFile2().getAbsolutePath(),
                        "-ab", "320k",
                        "-ac", "2",
                        "-metadata", "title=" + tags.name + "",
                        "-metadata", "artist=" + tags.artist + "",
                        outputDir + "/" + trackInfo.getFile().getName()
                    });

                    return trackInfo;
                } finally {
                    mutex.unlock();
                }
            }
        };
    }
}
