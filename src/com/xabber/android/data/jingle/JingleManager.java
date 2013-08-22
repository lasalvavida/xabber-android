package com.xabber.android.data.jingle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.ui.ContactList;
import com.xabber.android.ui.JingleViewer;
import com.lasalvavida.droingle.Jingle;
import com.lasalvavida.droingle.Payload;
import com.lasalvavida.droingle.StreamReceivedListener;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

/**
 * Handles Jingle IQ messages and establishes Jingle connections
 * Current implementation uses Droingle
 *
 * @author Rob Taglang
 */
public class JingleManager implements IQProvider, StreamReceivedListener {
    private static boolean request = false;
    private HashMap<String, Jingle> sessions = new HashMap<String, Jingle>();
    private static int port = 8998;
    private static Jingle.Protocol protocol = Jingle.Protocol.TCP;
    private String account;
    private boolean active;
    private static char[] allowedChars = {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','1','2','3','4','5','6','7','8','9','0'};
    private static Random random = new Random();
    private Activity activity;

    public JingleManager(String account) {
        this.account = account;
        ProviderManager.getInstance().addIQProvider("jingle", "urn:xmpp:jingle:1", this);
    }

    @Override
    public IQ parseIQ(XmlPullParser parser) throws Exception {
        System.err.println("Parsing xml!");
        Jingle jingle = null;
        Payload payload = null;
        String from = null;
        String session = null;
        int event = parser.getEventType();
        while(event != XmlPullParser.END_DOCUMENT) {
            if(event == XmlPullParser.START_TAG) {
                if(from == null && parser.getName().equals("jingle")) {
                    boolean close = false;
                    for(int i=0; i<parser.getAttributeCount(); i++) {
                        if(parser.getAttributeName(i).equals("initiator") || parser.getAttributeName(i).equals("responder")) {
                            from = parser.getAttributeValue(i);
                        }
                        else if(parser.getAttributeName(i).equals("action") && parser.getAttributeValue(i).equals("session-terminate")) {
                            System.err.println("Closing on my end as well!");
                            close = true;
                        }
                        else if(parser.getAttributeName(i).equals("sid")) {
                            session = parser.getAttributeValue(i);
                        }
                    }
                    if(close) {
                        closeSession(session);
                        break;
                    }
                }
                if(payload == null && parser.getName().equals("payload-type")) {
                    String id = null, clockrate = null, name = null;
                    for(int i=0; i<parser.getAttributeCount(); i++) {
                        if(parser.getAttributeName(i).equals("id")) {
                            id = parser.getAttributeValue(i);
                        }
                        else if(parser.getAttributeName(i).equals("name")) {
                            name = parser.getAttributeValue(i);
                        }
                        else if(parser.getAttributeName(i).equals("clockrate")) {
                            clockrate = parser.getAttributeValue(i);
                        }
                    }
                    if(name != null) {
                        if(Jingle.isCodecSupported(name)) {
                            if(clockrate == null) {
                                clockrate = "44100";
                            }
                            if(id == null) {
                                id = "0";
                            }
                            payload = new Payload(id, name, clockrate);
                        }
                    }
                }
                if(payload != null && parser.getName().equals("candidate")) {
                    String ip = null, port = null, protocol = null;
                    for(int i=0; i<parser.getAttributeCount(); i++) {
                        if(parser.getAttributeName(i).equals("ip")) {
                            ip = parser.getAttributeValue(i);
                        }
                        else if(parser.getAttributeName(i).equals("port")) {
                            port = parser.getAttributeValue(i);
                        }
                        else if(parser.getAttributeName(i).equals("protocol")) {
                            protocol = parser.getAttributeValue(i);
                        }
                    }
                    if(ip != null && port != null && protocol != null && session != null) {
                        jingle = new Jingle(payload.getCodec().toString(), payload.getClockRate(), 1, ip, Integer.parseInt(port), protocol);
                        sessions.put(session, jingle);
                        jingle.listen(this.port, this.protocol);
                        jingle.addStreamReceivedListener(this);
                        jingle.setUser(from);
                        active = true;
                        if(request) {
                            jingle.initiate();
                        }
                        else {
                            getUserInput(ContactList.getContext(), account, from, session);
                        }
                        break;
                    }
                }
            }
            event = parser.next();
        }
        if(jingle == null) {
            //some error
            return null;
        }
        else {
            IQ ret = new IQ() {
                @Override
                public String getChildElementXML() {
                    return null;
                }
            };
            ret.setType(IQ.Type.RESULT);
            return ret;
        }
    }

    @Override
    public void streamReceived(Jingle jingle) {
        if(!request) {
            try {
                jingle.initiate();
            } catch (IOException e) {
                Log.e(JingleManager.class.getName(), "Couldn't initiate jingle session: " + e.getMessage());
            }
        }
        else {
            request = false;
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setActivity(JingleViewer viewer) {
        activity = viewer;
    }

    //TODO: This needs to be more flexible, a lot of these values are just copy-pasted from xep-0167 (http://xmpp.org/extensions/xep-0167.html)
    public void sendJingleRequest(final String account, final String to) {
        request = true;
        Packet packet = new Packet() {
            @Override
            public String toXML() {
                return "<iq from='" + account + "' " +
                        "to='" + to + "' " +
                        "type='set'>" +
                        "<jingle xmlns='urn:xmpp:jingle:1' action='session-initiate' initiator='" + account + "' sid='" + genId(16) + "'>" +
                        "<content creator='initiator' name='voice'>" +
                        "<description xmlns='urn:xmpp:jingle:apps:rtp:1' media='audio'>" + Jingle.supportedCodecs() + "</description>" +
                        "<transport xmlns='urn:xmpp:jingle:transports:ice-udp:1' pwd='asd88fgpdd777uzjYhagZg' ufrag='8hhy'>" +
                        "<candidate component='1' foundation='1' generation='0' id='el0747fg11' ip='" + Jingle.getBindAddress("wlan0").getHostAddress() + "' network='1' port='" + port + "' priority='2130706431' protocol='" + protocol + "' type='host'/>" +
                        "</transport></content></jingle></iq>";
            }
        };
        try {
            ConnectionManager.getInstance().sendPacket(account, packet);
        } catch (NetworkException e) {
            Log.e(JingleManager.class.getName(), "Couldn't send packet: " + e.getMessage());
        }
    }

    public void acceptJingleRequest(final String account, final String to, final String sessionId) {
        Packet packet = new Packet() {
            @Override
            public String toXML() {
                return "<iq from='" + account + "' " +
                        "to='" + to + "' " +
                        "type='set'>" +
                        "<jingle xmlns='urn:xmpp:jingle:1' action='session-accept' responder='" + account + "' sid='" + sessionId + "'>" +
                        "<content creator='initiator' name='voice'>" +
                        "<description xmlns='urn:xmpp:jingle:apps:rtp:1' media='audio'>" + Jingle.supportedCodecs() + "</description>" +
                        "<transport xmlns='urn:xmpp:jingle:transports:ice-udp:1' pwd='asd88fgpdd777uzjYhagZg' ufrag='8hhy'>" +
                        "<candidate component='1' foundation='1' generation='0' id='el0747fg11' ip='" + Jingle.getBindAddress("wlan0").getHostAddress() + "' network='1' port='" + port + "' priority='2130706431' protocol='" + protocol + "' type='host'/>" +
                        "</transport></content></jingle></iq>";
            }
        };
        try {
            ConnectionManager.getInstance().sendPacket(account, packet);
        } catch (NetworkException e) {
            Log.e(JingleManager.class.getName(), "Couldn't send packet: " + e.getMessage());
        }
    }

    public void closeJingleMessage(final String account, final String to, final String sid) {
        Packet packet = new Packet() {
            @Override
            public String toXML() {
                return "<iq from='" + account + "' " +
                        "to='" + to + "' " +
                        "type='set'>" +
                        "<jingle xmlns='urn:xmpp:jingle:1' action='session-terminate' sid='" + sid +"'>" +
                        "<reason><success/></reason>" +
                        "</jingle></iq>";
            }
        };
        try {
            ConnectionManager.getInstance().sendPacket(account, packet);
        } catch (NetworkException e) {
            Log.e(JingleManager.class.getName(), "Couldn't send packet: " + e.getMessage());
        }
    }

    private void getUserInput(final Context context, final String account, final String from, final String sessionId) {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        final Ringtone r = RingtoneManager.getRingtone(context, notification);
        r.play();

        ((Activity)context).runOnUiThread(new Runnable() {
            public void run() {
                final AlertDialog alert = new AlertDialog.Builder(context).create();
                alert.setTitle("Incoming Call: " + from);
                alert.setMessage("Accept?");
                alert.setButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        r.stop();
                        acceptJingleRequest(account, from, sessionId);
                        context.startActivity(JingleViewer.createIntent(context, account,
                                from));
                    }
                });
                alert.setButton2("NO", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        r.stop();
                        close(from);
                        alert.dismiss();
                    }
                });
                alert.show();
            }
        });
    }

    /**
     * This is called by JingleViewer when a call is terminated. It sends a terminate message across the xmpp connection
     * @param user
     */
    public void close(String user) {
        for(String sid : sessions.keySet()) {
            Jingle jingle = sessions.get(sid);
            if(jingle != null) {
                if(toBareJid(jingle.getUser()).equals(toBareJid(user))) {
                    System.err.println("Sending close jingle message across!");
                    closeJingleMessage(account, jingle.getUser(), sid);
                    try {
                        jingle.close();
                    } catch (IOException e) {
                        Log.e(JingleManager.class.getName(), "Unable to close Jingle instance: " + e.getMessage());
                    }
                    sessions.remove(sid);
                    break;
                }
            }
        }
        if(sessions.size() == 0) {
            active = false;
        }
        request = false;
    }

    /**
     * This is called when we received a session terminate message
     * @param sid
     */
    public void closeSession(String sid) {
        Jingle jingle = sessions.remove(sid);
        try {
            if(jingle != null) {
                jingle.close();
            }
        } catch (IOException e) {
            Log.e(JingleManager.class.getName(), "Unable to close Jingle instance: " + e.getMessage());
        }
        if(activity != null) {
            activity.finish();
        }
        if(sessions.size() == 0) {
            active = false;
        }
        request = false;
    }

    public void close() {
        for(String sid : sessions.keySet()) {
            Jingle jingle = sessions.get(sid);
            if(jingle != null) {
                closeJingleMessage(account, jingle.getUser(), sid);
                try {
                    jingle.close();
                } catch (IOException e) {
                    Log.e(JingleManager.class.getName(), "Unable to close Jingle instance: " + e.getMessage());
                }
            }
        }
        sessions.clear();
        active = false;
        request = false;
    }

    public static String toBareJid(String jid) {
        String[] pieces = jid.split("/");
        if(pieces.length > 1) {
            return pieces[0];
        }
        return jid;
    }

    private static String genId(int length) {
        String build = "";
        for(int i=0; i<length; i++) {
            build += allowedChars[random.nextInt(allowedChars.length)];
        }
        return build;
    }
}
