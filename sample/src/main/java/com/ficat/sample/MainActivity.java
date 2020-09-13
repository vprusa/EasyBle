package com.ficat.sample;

import android.Manifest;
import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.ficat.easyble.BleDevice;
import com.ficat.easyble.BleManager;
import com.ficat.easyble.gatt.bean.CharacteristicInfo;
import com.ficat.easyble.gatt.bean.ServiceInfo;
import com.ficat.easyble.gatt.callback.BleConnectCallback;
import com.ficat.easyble.gatt.callback.BleMtuCallback;
import com.ficat.easyble.gatt.callback.BleNotifyCallback;
import com.ficat.easyble.gatt.callback.BleRssiCallback;
import com.ficat.easyble.gatt.callback.BleWriteCallback;
import com.ficat.easyble.scan.BleScanCallback;
import com.ficat.easypermissions.EasyPermissions;
import com.ficat.easypermissions.RequestExecutor;
import com.ficat.easypermissions.bean.Permission;
import com.ficat.sample.adapter.ScanDeviceAdapter;
import com.ficat.sample.adapter.CommonRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
  private final static String TAG = "EasyBle";

  private final static int CHARACTERISTIC_READABLE = 101;
  private final static int CHARACTERISTIC_WRITABLE = 102;
  private final static int CHARACTERISTIC_NOTIFICATION = 103;

  private RecyclerView rv;
  private BleManager manager;
  private List<BleDevice> deviceList = new ArrayList<>();
  private ScanDeviceAdapter adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    initView();
    initBleManager();
    showDevicesByRv();
  }

  private void initView() {
    Button btnScan = findViewById(R.id.btn_scan);
    Button btnDisconnect = findViewById(R.id.btn_disconnect);
    Button btnNotify = findViewById(R.id.btn_notify);
    Button btnWrite = findViewById(R.id.btn_write);
    Button btnReadRssi = findViewById(R.id.btn_read_rssi);
    Button btnMtu = findViewById(R.id.btn_mtu);
    rv = findViewById(R.id.rv);

    btnScan.setOnClickListener(this);
    btnDisconnect.setOnClickListener(this);
    btnNotify.setOnClickListener(this);
    btnWrite.setOnClickListener(this);
    btnReadRssi.setOnClickListener(this);
    btnMtu.setOnClickListener(this);
  }

  private void initBleManager() {
    //check if this android device supports ble
    if (!BleManager.supportBle(this)) {
      return;
    }
    //open bluetooth without a request dialog
    BleManager.toggleBluetooth(true);

    BleManager.ScanOptions scanOptions = BleManager.ScanOptions
        .newInstance()
        .scanPeriod(8000)
        .scanDeviceName(null);

    BleManager.ConnectOptions connectOptions = BleManager.ConnectOptions
        .newInstance()
        .connectTimeout(12000);

    manager = BleManager
        .getInstance()
        .setScanOptions(scanOptions)
        .setConnectionOptions(connectOptions)
        .setLog(true, "TAG")
        .init(this.getApplication());
  }

  private void showDevicesByRv() {
    rv.setLayoutManager(new LinearLayoutManager(this));
    rv.addItemDecoration(new RecyclerView.ItemDecoration() {
      @Override
      public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.top = 3;
      }
    });
    SparseArray<int[]> res = new SparseArray<>();
    res.put(R.layout.item_rv_scan_devices, new int[]{R.id.tv_name, R.id.tv_address, R.id.tv_connection_state});
    adapter = new ScanDeviceAdapter(this, deviceList, res);
    adapter.setOnItemClickListener(new CommonRecyclerViewAdapter.OnItemClickListener() {
      @Override
      public void onItemClick(View itemView, int position) {
        manager.stopScan();
        manager.connect(deviceList.get(position), new BleConnectCallback() {
          @Override
          public void onStart(boolean startConnectSuccess, String info, BleDevice device) {
            Log.e(TAG, "start connecting = " + startConnectSuccess + "     info: " + info);
          }

          @Override
          public void onFailure(int failCode, String info, BleDevice device) {
            Toast.makeText(MainActivity.this, failCode == BleConnectCallback.FAIL_CONNECT_TIMEOUT ?
                "connect timeout" : info, Toast.LENGTH_SHORT).show();
          }

          @Override
          public void onConnected(BleDevice device) {
            adapter.notifyDataSetChanged();
          }

          @Override
          public void onDisconnected(String info, int status, BleDevice device) {
            adapter.notifyDataSetChanged();
          }
        });
      }
    });
    rv.setAdapter(adapter);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.btn_scan:
        if (!BleManager.isBluetoothOn()) {
          BleManager.toggleBluetooth(true);
        }
        EasyPermissions
            .with(this)
            .request(Manifest.permission.ACCESS_COARSE_LOCATION)
            .autoRetryWhenUserRefuse(true, null)
            .result(new RequestExecutor.ResultReceiver() {
              @Override
              public void onPermissionsRequestResult(boolean grantAll, List<Permission> results) {
                if (grantAll) {
                  if (!manager.isScanning()) {
                    startScan();
                  }
                } else {
                  Toast.makeText(MainActivity.this,
                      "Please go to settings to grant location permission manually",
                      Toast.LENGTH_LONG).show();
                  EasyPermissions.goToSettingsActivity(MainActivity.this);
                }
              }
            });
        break;
      case R.id.btn_disconnect:
        manager.disconnectAll();
        break;
      case R.id.btn_notify:
        testNotify();
        break;
      case R.id.btn_write:
        testWrite();
        break;
      case R.id.btn_read_rssi:
        testReadRssi();
        break;
      case R.id.btn_mtu:
        testSetMtu();
        break;
      default:
        break;
    }
  }

  private void startScan() {
    manager.startScan(new BleScanCallback() {
      @Override
      public void onLeScan(BleDevice device, int rssi, byte[] scanRecord) {
        for (BleDevice d : deviceList) {
          if (device.address.equals(d.address)) {
            return;
          }
        }
        deviceList.add(device);
        adapter.notifyDataSetChanged();
      }

      @Override
      public void onStart(boolean startScanSuccess, String info) {
        Log.e(TAG, "start scan = " + startScanSuccess + "   info: " + info);
        if (startScanSuccess) {
          deviceList.clear();
          adapter.notifyDataSetChanged();
        }
      }

      @Override
      public void onFinish() {
        Log.e(TAG, "scan finish");
      }
    });
  }

  private void testReadRssi() {
    if (manager.getConnectedDevices().size() <= 0) {
      Toast.makeText(MainActivity.this, "No connected devices", Toast.LENGTH_SHORT).show();
      return;
    }
    //we use the first connected device to test
    BleDevice device2 = manager.getConnectedDevices().get(0);
    manager.readRssi(device2, new BleRssiCallback() {
      @Override
      public void onRssi(int rssi, BleDevice bleDevice) {
        Toast.makeText(MainActivity.this, "Rssi: " + rssi, Toast.LENGTH_SHORT).show();
      }

      @Override
      public void onFailure(int failCode, String info, BleDevice device) {
        Toast.makeText(MainActivity.this, "read rssi fail: " + info, Toast.LENGTH_SHORT).show();
      }
    });
  }

  private void testSetMtu() {
    if (manager.getConnectedDevices().size() <= 0) {
      Toast.makeText(MainActivity.this, "No connected devices", Toast.LENGTH_SHORT).show();
      return;
    }
    //we use the first connected device to test
    BleDevice device3 = manager.getConnectedDevices().get(0);
    manager.setMtu(device3, 128, new BleMtuCallback() {
      @Override
      public void onMtuChanged(int mtu, BleDevice device) {
        Toast.makeText(MainActivity.this, "Request MTU success: " + mtu, Toast.LENGTH_SHORT).show();
      }

      @Override
      public void onFailure(int failCode, String info, BleDevice device) {
        Toast.makeText(MainActivity.this, "set MTU fail: " + info, Toast.LENGTH_SHORT).show();
      }
    });
  }

  private void testWrite() {
    if (manager.getConnectedDevices().size() <= 0) {
      Toast.makeText(MainActivity.this, "No connected devices", Toast.LENGTH_SHORT).show();
      return;
    }
    //we use the first connected device to test
    BleDevice device1 = manager.getConnectedDevices().get(0);
    //randomly finding a writable characteristic to test
    Map<String, String> notificationInfo1 = getSpecificServiceInfo(device1, CHARACTERISTIC_WRITABLE);
    for (Map.Entry<String, String> e : notificationInfo1.entrySet()) {
      manager.write(device1, e.getKey(), e.getValue(), "TestWriteData001".getBytes(), new BleWriteCallback() {
        @Override
        public void onWriteSuccess(byte[] data, BleDevice device) {
          Toast.makeText(MainActivity.this, "write success!   data:  " + new String(data), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onFailure(int failCode, String info, BleDevice device) {
          Toast.makeText(MainActivity.this, "write fail: " + info, Toast.LENGTH_SHORT).show();
        }
      });
      return;
    }
  }

  // https://www.rgagnon.com/javadetails/java-0026.html
  // TODO deal with this on ESP32's side
  public static class UnsignedByte {
    public static void main (String args[]) {
      byte b1 = 127;
      byte b2 = -128;
      byte b3 = -1;

      System.out.println(b1);
      System.out.println(b2);
      System.out.println(b3);
      System.out.println(unsignedByteToInt(b1));
      System.out.println(unsignedByteToInt(b2));
      System.out.println(unsignedByteToInt(b3));
    /*
    127
    -128
    -1
    127
    128
    255
    */
    }

    public static int unsignedByteToInt(byte b) {
      return (int) b & 0xFF;
    }
  }

  private void testNotify() {
    if (manager.getConnectedDevices().size() <= 0) {
      Toast.makeText(MainActivity.this, "No connected devices", Toast.LENGTH_SHORT).show();
      return;
    }
    //we use the first connected device to test
    BleDevice device = manager.getConnectedDevices().get(0);
    //randomly finding a characteristic supporting notification to test
    Map<String, String> notificationInfo = getSpecificServiceInfo(device, CHARACTERISTIC_NOTIFICATION);
    for (final Map.Entry<String, String> e : notificationInfo.entrySet()) {
      manager.notify(device, e.getKey(), e.getValue(), new BleNotifyCallback() {
        @Override
        public void onCharacteristicChanged(byte[] d, BleDevice device) {
          int c = d[1], x = UnsignedByte.unsignedByteToInt(d[2]), y = UnsignedByte.unsignedByteToInt(d[3]), z = UnsignedByte.unsignedByteToInt(d[4]), b1 = d[5], b2 = d[6];
          Toast.makeText(MainActivity.this, "rec notif data: " + "l: " + d.length + " C " + c + " X " + x + " Y " + y + " Z " + z, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onNotifySuccess(String notifySuccessUuid, BleDevice device) {
          Log.e(TAG, "notify success: " + notifySuccessUuid);
        }

        @Override
        public void onFailure(int failCode, String info, BleDevice device) {
          Toast.makeText(MainActivity.this, "set notify fail: " + info, Toast.LENGTH_SHORT).show();
        }
      });
      return;
    }
  }

  /**
   * randomly finding a characteristic supporting specific property ,and using the characteristic
   * to test like notify() or write()
   *
   * @return the map-value is the uuid of characteristic used for test,and the map-key is the
   * uuid of service that contains this characteristic
   */
  private Map<String, String> getSpecificServiceInfo(BleDevice device, int characteristicProperty) {
    Map<String, String> map = new HashMap<>();
    Map<ServiceInfo, List<CharacteristicInfo>> serviceInfo = manager.getDeviceServices(device);
    for (Map.Entry<ServiceInfo, List<CharacteristicInfo>> entry : serviceInfo.entrySet()) {
      String serviceUuid = entry.getKey().uuid;
      for (CharacteristicInfo charInfo : entry.getValue()) {
        boolean specificReadable = characteristicProperty == CHARACTERISTIC_READABLE && charInfo.readable;
        boolean specificWritable = characteristicProperty == CHARACTERISTIC_WRITABLE && charInfo.writable;
        boolean specificNotify = characteristicProperty == CHARACTERISTIC_NOTIFICATION && (charInfo.notify ||
            charInfo.indicative);
        if (specificReadable || specificWritable || specificNotify) {
          map.put(serviceUuid, charInfo.uuid);
        }
      }
    }
    return map;
  }
}
