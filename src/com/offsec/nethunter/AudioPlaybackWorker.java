package com.offsec.nethunter;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.PowerManager.WakeLock;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import android.util.Log;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.offsec.nethunter.exception.AudioStoppedException;

public class AudioPlaybackWorker implements Runnable {
    /** 每秒尝试接收的次数.  */
    private static final int LOOPS_PER_SECOND = 8;
    private final String host;
    private final int port;
    private final WakeLock wakeLock;
    private final Handler handler;
    private final Listener listener;
    private volatile boolean stopped = false;
    private volatile long headroomUsec = 125;
    private volatile long latencyUsec = 1000;
    private boolean waitingForBufferFill = true;
    private Throwable error;
    private Socket sock;
    private InputStream audioData;
    private AudioTrack audioTrack;
    private byte[] audioBuffer;
    private int numSkip;
    private int bufferPos;
    private int chunkSize;
    private int byteRate;

    AudioPlaybackWorker(String host, int port, WakeLock wakeLock, Handler handler, Listener listener) {
        this.host = host;
        this.port = port;
        this.wakeLock = wakeLock;
        this.handler = handler;
        this.listener = listener;
    }

    @MainThread
    public void stop() {
        synchronized (this) {
            stopped = true;
            Socket s = sock;
            if (s != null) {
                // 关闭我们的套接字以强制停止长时间的 read(). 
                try {
                    // 注意: 希望这不是一个“I/O 操作”, 因为我们在主线程上运行它. 在 Android 7.1.2 上似乎工作正常. 
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Throwable getError() {
        return error;
    }

    public void run() {
        try {
            setup();

            boolean didBuffer = false;
            boolean started = false;
            while (!stopped) {
                wakeLock.acquire(1000);
                manageBufferSize();

                if (!waitingForBufferFill) {
                    readFromSocket();

                    writeToAudioTrack();

                    if (!started) {
                        started = true;
                        handler.post(() -> listener.onPlaybackStarted(this));
                    }
                } else {
                    if (!didBuffer) {
                        didBuffer = true;
                        handler.post(() -> listener.onPlaybackBuffering(this));
                    }
                    // 在正常情况下, 大约睡眠此循环运行的时间. 
                    //noinspection BusyWait
                    Thread.sleep(1000 / LOOPS_PER_SECOND);
                }
            }

            handler.post(() -> listener.onPlaybackStopped(this));
        } catch (AudioStoppedException e) {
            handler.post(() -> listener.onPlaybackStopped(this));
        } catch (Exception e) {
            // 抑制由 stop() 关闭我们的套接字引起的异常. 
            if (stopped && e instanceof SocketException) {
                handler.post(() -> listener.onPlaybackStopped(this));
            } else {
                Log.e(AudioPlaybackWorker.class.getSimpleName(), "stopWithError: " + e.getMessage(), e);
                error = e;
                handler.post(() -> listener.onPlaybackError(this, e));
            }
        } finally {
            cleanup();
        }
    }

    private void setup() throws IOException {
        final int sampleRate = 48000;
        // 每秒字节数 = 采样率 * 每个样本 2 字节 * 2 个声道
        byteRate = sampleRate * 2 * 2;

        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);

        chunkSize = byteRate / LOOPS_PER_SECOND;

        Log.i("AudioPlaybackWorker", "setup: minBufferSize=" + minBufferSize
                + " (" + (minBufferSize / (double) byteRate) + "us) chunkSize=" + chunkSize);

        connect();

        audioData = sock.getInputStream();

        // 始终使用最小缓冲区大小以获得最小延迟. 
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, minBufferSize,
                AudioTrack.MODE_STREAM);
        if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            throw new IllegalStateException(
                    "无法初始化 AudioTrack. "
                            + " state == " + audioTrack.getState());
        }
        audioTrack.play();

        bufferPos = 0;
        numSkip = 0;
        audioBuffer = new byte[chunkSize];
    }

    private void manageBufferSize() throws IOException {
        long latencyUsec = this.latencyUsec;

        // latencyUsec < 0 表示无限缓冲区: 从不跳过任何内容
        if (latencyUsec >= 0) {
            final long bufUsecTotal = headroomUsec + latencyUsec;
            final long latencyBytes = byteRate * latencyUsec / 1000000;
            final int bufferSize = (int) (byteRate * bufUsecTotal / 1000000);

            final int available = audioData.available();

            if (available > latencyBytes) {
                waitingForBufferFill = false;
            }

            if (available > bufferSize) {
                // 超出头部空间——向前跳过以免落后. 
                // 保持最新的 latencyUsec. 
                final long wantSkip = numSkip + available - latencyBytes;
                final long actual = audioData.skip(wantSkip);
                // 如果我们碰巧跳过了部分样本对, 我们需要在写入 audioTrack 时跳过剩余的字节. 
                final int malign = (int) ((bufferPos + actual) & 3L);
                if (malign != 0) {
                    numSkip = 4 - malign;
                } else {
                    numSkip = 0;
                }
                Log.d("Worker", "已跳过: wantSkip=" + wantSkip + " actual=" + actual + " numSkip=" + numSkip + " bufferPos=" + bufferPos);
                bufferPos = 0;
            }
        } else {
            // 不要等待无限缓冲区. 
            waitingForBufferFill = false;
        }
    }

    private void readFromSocket() throws IOException {
        // 永远不要尝试向 audioTrack 写入超过 chunkSize 的数据, 以免被阻塞超过必要时间. 
        int wantRead = chunkSize - bufferPos;

        int nRead = audioData.read(audioBuffer, bufferPos, wantRead);
        if (nRead < 0) {
            throw new EOFException("连接已关闭");
        }
        bufferPos += nRead;
    }

    private void writeToAudioTrack() throws IOException {
        int writeStart = numSkip;
        // [& ~3]: 仅尝试写入完整的样本对. 
        int wantWrite = (bufferPos - numSkip) & ~3;

        int sizeWrite = 0;
        if (wantWrite > 0) {
            sizeWrite = audioTrack.write(audioBuffer, writeStart, wantWrite);
        }

        if (sizeWrite < 0) {
            throw new IOException("audioTrack.write() 返回 " + sizeWrite);
        } else {
            if (sizeWrite > 0) {
                // 将剩余数据移动到缓冲区的开头. 
                int writeEnd = writeStart + sizeWrite;
                int len = bufferPos - writeEnd;
                System.arraycopy(audioBuffer, writeEnd, audioBuffer, 0, len);
                bufferPos = len;
                numSkip = 0;
            }
        }
    }

    private void cleanup() {
        audioBuffer = null;
        if (audioData != null) {
            try {
                audioData.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            audioData = null;
        }
        if (sock != null) {
            try {
                sock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            sock = null;
        }
        if (audioTrack != null) {
            // 如果我们在停止状态下调用 stop(), AudioTrack 会抛出异常. 如果 audioTrack.play() 失败, 就会发生这种情况. 
            if (audioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                audioTrack.stop();
            }
            audioTrack.release();
            audioTrack = null;
        }
    }

    /**
     * 创建 {@code sock}, 并将其连接到我们的 {@code host} 和 {@code port}. 
     * <p>
     * 其行为类似于 {@link Socket Socket(String, int)} 构造函数, 但允许引用套接字, 并可以通过 {@link #stop()} 中断. 
     *
     * @throws IOException 如果连接到主机失败. 
     * @throws AudioStoppedException 如果设置了 {@link #stopped}. 
     */
    private void connect() throws IOException {

        // 我们可能会在这里挂起以解析主机名. 目前无法中断此操作. 
        InetAddress[] addresses = InetAddress.getAllByName(host);
        if (addresses.length == 0) {
            throw new UnknownHostException("InetAddress.getAllByName() 未返回任何地址");
        }

        for (int i = 0; i < addresses.length; i++) {
            InetAddress address = addresses[i];
            try {
                synchronized (this) {
                    sock = null;
                    if (stopped) {
                        throw new AudioStoppedException();
                    }
                    sock = new Socket();
                }
                sock.setPerformancePreferences(0, 1, 0);
                sock.connect(new InetSocketAddress(address, port));

                // 我们现在已连接. 
                return;
            } catch (IOException connException) {
                try {
                    sock.close();
                } catch (IOException e) {
                    connException.addSuppressed(e);
                }

                // 仅在最后一个地址时抛出. 
                if (i == addresses.length - 1) {
                    throw connException;
                }
            }
        }

        throw new AssertionError("不应发生");
    }

    public void setBufferUsec(long headroomUsec, long latencyUsec) {
        this.headroomUsec = headroomUsec;
        this.latencyUsec = latencyUsec;
    }

    public interface Listener {
        @MainThread
        void onPlaybackError(@NonNull AudioPlaybackWorker worker, @NonNull Throwable t);

        @MainThread
        void onPlaybackBuffering(@NonNull AudioPlaybackWorker worker);

        @MainThread
        void onPlaybackStarted(@NonNull AudioPlaybackWorker worker);

        @MainThread
        void onPlaybackStopped(@NonNull AudioPlaybackWorker worker);
    }
}