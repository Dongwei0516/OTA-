package com.soft.cleargrass.otaupdate;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by dongwei on 2017/4/25.
 */

public class BleService extends Service{

    public BluetoothGatt mBluetoothGatt;
    private BluetoothManager mBluetoothManager = null;
    private BluetoothAdapter mBluetoothAdapter = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
