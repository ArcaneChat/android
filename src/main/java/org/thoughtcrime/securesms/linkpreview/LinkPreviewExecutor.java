package org.thoughtcrime.securesms.linkpreview;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Thread pool executor for link preview fetching operations.
 * Uses a fixed thread pool to avoid creating too many threads.
 */
public class LinkPreviewExecutor {

    private static final int THREAD_POOL_SIZE = 2;
    
    private static LinkPreviewExecutor instance;
    private final ExecutorService executor;

    private LinkPreviewExecutor() {
        executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public static LinkPreviewExecutor getInstance() {
        if (instance == null) {
            synchronized (LinkPreviewExecutor.class) {
                if (instance == null) {
                    instance = new LinkPreviewExecutor();
                }
            }
        }
        return instance;
    }

    public void execute(Runnable task) {
        executor.execute(task);
    }
}
