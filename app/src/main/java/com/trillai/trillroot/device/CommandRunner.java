package com.trillai.trillroot.device;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Bounded, deadline-limited execution of fixed argument vectors. No shell interpolation. */
public final class CommandRunner implements Closeable {
    public static final class Result {
        public final boolean completed;
        public final boolean timedOut;
        public final int exitCode;
        public final String output;

        Result(boolean completed, boolean timedOut, int exitCode, String output) {
            this.completed = completed;
            this.timedOut = timedOut;
            this.exitCode = exitCode;
            this.output = output;
        }
        static Result failure(boolean timeout) { return new Result(false, timeout, -1, ""); }
    }

    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "TrillRoot-deadline"); t.setDaemon(true); return t;
    });
    private final ExecutorService readers = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "TrillRoot-output"); t.setDaemon(true); return t;
    });
    private final Set<Process> active = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private volatile boolean closed;

    public Result run(List<String> arguments, long timeoutMillis, int maxOutputBytes) {
        if (closed || arguments.isEmpty() || timeoutMillis < 1 || maxOutputBytes < 1) {
            return Result.failure(false);
        }
        Process process = null;
        ScheduledFuture<?> deadline = null;
        Future<String> output = null;
        AtomicBoolean expired = new AtomicBoolean(false);
        try {
            ProcessBuilder builder = new ProcessBuilder(arguments);
            builder.redirectErrorStream(true);
            process = builder.start();
            active.add(process);
            final Process running = process;
            try { OutputStream stdin = running.getOutputStream(); if (stdin != null) stdin.close(); }
            catch (IOException ignored) { }
            deadline = timer.schedule(() -> {
                expired.set(true);
                running.destroy();
            }, timeoutMillis, TimeUnit.MILLISECONDS);
            final InputStream stdout = process.getInputStream();
            output = readers.submit(() -> readBounded(stdout, maxOutputBytes));
            int exit = process.waitFor();
            String text = output.get(350, TimeUnit.MILLISECONDS);
            if (expired.get() || closed) return Result.failure(expired.get());
            return new Result(true, false, exit, text);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.failure(expired.get());
        } catch (Exception e) {
            return Result.failure(expired.get());
        } finally {
            if (deadline != null) deadline.cancel(false);
            if (output != null) output.cancel(true);
            if (process != null) {
                active.remove(process);
                process.destroy();
                closeQuietly(process.getInputStream());
                closeQuietly(process.getErrorStream());
                closeQuietly(process.getOutputStream());
            }
        }
    }

    private static String readBounded(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(Math.min(limit, 1024));
        byte[] buffer = new byte[1024];
        try {
            int count;
            while ((count = input.read(buffer)) != -1) {
                int keep = Math.min(count, Math.max(0, limit - bytes.size()));
                if (keep > 0) bytes.write(buffer, 0, keep);
                // Drain excess without retaining it, so a child cannot exhaust app memory.
            }
        } finally {
            closeQuietly(input);
        }
        return new String(bytes.toByteArray(), "UTF-8");
    }

    private static void closeQuietly(Closeable c) {
        if (c != null) try { c.close(); } catch (IOException ignored) { }
    }

    @Override public void close() {
        closed = true;
        timer.shutdownNow();
        readers.shutdownNow();
        for (Process process : active) process.destroy();
        active.clear();
    }
}
