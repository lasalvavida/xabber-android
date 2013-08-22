package com.lasalvavida.droingle;

import android.media.AudioFormat;

import java.io.IOException;
import java.lang.String;
import java.net.*;
import java.security.InvalidParameterException;
import java.util.*;

/**
 * Create and manage Jingle sessions
 *
 * @author Rob Taglang
 */
public class Jingle {

    public final static Set<Payload> SUPPORTED_CODECS = new HashSet<Payload>();

    private Codec codec;
    private int clockRate;
    private int channels = 1;
    private InetAddress ip;
    private int port;
    private Protocol protocol;
    private String user;

    private MediaStreamRecorder recorder;
    private MediaStreamReceiver receiver;

    private HashSet<StreamReceivedListener> streamReceivedListeners = new HashSet<StreamReceivedListener>();

    static {
        SUPPORTED_CODECS.add(new Payload(1, Codec.L16, 44100));
    }

    public Jingle(Codec codec, int clockRate, int channels, InetAddress ip, int port, Protocol protocol) {
        this.codec = codec;
        this.clockRate = clockRate;
        this.channels = channels;
        this.ip = ip;
        this.port = port;
        this.protocol = protocol;
    }

    public Jingle(String codec, int clockRate, int channels, InetAddress ip, int port, String protocol) {
        this(Codec.fromString(codec), clockRate, channels, ip, port, Protocol.fromString(protocol));
    }

    public Jingle(String codec, int clockRate, int channels, String ip, int port, String protocol) throws UnknownHostException {
        this(codec, clockRate, channels, InetAddress.getByName(ip), port, protocol);
    }

    public void initiate() throws IOException {
        int channelConfig = -1;
        if(channels == 1) {
            channelConfig = AudioFormat.CHANNEL_IN_MONO;
        }
        else if(channels == 2) {
            channelConfig = AudioFormat.CHANNEL_IN_STEREO;
        }
        if(protocol == Protocol.TCP) {
            System.out.println("Started recorder!");
            Socket socket = new Socket(ip, port);
            recorder = new MediaStreamRecorder(clockRate, channelConfig, codec.toAndroid(), socket.getOutputStream());
            recorder.start();
        } else if(protocol == Protocol.UDP) {
            //TODO: implement UDP
        }
        else {
            throw new InvalidParameterException();
        }
    }

    public void listen(int port, Protocol p) throws IOException {
        if(protocol == Protocol.TCP) {
            receiver = new MediaStreamReceiver(this, port);
            receiver.start();
        }
    }

    public void addStreamReceivedListener(StreamReceivedListener listener) {
        if(receiver != null) {
            streamReceivedListeners.add(listener);
        }
    }

    public Set<StreamReceivedListener> getStreamReceivedListeners() {
        return streamReceivedListeners;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getUser() {
        return user;
    }

    public void close() throws IOException {
        if(recorder != null) {
            recorder.close();
        }
        if(receiver != null) {
            receiver.close();
        }
    }

    public static boolean isCodecSupported(Codec codec) {
        for(Payload p : SUPPORTED_CODECS) {
            if(p.getCodec() == codec) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCodecSupported(String codec) {
        return isCodecSupported(Codec.fromString(codec));
    }

    /**
     * @return xml stanzas describing the supported payloads
     */
    public static String supportedCodecs() {
        String build = "";
        for(Payload payload : SUPPORTED_CODECS) {
            build += payload.toString();
        }
        return build;
    }

    public static InetAddress getBindAddress(String inf) {
        List<InetAddress> all = filterForIPV4(getAddressesForInterface(inf));
        if(all != null && all.size() > 0) {
            return all.get(0);
        }
        return null;
    }

    /**
     * Retrieves available IP addresses from network hardware
     * @return
     */
    private static List<InetAddress> getBindAddresses() {
        List<InetAddress> addresses = new ArrayList<InetAddress>();
        for(NetworkInterface network : getNetworkInterfaces()) {
            for(InetAddress address : Collections.list(network.getInetAddresses())) {
                //as of right now, xop only supports IPv4
                if(address instanceof Inet4Address) {
                    addresses.add(address);
                }
            }
        }
        return addresses;
    }

    private static List<InetAddress> filterForIPV4(List<InetAddress> addresses) {
        if(addresses != null) {
            List<InetAddress> filtered = new ArrayList<InetAddress>();
            for(InetAddress address : addresses) {
                if(address instanceof Inet4Address) {
                    filtered.add(address);
                }
            }
            return filtered;
        }
        return null;
    }

    private static List<InetAddress> getAddressesForInterface(String net) {
        try {
            if(net != null && NetworkInterface.getByName(net) != null) {
                return Collections.list(NetworkInterface.getByName(net).getInetAddresses());
            }
        } catch (SocketException e) {
            System.err.println("Couldn't get addresses for interface: " + e.getMessage());
        }
        return null;
    }

    private static List<InetAddress> getAddressesForInterface(NetworkInterface net) {
        if(net != null) {
            return Collections.list(net.getInetAddresses());
        }
        return null;
    }

    private static List<NetworkInterface> getNetworkInterfaces() {
        Enumeration<NetworkInterface> interfaces = null;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
                System.err.println("Couldn't get network interfaces: " + e.getMessage());
        }
        return Collections.list(interfaces);
    }

    public enum Protocol {
        UDP,
        TCP;

        public static Protocol fromString(String name) {
            for(Protocol p : values()) {
                if(p.toString().equals(name)) {
                    return p;
                }
            }
            return null;
        }
    }
}
