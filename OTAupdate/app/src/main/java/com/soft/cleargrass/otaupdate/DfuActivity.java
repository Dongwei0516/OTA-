package com.soft.cleargrass.otaupdate;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.LoaderManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

/**
 * Created by dongwei on 2017/4/25.
 */

public class DfuActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor>{
    private Button btnDfuUpdate, btnScanStart, btnFileSelect;
    private TextView deviceDfu, fileNameTv, btmMsg;
    private BluetoothGatt mBluetoothGatt;
    private Context context;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private Handler mHandler;
    private BluetoothDevice dfuDevice;
    private int mFileType;
    private int mFileTypeTmp;
    private Uri mFileStreamUri;
    private String mInitFilePath;
    private Uri mInitFileStreamUri;
    private static final String EXTRA_URI = "uri";
    public static final String SETTINGS_KEEP_BOND = "settings_keep_bond";
    private static final String PREFS_DEVICE_NAME = "no.nordicsemi.android.nrftoolbox.dfu.PREFS_DEVICE_NAME";
    private static final String PREFS_FILE_NAME = "no.nordicsemi.android.nrftoolbox.dfu.PREFS_FILE_NAME";
    private static final String PREFS_FILE_TYPE = "no.nordicsemi.android.nrftoolbox.dfu.PREFS_FILE_TYPE";
    private static final String PREFS_FILE_SIZE = "no.nordicsemi.android.nrftoolbox.dfu.PREFS_FILE_SIZE";

    private String mFilePath;
    private static final int PERMISSION_REQ = 25;
    private static final int ENABLE_BT_REQ = 0;
    private static final int SELECT_FILE_REQ = 1;
    private static final int SELECT_INIT_FILE_REQ = 2;

    private Dialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dfu);

        final String address,name;

        Intent intent = this.getIntent();
        mBluetoothDevice = intent.getParcelableExtra("device");
        Log.d("device", mBluetoothDevice.getAddress());
        name = mBluetoothDevice.getName();
        address = mBluetoothDevice.getAddress();

        requestMacAddress();

        deviceDfu = (TextView)findViewById(R.id.device_Dfu);
        btnFileSelect = (Button)findViewById(R.id.dfuFileBtn);
        btnDfuUpdate = (Button)findViewById(R.id.dfuModeBtn);
        btnScanStart = (Button)findViewById(R.id.reconncetBtn);
        btmMsg = (TextView)findViewById(R.id.bottom_msg_dfu);
        fileNameTv = (TextView)findViewById(R.id.file_name);

        deviceDfu.setText("MacAddress: "+address);

        context = this;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mFileType = DfuService.TYPE_AUTO;
//        connect(mBluetoothDevice);

        progressDialog = new Dialog(DfuActivity.this,R.style.progress_dialog);
        progressDialog.setContentView(R.layout.dialog_loading);
        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        progressDialog.setCanceledOnTouchOutside(false);

        btnDfuUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d("State","startDfu");
//                onConnect();
                onUpload();
                btnDfuUpdate.setVisibility(View.GONE);
