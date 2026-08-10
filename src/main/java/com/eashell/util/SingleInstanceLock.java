package com.eashell.util;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;

/**
 * Prevents two copies of EAShell running at once - both would load, edit and save
 * eashell_data.json independently, so whichever saves last silently wins and the other's
 * changes are lost. Trivial to hit once there's a Start Menu shortcut to double-click.
 */
public class SingleInstanceLock {
    private static FileChannel channel;
    private static FileLock lock;

    /** @return true if this process now holds the lock (no other instance is running) */
    public static boolean tryAcquire() {
        try {
            Files.createDirectories(Constants.DATA_DIR);
            channel = new RandomAccessFile(Constants.LOCK_FILE.toFile(), "rw").getChannel();
            lock = channel.tryLock();
            return lock != null;
        } catch (IOException e) {
            // An unrelated I/O failure here shouldn't block the app from starting at all.
            return true;
        }
    }

    public static void release() {
        try {
            if (lock != null) {
                lock.release();
            }
            if (channel != null) {
                channel.close();
            }
        } catch (IOException ignored) {
        }
    }

    private SingleInstanceLock() {}
}
