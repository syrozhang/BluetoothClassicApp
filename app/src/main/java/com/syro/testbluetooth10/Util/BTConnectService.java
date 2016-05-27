package com.syro.testbluetooth10.Util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


/**
 * Created by Syro on 2015-12-23.
 */
public class BTConnectService {
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private final Handler handler;
    private final BluetoothAdapter bluetoothAdapter;
    //    private BluetoothDevice rmtBlueToothDvc;
    private int connectStatus;
    public static boolean chatActivityIsDestroyed;
    private String TAG = "SyroZhang";
    private static final String myServerName = "Kvering";
//    private static final UUID uuid = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
		// Bluetooth base UUID = 0000xxxx-0000-1000-8000-00805F9B34FB
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int REFRESH_CHAT_CONTENT = 4;
    public static final int CONN_STAT_ACCEPT_THREAD_BEGIN = 5;
    public static final int CONN_STAT_CONNECT_THREAD_BEGIN = 6;
    public static final int CONN_STAT_CONNECTED_THREAD_BEGIN = 7;
    public static final int CONN_STAT_DISCONNECTED = 8;

    public BTConnectService(Handler handler) {
        this.handler = handler;
        this.connectStatus = STATE_NONE;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.chatActivityIsDestroyed = false;
    }

    private synchronized void accept() {
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        acceptThread = new AcceptThread();
        acceptThread.start();
        setState(STATE_LISTEN);
    }

    public synchronized void connect(BluetoothDevice device) throws IOException {
//        rmtBlueToothDvc = device;
        Log.v(TAG, "connect()");

        if (acceptThread != null) {
            acceptThread = null;
        }
        if (connectThread != null) {
            connectThread.cancel();
        }
        if (connectedThread != null) {
            connectedThread.cancel();
        }

        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    private synchronized void connected(BluetoothSocket socket) {
        Log.v(TAG, "connected()");

        if (acceptThread != null) {
            acceptThread.cancel();
        }
        if (connectThread != null) {
            connectThread.cancel();
        }
        if (connectedThread != null) {
            connectedThread.cancel();
        }

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
        setState(STATE_CONNECTED);
    }

    private synchronized void setState(int state) {
        connectStatus = state;
    }

    public void sendMessage(String chatText) {
        if (chatText.length() > 0) {
            byte[] bytes = chatText.getBytes();//            BTConnectService.this.write(bytes);
            ConnectedThread tmp;// Create temporary object
            synchronized (this) {// Synchronize a copy of the ConnectedThread
                if (connectStatus != STATE_CONNECTED) return;
                tmp = connectedThread;
            }
            tmp.write(bytes);// Perform the write unsynchronized
        }
    }

    public synchronized void stop(boolean IsDestroyed) {
        this.chatActivityIsDestroyed = IsDestroyed;//放在socket关闭导致IOException前
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    // Connecting as a Server
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket srvSocket;

        private AcceptThread() {
            Log.v(TAG, "create AcceptThread");

            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(myServerName, uuid);
            } catch (IOException e) {
                Log.v(TAG, "listenUsingRfcommWithServiceRecord() failed", e);
            }

            srvSocket = tmp;
        }

        @Override
        public void run() {
            Log.v(TAG, "AcceptThread begin......");
            handler.sendEmptyMessage(CONN_STAT_ACCEPT_THREAD_BEGIN);

            BluetoothSocket localSocket = null;
            // Keep listening until a socket is returned or exception occurs
            while (connectStatus != STATE_CONNECTED) {
                try {
                    Log.v(TAG, "BluetoothServerSocket.accept()<======");
                    localSocket = srvSocket.accept();// This is a blocking call
                } catch (IOException e) {
                    Log.v(TAG, "accept() failed", e);
                    break;// step out from while()
                }

                if (localSocket != null) {// If a connection was successful
                    synchronized (BTConnectService.this) {
                        switch (connectStatus) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal.
                                // Start the connected thread.
                                BTConnectService.this.connected(localSocket);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected.
                                // Terminate new socket.
                                try {
                                    localSocket.close();
                                } catch (IOException e) {
                                    Log.v(TAG, "could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            try {
                srvSocket.close();
            } catch (IOException e) {
                Log.v(TAG, "close server failed", e);
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    // Connecting as a Client
    private class ConnectThread extends Thread {
        private final BluetoothSocket localSocket;

        public ConnectThread(BluetoothDevice device) throws IOException {
            Log.v(TAG, "create ConnectThread");

            BluetoothSocket tmp = null;
            tmp = device.createRfcommSocketToServiceRecord(uuid);
            localSocket = tmp;
        }

        public void run() {
            Log.v(TAG, "ConnectThread begin......");
            handler.sendEmptyMessage(CONN_STAT_CONNECT_THREAD_BEGIN);

            bluetoothAdapter.cancelDiscovery();// Cancel discovery because it will slow down the connection
            try {
                Log.v(TAG, "BluetoothSocket.connect()======>");
                localSocket.connect();// This will block until it succeeds or throws an exception
            } catch (IOException connectException) {
                setState(STATE_LISTEN);
                try {
                    localSocket.close();// when Unable to connect, close the socket
                } catch (IOException closeException) {
                    Log.v(TAG, "unable to close socket during connection failure", closeException);
                }

                BTConnectService.this.accept();// become Listening mode
                return;
            }
            synchronized (BTConnectService.this) {
                connectThread = null;
            }
            BTConnectService.this.connected(localSocket);
        }

        // used to close socket
        public void cancel() {
            try {
                localSocket.close();
            } catch (IOException e) {
                Log.v(TAG, "close unwanted socket failed", e);
            }
        }


    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket localSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.v(TAG, "create ConnectedThread");
            localSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.v(TAG, "temp sockets not created", e);
            }
            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            Log.v(TAG, "[ Transmission is on ]");
            handler.sendEmptyMessage(CONN_STAT_CONNECTED_THREAD_BEGIN);

            byte[] buffer = new byte[1024];
            while (true) {// Keep listening to InputStream while connected
                try {
                    for (int i = 0; i < buffer.length; i++) {
                        buffer[i] = 0;
                    }
                    inputStream.read(buffer);// Read from the InputStream
                    String rcvStr = new String(buffer);
                    Bundle bundle = new Bundle();
                    bundle.putString("recv_chat_content", rcvStr);

                    Message msg = new Message();
                    msg.what = REFRESH_CHAT_CONTENT;
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                } catch (IOException e) {
                    Log.v(TAG, "[ Transmission is off... ]", e);
                    handler.sendEmptyMessage(CONN_STAT_DISCONNECTED);
                    if (chatActivityIsDestroyed == false) {
                        BTConnectService.this.accept();// become listening-mode when ChatActivity is not destroyed
                    }
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                outputStream.write(buffer);
            } catch (IOException e) {
                Log.v(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                localSocket.close();
            } catch (IOException e) {
                Log.v(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