//                openFileChooser();
            }
        });

        btnScanStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DfuActivity.this,MainActivity.class);
                finish();
                startActivity(intent);
            }
        });

        btnFileSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onSelectFileClicked(v);
                btnFileSelect.setVisibility(View.GONE);
                btnDfuUpdate.setVisibility(View.VISIBLE);
            }
        });

    }

    public void onSelectFileClicked(final View view){
        Log.d("state","openFile");
        openFileChooser();
    }

    private void openFileChooser(){
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mFileTypeTmp == DfuService.TYPE_AUTO ? DfuService.MIME_TYPE_ZIP : DfuService.MIME_TYPE_OCTET_STREAM);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            // file browser has been found on the device
            startActivityForResult(intent, SELECT_FILE_REQ);
        } else {
            // there is no any file browser app, let's try to download one
//            final View customView = getLayoutInflater().inflate(R.layout.app_file_brower_item, null);
//            final ListView appsList = (ListView) customView.findViewById(android.R.id.list);
//            appsList.setAdapter(new FileBrowserAppsAdapter(this));
//            appsList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
//            appsList.setItemChecked(0, true);
//            new AlertDialog.Builder(this).setTitle(R.string.dfu_alert_no_filebrowser_title).setView(customView)
//                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(final DialogInterface dialog, final int which) {
//                            dialog.dismiss();
//                        }
//                    }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(final DialogInterface dialog, final int which) {
//                    final int pos = appsList.getCheckedItemPosition();
//                    if (pos >= 0) {
//                        final String query = getResources().getStringArray(R.array.dfu_app_file_browser_action)[pos];
//                        final Intent storeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(query));
//                        startActivity(storeIntent);
//                    }
//                }
//            }).show();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data){
        if (resultCode != RESULT_OK)
            return;

        switch (requestCode) {
            case SELECT_FILE_REQ: {
                // clear previous data
                mFileType = mFileTypeTmp;

                final Uri uri = data.getData();
                if (uri.getScheme().equals("file")) {
                    // the direct path to the file has been returned
                    final String path = uri.getPath();
                    final File file = new File(path);
                    mFilePath = path;

                    updateFileInfo(file.getName());
                }else if (uri.getScheme().equals("content")){
                    mFileStreamUri = uri;
                    // if application returned Uri for streaming, let's us it. Does it works?
                    // FIXME both Uris works with Google Drive app. Why both? What's the difference? How about other apps like DropBox?
                    final Bundle extras = data.getExtras();
                    if (extras != null && extras.containsKey(Intent.EXTRA_STREAM))
                        mFileStreamUri = extras.getParcelable(Intent.EXTRA_STREAM);

                    // file name and size must be obtained from Content Provider
                    final Bundle bundle = new Bundle();
                    bundle.putParcelable(EXTRA_URI, uri);
                    getLoaderManager().restartLoader(SELECT_FILE_REQ, bundle, this);
                }
                break;
            }
            default:
                break;
    }}

    private void updateFileInfo(final String fileName){
        fileNameTv.setText(fileName);
    }

    private void requestMacAddress(){  //请求mac地址 获取版本号

    }

    private final DfuProgressListener mDfuProgressListener = new DfuProgressListener() {
        @Override
        public void onDeviceConnecting(String deviceAddress) {
            Log.i("dfu", "onDeviceConnecting");
            btmMsg.setText("设备连接中。。");
            onConnect();
            progressDialog.show();
        }

        @Override
        public void onDeviceConnected(String deviceAddress) {
            Log.i("dfu", "onDeviceConnected");
            btmMsg.setText("连接成功！");
        }

        @Override
        public void onDfuProcessStarting(String deviceAddress) {
            Log.i("dfu", "onDfuProcessStarting");
            btmMsg.setText("设备进入DFU模式");
            progressDialog.dismiss();
        }

        @Override
        public void onDfuProcessStarted(String deviceAddress) {
            Log.i("dfu", "onDfuProcessStarted");
        }

        @Override
        public void onEnablingDfuMode(String deviceAddress) {
            Log.i("dfu", "onEnablingDfuMode");
            btmMsg.setText("DFU模式已启动！");

        }

        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            Log.i("dfu", "onProgressChanged");
            Log.i("dfu", "onProgressChanged" + percent);
            btmMsg.setText("升级进度："+percent+"%");
//            dfuDialogFragment.setProgress(percent);
        }

        @Override
        public void onFirmwareValidating(String deviceAddress) {
            Log.i("dfu", "onFirmwareValidating");
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            Log.i("dfu", "onDeviceDisconnecting");
            btmMsg.setText("正在断开连接。。");
        }

        @Override
        public void onDeviceDisconnected(String deviceAddress) {
            Log.i("dfu", "onDeviceDisconnected");
//            btmMsg.setText("连接中。。");
            btnDfuUpdate.setVisibility(View.GONE);
        }

        @Override
        public void onDfuCompleted(String deviceAddress) {
            Log.i("dfu", "onDfuCompleted");
            Toast.makeText(getApplicationContext(), "DFU升级完成，请重新连接设备！", Toast.LENGTH_SHORT).show();
            btmMsg.setText("DFU升级完成");
            btnScanStart.setVisibility(View.VISIBLE);
//            stopDfu();
//            dfuDialogFragment.getProgressBar().setIndeterminate(true);
            //升级成功，重新连接设备
        }

        @Override
        public void onDfuAborted(String deviceAddress) {
            Log.i("dfu", "onDfuAborted");
        }

        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            Log.i("dfu", "onError");
            progressDialog.dismiss();
