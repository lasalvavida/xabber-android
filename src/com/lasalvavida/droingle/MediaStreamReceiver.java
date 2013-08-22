package com.lasalvavida.droingle;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Spins off MediaStreamPlayers from a ServerSocket
 *
 * @author Rob Taglang
 */
public class MediaStreamReceiver extends Thread {
    private ServerSocket socket;
    private AtomicBoolean killSwitch = new AtomicBoolean(false);
    private HashSet<MediaStreamPlayer> players = new HashSet<MediaStreamPlayer>();
    private HashSet<Socket> sockets = new HashSet<Socket>();
    private Jingle owner;

    public MediaStreamReceiver(Jingle owner, ServerSocket socket) {
        this.socket = socket;
        this.owner = owner;
    }

    public MediaStreamReceiver(Jingle owner, int port) throws IOException {
        this(owner, new ServerSocket(port));
    }

    @Override
    public void run() {
        while(!killSwitch.get()) {
            try {
                Socket s = socket.accept();
                for(StreamReceivedListener listener : owner.getStreamReceivedListeners()) {
                    listener.streamReceived(owner);
                }
                sockets.add(s);
                MediaStreamPlayer player = new MediaStreamPlayer(s.getInputStream());
                players.add(player);
                player.start();
            } catch (IOException e) {
                System.err.println("Couldn't accept socket connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void close() throws IOException {
        killSwitch.set(true);
        socket.close();
        for(Socket s : sockets) {
            s.close();
        }
        sockets.clear();
        for(MediaStreamPlayer p : players) {
            try {
                p.close();
            } catch (IOException e) {
                System.err.println("Unable to close player: " + p);
            }
        }
        players.clear();
    }
}
