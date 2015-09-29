package com.finrobotics.neyyasample;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.finrobotics.neyyasdk.core.NeyyaDevice;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "NeyyaSDK";
    private MyService mMyService;
    private TextView mStatusTextView;
    private boolean mScanning = false;
    private Button mSearchButton;
    private DeviceListAdapter mDeviceListAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStatusTextView = (TextView) findViewById(R.id.statusTextView);
        mSearchButton = (Button) findViewById(R.id.searchButton);
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mScanning) {
                   // mMyService.startSearch();
                    final Intent intent = new Intent(MyService.BROADCAST_COMMAND_SEARCH);
                    sendBroadcast(intent);
                } else {
                   // mMyService.stopSearch();
                }
            }
        });
        ListView mDeviceListView = (ListView) findViewById(R.id.deviceListView);
        mDeviceListAdapter = new DeviceListAdapter();
        mDeviceListView.setAdapter(mDeviceListAdapter);
        mDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, ConnectActivity.class);
                intent.putExtra("SELECTED_DEVICE", mDeviceListAdapter.getDevice(position));
                startActivity(intent);
            }
        });

        mStatusTextView.setText("Disconnected");
        Intent neyyaServiceIntent = new Intent(this, MyService.class);
       // bindService(neyyaServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        startService(neyyaServiceIntent);


    }

    @Override
    protected void onResume() {
        super.onResume();
        logd("On Resume Main Activity");
        registerReceiver(mNeyyaUpdateReceiver, makeNeyyaUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        logd("On Pause Main Activity");
        unregisterReceiver(mNeyyaUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
       // unbindService(mServiceConnection);
        logd("On destroy MainActivity");
        super.onDestroy();
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            logd("Service bound");
            mMyService = (MyService) ((MyService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    private final BroadcastReceiver mNeyyaUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            //  logd("Broadcast received");
            final String action = intent.getAction();
            if (MyService.BROADCAST_STATE.equals(action)) {
                int status = intent.getIntExtra(MyService.STATE_DATA, 0);
                if (status == MyService.STATE_DISCONNECTED) {
                    mStatusTextView.setText("Disconnected");
                } else if (status == MyService.STATE_SEARCHING) {
                    mStatusTextView.setText("Searching");
                    mSearchButton.setText("Stop Search");
                    mDeviceListAdapter.clear();
                    mDeviceListAdapter.notifyDataSetChanged();
                    mScanning = true;
                } else if (status == MyService.STATE_SEARCH_FINISHED) {
                    mStatusTextView.setText("Searching finished");
                    mSearchButton.setText("Start Search");
                    mScanning = false;
                } else {
                    mStatusTextView.setText("Status - " + status);
                }

            } else if (MyService.BROADCAST_DEVICES.equals(action)) {
                mDeviceListAdapter.setDevices((ArrayList<NeyyaDevice>) intent.getSerializableExtra(MyService.DEVICE_LIST_DATA));
                mDeviceListAdapter.notifyDataSetChanged();

            } else if (MyService.BROADCAST_ERROR.equals(action)) {
                int errorNo = intent.getIntExtra(MyService.ERROR_NUMBER_DATA, 0);
                String errorMessage = intent.getStringExtra(MyService.ERROR_MESSAGE_DATA);
                logd("Error occurred. Error number - " + errorNo + " Message - " + errorMessage);
            }
        }
    };

    private class DeviceListAdapter extends BaseAdapter {
        private ArrayList<NeyyaDevice> mDevices;
        private LayoutInflater mInflator;


        public DeviceListAdapter() {
            super();
            mDevices = new ArrayList<>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void setDevices(ArrayList<NeyyaDevice> devices) {
            this.mDevices = devices;
        }

        public NeyyaDevice getDevice(int position) {
            return mDevices.get(position);
        }

        public void clear() {
            mDevices.clear();
        }

        @Override
        public int getCount() {
            return mDevices.size();
        }

        @Override
        public Object getItem(int position) {
            return mDevices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            NeyyaDevice device = mDevices.get(position);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());
            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    private IntentFilter makeNeyyaUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MyService.BROADCAST_STATE);
        intentFilter.addAction(MyService.BROADCAST_DEVICES);
        intentFilter.addAction(MyService.BROADCAST_ERROR);
        intentFilter.addAction(MyService.BROADCAST_LOG);
        return intentFilter;
    }


    private void loge(final String message) {
        if (com.finrobotics.neyyasdk.BuildConfig.DEBUG)
            Log.e(TAG, message);
    }

    private void loge(final String message, final Throwable e) {
        if (com.finrobotics.neyyasdk.BuildConfig.DEBUG)
            Log.e(TAG, message, e);
    }

    private void logw(final String message) {
        if (com.finrobotics.neyyasdk.BuildConfig.DEBUG)
            Log.w(TAG, message);
    }

    private void logi(final String message) {
        if (com.finrobotics.neyyasdk.BuildConfig.DEBUG)
            Log.i(TAG, message);
    }

    private void logd(final String message) {
        //  if (com.finrobotics.neyyasdk.BuildConfig.DEBUG)
        Log.d(TAG, message);
    }
}
