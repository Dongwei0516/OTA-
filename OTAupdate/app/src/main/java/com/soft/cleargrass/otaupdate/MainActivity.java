package com.soft.cleargrass.otaupdate;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;
    private List<BluetoothDevice> mList = new ArrayList<>();
    private RelativeLayout mScanLayout1, mScanLayout2,refreshBtn;
    private ListViewAdapter mListViewAdapter;
    private ArrayList<Integer> mRSSI;
    private ListView deviceList;
    private Context context;

    public BluetoothGatt mBluetoothGatt;

    List<Device> mDeviceList = new ArrayList<Device>();

    Button scanStartBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceList = (ListView) findViewById(R.id.devicelist);
        scanStartBtn = (Button)findViewById(R.id.startScanBtn);
        mScanLayout1 = (RelativeLayout)findViewById(R.id.startScanRl);
        mScanLayout2 = (RelativeLayout)findViewById(R.id.blelist_Rl);
        refreshBtn = (RelativeLayout) findViewById(R.id.refresh_btn);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        scanStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                scanStartBtn.setVisibility(View.GONE);
                mScanLayout1.setVisibility(View.GONE);
                mScanLayout2.setVisibility(View.VISIBLE);
                deviceList.setVisibility(View.VISIBLE);
//                onClickScan(v);

                mBluetoothAdapter.startLeScan(mLeScanCallback);
                mBluetoothAdapter.startDiscovery();

                mListViewAdapter = new ListViewAdapter(mDeviceList, context);
            }
        });

        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Integer rssi = null;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mDevice = mDeviceList.get(position).getDevices();
                rssi = mDeviceList.get(position).getRssis();
//                bottomMsg.setText(context.getString(R.string.dmp_test_cancel));
                connect(mDevice);
                BluetoothDevice device = mDeviceList.get(position).getDevices();
                Intent intent = new Intent(MainActivity.this, DfuActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelable("device", device);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mList.clear();
                mDeviceList.clear();
                mBluetoothAdapter.startLeScan(mLeScanCallback);
                mBluetoothAdapter.startDiscovery();
            }
        });

    }

    public static class Device extends Object {
        private BluetoothDevice devices;
        private int rssis;

        public Device(BluetoothDevice devices, Integer rssis) {
            this.devices = devices;
            this.rssis = rssis;
        }

        public void setDevices(BluetoothDevice devices) {
            this.devices = devices;
        }

        public void setRssis(int rssis) {
            Intent intent = new Intent();
            this.rssis = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
        }

        public BluetoothDevice getDevices() {
            return devices;
        }

        public Integer getRssis() {
            return rssis;
        }

        public String toString() {
            return this.getDevices().getAddress();
        }

        public boolean equals(Object obj) {
            Device device = (Device) obj;
            return device.devices.getAddress() == devices.getAddress();
        }

        public int hashCode() {
            Device device = this;
            return device.hashCode();
        }

    }

    public class sortRssi implements Comparator<Device> {
        @Override
        public int compare(Device o1, Device o2) {
            return o2.getRssis() - o1.getRssis();
        }
    }


    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {

          if (!TextUtils.isEmpty(device.getName()) && rssi > -70) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String hexStr = Utils.bytesToHexString(scanRecord);
                        String str = Utils.hexStrToStr(hexStr);
//                        Log.d("scanRecord", device.getAddress() + "  " + hexStr);
//                        Log.d("scanRecord", device.getAddress() + "  " + Utils.decode(hexStr));
                        if (!mList.contains(device)) {
                            Log.d("111","1111");
                            mList.add(device);
                            mRSSI.add(rssi);
                            Log.d("rssi", String.valueOf(rssi));
                            Device device1 = new Device(device, rssi);
                            if (!mDeviceList.contains(device1)) {
                                mDeviceList.add(device1);
                            }
//                            Collections.sort(mDeviceList, new sortRssi());
//                        Log.d("List", String.valueOf(mList));
//                        Log.d("DeviceList", String.valueOf(mDeviceList));

                        }else {
                            Log.d("222","2222");
                            mList.indexOf(device);
                            mRSSI.indexOf(rssi);
                            Log.d("rssi", String.valueOf(rssi));
//                            Device device1 = new Device(device, rssi);
//                            for (int i=0; i<mDeviceList.size(); i++) {
//                                if (mDeviceList.get(i).getDevices().equals(device1.getDevices())) {
//                                    mDeviceList.indexOf(device1);
//                                }
//                            }
//                            Collections.sort(mDeviceList, new sortRssi());
                        }
                        Collections.sort(mDeviceList, new sortRssi());
                        if (!mListViewAdapter.isEmpty()) {
                            deviceList.setAdapter(mListViewAdapter);
                        }
                    }
                });
            }
        }
    };

    public class ListViewAdapter extends BaseAdapter {

        private LayoutInflater layoutInflater;

        public ListViewAdapter(List<Device> list, Context context) {
            mDeviceList = list;
            layoutInflater = LayoutInflater.from(MainActivity.this);
            mRSSI = new ArrayList<Integer>();
        }


        @Override
        public int getCount() {
            return mDeviceList.size();
        }

        @Override
        public Object getItem(int position) {
            return mDeviceList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null) {
                viewHolder = new ListViewAdapter.ViewHolder();
                convertView = layoutInflater.inflate(R.layout.list_item, null);
                viewHolder.device = (TextView) convertView.findViewById(R.id.device);
                viewHolder.address = (TextView) convertView.findViewById(R.id.address);
                viewHolder.image = (ImageView) convertView.findViewById(R.id.strength);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ListViewAdapter.ViewHolder) convertView.getTag();
            }

            if (TextUtils.isEmpty(mDeviceList.get(position).getDevices().getName())) {
                viewHolder.device.setText("未找到设备！");
            } else {
                viewHolder.device.setText(mDeviceList.get(position).getDevices().getName());
            }

//            Collections.sort(mRSSI);
            viewHolder.address.setText(mDeviceList.get(position).getDevices().getAddress() + "    RSSI:" + mDeviceList.get(position).getRssis());
//            viewHolder.address.setText(mDeviceList.get(position).getDevices().getAddress() + "    RSSI:" + mRSSI.get(position));

//            Log.d("strength", String.valueOf(mDeviceList.get(position).getRssis()));

            if (mDeviceList.get(position).getRssis() > -30) {
                viewHolder.image.setImageResource(R.drawable.wifi11);
            } else if (mDeviceList.get(position).getRssis() > -50) {
                viewHolder.image.setImageResource(R.drawable.wifi21);
            } else if (mDeviceList.get(position).getRssis() > -70) {
                viewHolder.image.setImageResource(R.drawable.wifi31);
            } else {
                viewHolder.image.setImageResource(R.drawable.wifi41);
            }

            return convertView;

        }

        class ViewHolder {
            TextView device;
            TextView address;
            ImageView image;
        }
    }


    public void connect(final BluetoothDevice device){
        mBluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
        Log.d("Connect","startConnect");
    }

    BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
                Log.d("Connected", "onConnected");
            } else {
                Log.d("Connected", "DisConnected: " + status);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

//                getNotification();
                Log.d("State", "ServicesDiscovered");
            } else {
                Log.d("State", "SvcDiscoveredFailed: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                Log.d("State", "CharRead");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d("State", "CharWrite");
        }


    };

}
