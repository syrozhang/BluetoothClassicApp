package com.syro.testbluetooth10.Activity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import com.syro.testbluetooth10.R;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.syro.testbluetooth10.Adapter.DeviceListAdapter;
import com.syro.testbluetooth10.Util.BTPairService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private SlidingMenu slidingMenu;
    private Button bluetoothScan;
    private Button bluetoothSwitch;
    private ListView prdListView;
    private ListView avbListView;
//    private PullToRefreshListView prdListView;
//    private PullToRefreshListView avbListView;
    private TextView btStatusBar;
    private BluetoothAdapter bluetoothAdapter;
    List<HashMap<String, Object>> remoteDvcList = new ArrayList<HashMap<String, Object>>();
    List<HashMap<String, Object>> pairedDvcList = new ArrayList<HashMap<String, Object>>();
    private SimpleAdapter adapterRmtDvc;
    private DeviceListAdapter adapterPrdDvc;
    private String TAG = "SyroZhang";
    private String pairPin = "101010";
    private static Timer timerCnt;
    private int ticks;
    private int btDvcCnt;
    private boolean toolbarItemIsChecked;
    private static final int REFRESH_TIMER = 1;
    private static final int REFRESH_LISTVIEW = 2;

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH_TIMER:
                    btStatusBar.setText("Bluetooth: scanning " + ticks + " seconds...");
                    ticks--;
                    if (ticks < 0) {
                        stopTimer();
                        btStatusBar.setText("Bluetooth: scanning is over...");
                    }
                    break;
                case REFRESH_LISTVIEW:
                    findPairedBluetoothDvc();
                    bluetoothAdapter.startDiscovery();//搜索没有配对过的蓝牙设备
                    startTimer(12);
                    adapterPrdDvc.notifyDataSetChanged();
                    adapterRmtDvc.notifyDataSetChanged();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.v(TAG, "onCreate()");

        initToolbar();
        initSlidingMenu();
        btStatusBar = (TextView) findViewById(R.id.bluetooth_status_bar);
        prdListView = (ListView) findViewById(R.id.paired_device_listview);
        avbListView = (ListView) findViewById(R.id.available_device_listview);
//        prdListView = (PullToRefreshListView) findViewById(R.id.paired_device_listview);
//        avbListView = (PullToRefreshListView) findViewById(R.id.available_device_listview);
        bluetoothScan = (Button) findViewById(R.id.bluetooth_scan_btn);
        bluetoothSwitch = (Button) findViewById(R.id.btn_bluetooth_switch);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isEnabled()) {
            btStatusBar.setText("Bluetooth: [ Enabled ]");
        } else {
            btStatusBar.setText("Bluetooth: [ Disabled ]");
        }

        adapterRmtDvc = new SimpleAdapter(
                MainActivity.this,
                remoteDvcList,
                R.layout.list_item_available_devices,
                new String[]{"img", "name", "addr"},
                new int[]{R.id.dvc_img, R.id.dvc_name, R.id.dvc_addr});

        adapterPrdDvc = new DeviceListAdapter(
                MainActivity.this,
                pairedDvcList,
                R.layout.list_item_paired_devices);

        avbListView.setAdapter(adapterRmtDvc);//在屏幕上显示ListView
        prdListView.setAdapter(adapterPrdDvc);//在屏幕上显示ListView

        IntentFilter intf = new IntentFilter();
        intf.addAction(BluetoothDevice.ACTION_FOUND);
        intf.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intf.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intf.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(appReceiver, intf);

        bluetoothSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toolbarItemIsChecked = !toolbarItemIsChecked;
                stopTimer();// always turnoff the Timer
                if (toolbarItemIsChecked == true) {
                    startBluetooth();
                } else {
                    pairedDvcList.clear();
                    remoteDvcList.clear();
                    adapterPrdDvc.notifyDataSetChanged();
                    adapterRmtDvc.notifyDataSetChanged();
                    bluetoothAdapter.disable();
                    btStatusBar.setText("Bluetooth: [ Disabled ]");
                }
            }
        });

        bluetoothScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter.isEnabled()) {
                    btDvcCnt = 0;
                    if (bluetoothAdapter.isDiscovering()) {
                        bluetoothAdapter.cancelDiscovery();
                        stopTimer();
                    }
                    startTimer(12);
                    pairedDvcList.clear();
                    remoteDvcList.clear();
                    findPairedBluetoothDvc();
                    bluetoothAdapter.startDiscovery();//搜索蓝牙设备
                    handler.sendEmptyMessage(REFRESH_LISTVIEW);//refresh listview
                } else {
                    stopTimer();
                    pairedDvcList.clear();
                    remoteDvcList.clear();
                    btStatusBar.setText("Bluetooth: *** Bluetooth isn\'t enabled yet ***");
                }
            }
        });


        prdListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.v(TAG, "name: " + (pairedDvcList.get(position)).get("name") + "\n" +
                        (pairedDvcList.get(position)).get("addr"));

                //进入ChatActivity
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                intent.putExtra("deviceName", "" + (pairedDvcList.get(position)).get("name"));
                intent.putExtra("address", "" + (pairedDvcList.get(position)).get("addr"));
                startActivity(intent);
            }
        });


