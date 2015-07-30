package com.jifalops.btbeacon;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {
    private static final int REQUEST_BT_DISCOVERABLE = 1;
    private TextView textView;
    private BluetoothAdapter btAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.text);
        autoScrollTextView(textView, (ScrollView) findViewById(R.id.scrollView));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_BT_DISCOVERABLE) {
            if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "App cannot work unless Bluetooth is discoverable.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isFinishing()) {
            return;
        }

        BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();

        registerReceiver(scanModeChangedReceiver, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));

        if (btAdapter == null || !btAdapter.isEnabled() ||
                btAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            requestBtDiscoverable(0);
            return;
        }

        registerReceiver(scanReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(discoveryStartedReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        registerReceiver(discoveryFinishedReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        btAdapter.startDiscovery();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(scanModeChangedReceiver);
        if (btAdapter != null && btAdapter.isEnabled() &&
                btAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            unregisterReceiver(scanReceiver);
            unregisterReceiver(discoveryStartedReceiver);
            unregisterReceiver(discoveryFinishedReceiver);
            btAdapter.cancelDiscovery();

            requestBtDiscoverable(1);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void autoScrollTextView(TextView tv, final ScrollView sv) {
        tv.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                sv.post(new Runnable() {
                    @Override
                    public void run() {
                        sv.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    private void requestBtDiscoverable(int duration) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
        startActivityForResult(intent, REQUEST_BT_DISCOVERABLE);
    }

    private final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                textView.append("RX " + rssi + "dBm from " + device.getAddress() + ".\n");
            }
        }
    };

    private final BroadcastReceiver discoveryStartedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            textView.append("Discovery started.\n");
        }
    };

    private final BroadcastReceiver discoveryFinishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            textView.append("Discovery finished. Restarting...\n");
            btAdapter.startDiscovery();
        }
    };

    private final BroadcastReceiver scanModeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int newMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1);
            int oldMode = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, -1);
            if (oldMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE &&
                    newMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                textView.append("Device is now discoverable.\n");
            } else if (oldMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE &&
                    newMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                textView.append("Device is no longer discoverable.\n");
            }
        }
    };
}
