package com.lasalvavida.droingle;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Record raw audio bytes from the hardware and write them to a stream
 *
 * @author Rob Taglang
 */
public class MediaStreamRecorder extends Thread {
    private AudioRecord record;
    private byte[] buffer;
    private AtomicBoolean killSwitch = new AtomicBoolean(false);
    private OutputStream stream;

    public MediaStreamRecorder(int audioSource, int sampleRate, int channelConfig, int audioFormat, OutputStream stream) {
        this.stream = stream;
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        record = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize);
        buffer = new byte[bufferSize];
        record.startRecording();
    }

    public MediaStreamRecorder(int sampleRate, int channelConfig, int audioFormat, OutputStream stream) {
        this(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, stream);
    }

    public MediaStreamRecorder(OutputStream stream) throws IOException {
        this(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, stream);
    }

    @Override
    public void run() {
        int len;
        while(!killSwitch.get()) {
            while((len = record.read(buffer, 0, buffer.length)) > 0) {
                try {
                    stream.write(Arrays.copyOfRange(buffer, 0, len));
                } catch(IOException e) {
                    System.err.println("Couldn't write bytes across socket: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public void close() throws IOException {
        killSwitch.set(true);
        record.stop();
        record.release();
        stream.close();
    }
}