//        avbListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Log.v(TAG, "name: " + (remoteDvcList.get(position)).get("name") + "\n" +
//                        (remoteDvcList.get(position)).get("addr"));
//
//                String address = "" + (remoteDvcList.get(position)).get("addr");
//                try {
//                    pairBluetoothDevice(address);//配对远程蓝牙设备
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
        avbListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.v(TAG, "name: " + (remoteDvcList.get(position)).get("name") + "\n" +
                        (remoteDvcList.get(position)).get("addr"));

                String address = "" + (remoteDvcList.get(position)).get("addr");
                try {
                    pairBluetoothDevice(address);//配对远程蓝牙设备
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

//        avbListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
//            @Override
//            public void onRefresh(PullToRefreshBase<ListView> refreshView) {
//                new AsyncTask<Void, Void, Void>() {
//                    @Override
//                    protected Void doInBackground(Void... params) {
//                        //刷新时的耗时操作都放doInBackground()内
//                        if (bluetoothAdapter.isEnabled()) {
//                            btDvcCnt = 0;
//                            if (bluetoothAdapter.isDiscovering()) {
//                                bluetoothAdapter.cancelDiscovery();
//                                stopTimer();
//                            }
//                            startTimer(12);
//                            pairedDvcList.clear();
//                            remoteDvcList.clear();
//                            findPairedBluetoothDvc();
//                            bluetoothAdapter.startDiscovery();//搜索蓝牙设备
//                            handler.sendEmptyMessage(REFRESH_LISTVIEW);//refresh listview
//                        } else {
//                            stopTimer();
//                            pairedDvcList.clear();
//                            remoteDvcList.clear();
//                        }
//                        return null;
//                    }
//
//                    @Override
//                    protected void onPostExecute(Void aVoid) {
//                        //onPostExecute()可以使用doInBackground()的结果来操作UI线程
//
//                        avbListView.onRefreshComplete();
//                    }
//                }.execute();
//            }
//        });
    }

    private void initSlidingMenu() {
        slidingMenu = new SlidingMenu(this);
        slidingMenu.setMode(SlidingMenu.RIGHT);
        slidingMenu.setBehindOffsetRes(R.dimen.sliding_menu_offset);
        slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        slidingMenu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
        slidingMenu.setMenu(R.layout.sliding_menu);
    }

    private BroadcastReceiver appReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {//搜索到新设备
                btDvcCnt++;
                Log.v(TAG, "Available Devices[" + btDvcCnt + "] name: " + device.getName() + " " + "addr: " + device.getAddress());
                //只添加没有配对过的蓝牙设备
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    //遍历remoteDvcList，若存在地址相同的设备就不加入remoteDvcList
                    boolean isExist = false;
                    for (Iterator<HashMap<String, Object>> i = remoteDvcList.iterator(); i.hasNext(); ) {
                        if ((i.next().get("addr")).equals(device.getAddress())) {
                            isExist = true;
                        }
                    }
                    if (isExist == false) {
                        HashMap<String, Object> btDevice = new HashMap<String, Object>();
                        btDevice.put("img", R.drawable.bluetooth);
                        btDevice.put("name", btDvcCnt + ": " + device.getName());
                        btDevice.put("addr", device.getAddress());
                        remoteDvcList.add(btDevice);
                    }
                }
                handler.sendEmptyMessage(REFRESH_LISTVIEW);//refresh listview
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_BONDING:
                        Log.v(TAG, "开始配对");
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        Log.v(TAG, "完成配对");
                        //遍历去掉remoteDvcList中配对完成的蓝牙设备
                        for (Iterator<HashMap<String, Object>> i = remoteDvcList.iterator(); i.hasNext(); ) {
                            if (((i.next()).get("addr")).equals(device.getAddress())) {
                                i.remove();
                            }
                        }
                        handler.sendEmptyMessage(REFRESH_LISTVIEW);//refresh listview
                        break;
                    case BluetoothDevice.BOND_NONE:
                        Log.v(TAG, "取消配对");
                        //遍历去掉pairedDvcList中取消配对的蓝牙设备
                        for (Iterator<HashMap<String, Object>> i = pairedDvcList.iterator(); i.hasNext(); ) {
                            if ((i.next().get("addr")).equals(device.getAddress())) {
                                i.remove();
                            }
                        }
                        handler.sendEmptyMessage(REFRESH_LISTVIEW);//refresh listview
                        break;
                }
            } else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                try {
                    Log.v(TAG, "ACTION_PAIRING_REQUEST...");
                    BTPairService.setPin(device.getClass(), device, pairPin);
                    BTPairService.cancelPairingUserInput(device.getClass(), device);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.v(TAG, "扫描蓝牙设备开始...");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.v(TAG, "扫描蓝牙设备结束...");
            }

        }
    };

    private void startBluetooth() {
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                //使蓝牙设备可见以便配对
                Intent intent1 = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//                intent1.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                intent1.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);// always discoverable
                startActivity(intent1);
                //请求用户开启蓝牙系统服务
                Intent intent2 = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(intent2);
                bluetoothAdapter.enable();//直接开启，不经过提示
                btStatusBar.setText("Bluetooth: [ Enabled ]");
            }
        } else {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("No bluetoothAdapter devices");
            dialog.setMessage("Your equipment does not support bluetoothAdapter, please change device");
            dialog.setNegativeButton("cancel",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
            dialog.show();
        }
    }

    private void findPairedBluetoothDvc() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();//返回配对的远程蓝牙设备
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                Log.v(TAG, "Paired Devices [ name: " + device.getName() + " " + "; address: " + device.getAddress());
                //遍历pairedDvcList，若存在地址相同的设备就不加入pairedDvcList
                boolean isExist = false;
                for (Iterator<HashMap<String, Object>> i = pairedDvcList.iterator(); i.hasNext(); ) {
                    if ((i.next().get("addr")).equals(device.getAddress())) {
                        isExist = true;
                    }
                }
                if (isExist == false) {
                    HashMap<String, Object> btDevice = new HashMap<String, Object>();
                    btDevice.put("img", R.drawable.bluetooth);
                    btDevice.put("name", device.getName());
                    btDevice.put("addr", device.getAddress());
                    pairedDvcList.add(btDevice);
                }
            }
        } else {
            Log.v(TAG, "No paired devices founded...");
        }

    }

    private void startTimer(int num) {
        if (timerCnt != null) {
            stopTimer();
        }
        ticks = num;
        timerCnt = new Timer();
        timerCnt.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.sendEmptyMessage(REFRESH_TIMER);
            }
        }, 0, 1000);//每1000ms发送一次信号给handler
    }

    private void stopTimer() {
        if (timerCnt != null) {
            timerCnt.cancel();
            timerCnt = null;
        }
    }


    //主动蓝牙设备配对
    private void pairBluetoothDevice(String address) throws Exception {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        Log.v(TAG, "target_address: " + address);
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
//        BTPairService.printAllInform(device.getClass());// print all class-info
        if (device.getBondState() == BluetoothDevice.BOND_NONE) {
            BTPairService.setPin(device.getClass(), device, pairPin);
            BTPairService.createBond(device.getClass(), device);
        }
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.activity_main_toolbar);
        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("BTChat");
//        toolbar.setSubtitle("副标题");
        toolbar.setLogo(R.drawable.jeckson);
//        toolbar.setNavigationIcon(android.R.drawable.ic_input_delete);

//        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                toolbarItemIsChecked = !toolbarItemIsChecked;
//                switch (item.getItemId()) {
//                    case R.id.action_bluetooth_switch:
//                        stopTimer();// always turnoff the Timer
//                        if (toolbarItemIsChecked == true) {
//                            startBluetooth();
//                        } else {
//                            pairedDvcList.clear();
//                            remoteDvcList.clear();
//                            adapterPrdDvc.notifyDataSetChanged();
//                            adapterRmtDvc.notifyDataSetChanged();
//                            bluetoothAdapter.disable();
//                            btStatusBar.setText("Bluetooth: [ Disabled ]");
//                        }
//                        break;
//                }
//                return false;
//            }
//        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                slidingMenu.toggle(true);
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "onStop()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.v(TAG, "onRestart()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothAdapter != null) {
            bluetoothAdapter.disable();
        }
        unregisterReceiver(appReceiver);
        Log.v(TAG, "onDestroy()");
    }

}