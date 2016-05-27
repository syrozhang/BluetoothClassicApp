package com.syro.testbluetooth10.Activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.syro.testbluetooth10.R;
import com.syro.testbluetooth10.Util.*;
import java.io.IOException;

public class ChatActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private Button send;
    private EditText editWindow;
    private TextView chatWindow;
    private TextView connStatBar;
    private ScrollView scrollView;
    private BluetoothAdapter bluetoothAdapter;
    private String TAG = "SyroZhang";
    private String rmtDvcName;
    private String rmtDvcAddr;
    private StringBuffer stringBuffer = new StringBuffer();
    private BTConnectService btConnectService;

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BTConnectService.REFRESH_CHAT_CONTENT:
                    stringBuffer.setLength(0);
                    stringBuffer.append(chatWindow.getText().toString() + "\n"
                            + msg.getData().getString("recv_chat_content"));
                    chatWindow.setText(stringBuffer);// display what a remote Bluetooth device said
                    scrollToBottom(scrollView, chatWindow);
                    break;
                case BTConnectService.CONN_STAT_ACCEPT_THREAD_BEGIN:
                    connStatBar.setText("listening...");
                    break;
                case BTConnectService.CONN_STAT_CONNECT_THREAD_BEGIN:
                    connStatBar.setText("Connecting...");
                    break;
                case BTConnectService.CONN_STAT_CONNECTED_THREAD_BEGIN:
                    connStatBar.setText("[ Transmission is on ]");
                    break;
                case BTConnectService.CONN_STAT_DISCONNECTED:
                    connStatBar.setText("[ Transmission is off... ]");
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initToolbar();
        chatWindow = (TextView) findViewById(R.id.chat_disp);
        editWindow = (EditText) findViewById(R.id.chat_content);
        connStatBar = (TextView) findViewById(R.id.connection_stat);
        btConnectService = new BTConnectService(handler);

        Intent i = getIntent();
        rmtDvcName = i.getStringExtra("deviceName");
        rmtDvcAddr = i.getStringExtra("address");
//        Log.v(TAG, "Paired Device name = " + rmtDvcName);
//        Log.v(TAG, "Paired Device addr = " + rmtDvcAddr);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice rmtBluetoothDvc = bluetoothAdapter.getRemoteDevice(rmtDvcAddr);
        try {
            btConnectService.connect(rmtBluetoothDvc);// connect to remote bluetooth device
        } catch (IOException e) {
            Log.v(TAG, "BTConnectService.connect() failed");
        }

        send = (Button) findViewById(R.id.btn_send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String chatText = bluetoothAdapter.getName() + " : " + editWindow.getText().toString();
                btConnectService.sendMessage(chatText);// send chatMsg to remote Bluetooth device

                chatText = "我：" + editWindow.getText().toString();
                stringBuffer.setLength(0);// clear StringBuffer
                stringBuffer.append(chatWindow.getText().toString() + "\n" + chatText);
                chatWindow.setText(stringBuffer);// display what I said
                editWindow.setText("");// clear edit window
                scrollToBottom(scrollView, chatWindow);
            }
        });

        scrollView = (ScrollView) findViewById(R.id.scrview);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:// phone's backward key to close ChatActivity
                finish();
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.activity_main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("BTChat");
        toolbar.setLogo(R.drawable.jeckson);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        btConnectService.stop(true);
        btConnectService = null;
    }

    private void scrollToBottom(final ScrollView scrollView, final View inner) {
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (scrollView == null || inner == null) {
                    return;
                }
                int offset = inner.getMeasuredHeight() - scrollView.getMeasuredHeight();
                if (offset < 0) {
                    offset = 0;
                }
                scrollView.scrollTo(0, offset);
            }
        });
    }

}
