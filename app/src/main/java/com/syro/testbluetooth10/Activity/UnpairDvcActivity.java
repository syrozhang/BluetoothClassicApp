package com.syro.testbluetooth10.Activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.syro.testbluetooth10.Util.BTPairService;
import com.syro.testbluetooth10.R;

public class UnpairDvcActivity extends AppCompatActivity {
    private String TAG = "SyroZhang";
    private BluetoothAdapter bluetoothAdapter;
    private Button btnUnpair;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unpair);

        final Intent intent = getIntent();
        Log.v(TAG, "pairDvcName: " + intent.getStringExtra("pairDvcName") + " " +
                "pairDvcAddr: " + intent.getStringExtra("pairDvcAddr"));
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        btnUnpair = (Button) findViewById(R.id.btn_unpair_activity);
        btnUnpair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    unpairBluetoothDevice(intent.getStringExtra("pairDvcAddr"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finish();
            }
        });

    }

    private void unpairBluetoothDevice(String address) throws Exception {
        //取消蓝牙配对
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        BTPairService.removeBond(device.getClass(), device);
    }
}
