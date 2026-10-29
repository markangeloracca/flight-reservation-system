package ca.pacific.frs.app;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/** OS file lock so only one copy of the application uses the database at a time. */
public final class SingleInstanceLock implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;

    private SingleInstanceLock(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    /** Returns the lock, or empty if another instance already holds it. */
    public static Optional<SingleInstanceLock> tryAcquire(Path lockFile) {
        try {
            FileChannel ch = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock lock;
            try {
                lock = ch.tryLock();
            } catch (OverlappingFileLockException e) {
                lock = null;
            }
            if (lock == null) {
                ch.close();
                return Optional.empty();
            }
            return Optional.of(new SingleInstanceLock(ch, lock));
        } catch (IOException e) {
            // If locking is unsupported on this file system, run without it rather than refusing to start.
            return Optional.of(new SingleInstanceLock(null, null));
        }
    }

    @Override
    public void close() {
        try {
            if (lock != null) {
                lock.release();
            }
            if (channel != null) {
                channel.close();
            }
        } catch (IOException ignored) {
            // exiting anyway
        }
    }
}