//            btmMsg.setText("升级失败，请重新升级！");
//            Toast.makeText(getApplicationContext(), "升级失败，请重新点击升级。", Toast.LENGTH_SHORT).show();
        }
    };

    public void onUpload(){
//        if (isDfuServiceRunning()){
//
//        }
        String address,name;
        Intent intent = this.getIntent();
        mBluetoothDevice = intent.getParcelableExtra("device");
        address = mBluetoothDevice.getAddress();
//        name = mBluetoothDevice.getName();
        Log.d("address",address);

//        new DfuServiceInitiator(address).setDisableNotification(true)
//                .setZip(R.raw.nrfutil_dfu_2017_04_24).start(DfuActivity.this,DfuService.class);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
//        final SharedPreferences.Editor editor = preferences.edit();
//        editor.putString(PREFS_DEVICE_NAME, mBluetoothDevice.getName());
//        editor.putString(PREFS_FILE_NAME, fileNameTv.getText().toString());
//        editor.putString(PREFS_FILE_TYPE, mFileTypeView.getText().toString());
//        editor.putString(PREFS_FILE_SIZE, mFileSizeView.getText().toString());
//        editor.apply();

        new DfuServiceInitiator(address).setDisableNotification(true).setZip(mFileStreamUri, mFilePath).start(this,
                DfuService.class);

//        if (mFileType == DfuService.TYPE_AUTO)
//            starter.setZip(mFileStreamUri, mFilePath);
//        else {
//            starter.setBinOrHex(mFileType, mFileStreamUri, mFilePath).setInitFile(mInitFileStreamUri, mInitFilePath);
//        }
//        starter.start(this, DfuService.class);
    }

    private boolean isDfuServiceRunning() {
//        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
//        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            if (DfuService.class.getName().equals(service.service.getClassName())) {
//                return true;
//            }
//        }
        return false;
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (device.getName()==null){

                    }else if (device.getName().contains("DFU")){
                        dfuDevice = device;
                        Log.d("device",dfuDevice.getName());
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    }
                    nHandler.sendEmptyMessage(1);

                }
            });
        }
    };

    public final Handler nHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case 1:

            }
        }
    };

    private void onConnect(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.startLeScan(mLeScanCallback);

        mHandler = new Handler();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("DDDDD",dfuDevice.getName());
                mBluetoothGatt = dfuDevice.connectGatt(context,false,bluetoothGattCallback);
                new DfuServiceInitiator(dfuDevice.getAddress()).setDisableNotification(true)
                        .setZip(mFileStreamUri, mFilePath).start(getApplicationContext(),DfuService.class);
            }
        },2000);

    }

    BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status,int newState){
            super.onConnectionStateChange(gatt,status,newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                gatt.discoverServices();
                Log.d("Connected", "onConnected");
                mBluetoothGatt.discoverServices();
            }else {
                Log.d("Connected" ,"DisConnected: "+ status);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status){
            if (status == BluetoothGatt.GATT_SUCCESS){
                Log.d("State","ServicesDiscovered");
            }else{
                Log.d("State","SvcDiscoveredFailed: "+status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
            if (status == BluetoothGatt.GATT_SUCCESS){
                Log.d("State","CharRead");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic){
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,int status) {
            gatt.writeCharacteristic(characteristic);
        }

    };

    @Override
    protected void onResume() {
        DfuServiceListenerHelper.registerProgressListener(this, mDfuProgressListener);
        super.onResume();
    }

    @Override
    protected void onPause() {
        DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Uri uri = args.getParcelable(EXTRA_URI);
        return new CursorLoader(this, uri, null /* all columns, instead of projection */, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToNext()) {
			/*
			 * Here we have to check the column indexes by name as we have requested for all. The order may be different.
			 */
            final String fileName = data.getString(data.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)/* 0 DISPLAY_NAME */);
            final int fileSize = data.getInt(data.getColumnIndex(MediaStore.MediaColumns.SIZE) /* 1 SIZE */);
            String filePath = null;
            final int dataIndex = data.getColumnIndex(MediaStore.MediaColumns.DATA);
            if (dataIndex != -1)
                filePath = data.getString(dataIndex /* 2 DATA */);
            if (!TextUtils.isEmpty(filePath))
                mFilePath = filePath;

            updateFileInfo(fileName);
        } else {
            fileNameTv.setText(null);

            mFilePath = null;
            mFileStreamUri = null;
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        fileNameTv.setText(null);
        mFilePath = null;
        mFileStreamUri = null;

    }
}
