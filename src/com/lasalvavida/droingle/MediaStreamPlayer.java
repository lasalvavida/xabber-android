package com.lasalvavida.droingle;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Plays raw audio data from a stream
 *
 * @author Rob Taglang
 */
public class MediaStreamPlayer extends Thread {
    private InputStream stream;
    private AudioTrack track;
    private byte[] buffer;
    private AtomicBoolean killSwitch = new AtomicBoolean(false);

    public MediaStreamPlayer(int streamType, int sampleRate, int channelConfig, int audioFormat, int mode, InputStream stream) throws IOException {
        this.stream = stream;
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        track = new AudioTrack(streamType, sampleRate, channelConfig, audioFormat, bufferSize, mode);
        track.play();
        buffer = new byte[bufferSize];
    }

    public MediaStreamPlayer(InputStream stream) throws IOException{
        this(AudioManager.STREAM_VOICE_CALL, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, AudioTrack.MODE_STREAM, stream);
    }

    @Override
    public void run() {
        int len;
        while(!killSwitch.get()) {
            try {
                while((len = stream.read(buffer)) > 0) {
                    play(buffer, 0, len);
                }
            } catch(IOException e) {
                System.err.println("Unable to read bytes from socket");
                e.printStackTrace();
            }
        }
    }

    public void play(byte[] buffer, int offset, int length) {
        track.write(buffer, offset, length);
    }

    public void close() throws IOException {
        killSwitch.set(true);
        track.stop();
        track.release();
    }
}
