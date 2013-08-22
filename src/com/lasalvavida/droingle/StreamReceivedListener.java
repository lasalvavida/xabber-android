package com.lasalvavida.droingle;

/**
 * Callbacks from MediaStreamReceiver
 *
 * @author Rob Taglang
 */
public interface StreamReceivedListener {
    public void streamReceived(Jingle jingle);
}
