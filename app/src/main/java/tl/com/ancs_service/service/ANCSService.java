package tl.com.ancs_service.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.UUID;

import tl.com.ancs_service.util.ConstanceValue;

/**
 * Created by tl on 2018-9-27
 * 广播ancs服务
 */
public class ANCSService extends Service {

  public static final String TAG = "ANCSService";

  private BluetoothManager bluetoothManager;
  private BluetoothAdapter blueToothAdapter;
  private BluetoothLeAdvertiser bluetoothLeAdvertiser;
  private BluetoothGattServer gattServer;
  private BluetoothGattCharacteristic notificationCharacteristic;
  private ANCSBinder ancsBinder = new ANCSBinder();
  private BluetoothGattServerCallback bluetoothGattServerCallback;
  private boolean isConnected = false;


  public class ANCSBinder extends Binder {
    public ANCSService getANCSService() {
      return ANCSService.this;
    }
  }


  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return ancsBinder;
  }


  @Override
  public void onCreate() {
    super.onCreate();
    initGATTServer();
  }


  /**
   * 1.初始化BLE蓝牙广播Advertiser，配置指定UUID的服务
   */
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void initGATTServer() {

    AdvertiseSettings settings = new AdvertiseSettings.Builder()
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
        .setConnectable(true)
        .build();

    AdvertiseData advertiseData = new AdvertiseData.Builder()
        //    .addServiceUuid(new ParcelUuid(UUID.fromString(ConstanceValue.SERVICE_ANCS)))
        .setIncludeDeviceName(true)
        .setIncludeTxPowerLevel(true)
        .build();

    //通过UUID_SERVICE构建
    AdvertiseData scanResponseData = new AdvertiseData.Builder()
        .addServiceUuid(new ParcelUuid(UUID.fromString(ConstanceValue.SERVICE_ANCS)))
        .setIncludeTxPowerLevel(true)
        .build();

    //广播创建成功之后的回调
    AdvertiseCallback callback = new AdvertiseCallback() {
      @Override
      public void onStartSuccess(AdvertiseSettings settingsInEffect) {
        Log.d(TAG, "BLE advertisement added successfully");
        //showText("1. initGATTServer success");
        //println("1. initGATTServer success");
      }

      @Override
      public void onStartFailure(int errorCode) {
        Log.e(TAG, "Failed to add BLE advertisement, reason: " + errorCode);
        //showText("1. initGATTServer failure");
      }
    };

    bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    blueToothAdapter = bluetoothManager.getAdapter();

    //部分设备不支持Ble中心
    bluetoothLeAdvertiser = blueToothAdapter.getBluetoothLeAdvertiser();
    if (bluetoothLeAdvertiser == null) {
      Log.i(TAG, "BluetoothLeAdvertiser为null");
    }

    //初始化服务
    initServices(ANCSService.this);

    //开始广播
    if (bluetoothLeAdvertiser != null) {
      bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponseData, callback);
    }
  }


  /**
   * 初始化Gatt服务，主要是配置Gatt服务各种UUID
   *
   * @param context
   */
  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  private void initServices(Context context) {
    setBluetoothGattServerCallback();
    //创建GattServer服务器
    gattServer = bluetoothManager.openGattServer(context, bluetoothGattServerCallback);

    //这个指定的创建指定UUID的服务
    BluetoothGattService service = new BluetoothGattService(UUID.fromString
        (ConstanceValue.SERVICE_ANCS),
        BluetoothGattService.SERVICE_TYPE_PRIMARY);
    //添加指定UUID的可读characteristic
//    BluetoothGattCharacteristic characteristicRead = new BluetoothGattCharacteristic(
//        UUID.fromString(ConstanceValue.CHARACTERISTICS_NOTIFICATION_SOURCE),
//        BluetoothGattCharacteristic.PROPERTY_READ,
//        BluetoothGattCharacteristic.PERMISSION_READ);

// service.addCharacteristic(characteristicRead);


    //添加指定UUID的可写characteristic
    notificationCharacteristic = new BluetoothGattCharacteristic(UUID
        .fromString(ConstanceValue.CHARACTERISTICS_NOTIFICATION_SOURCE),
        BluetoothGattCharacteristic.PROPERTY_WRITE |
            BluetoothGattCharacteristic.PROPERTY_READ |
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
        BluetoothGattCharacteristic.PERMISSION_WRITE |
            BluetoothGattCharacteristic.PERMISSION_READ);
    //添加可读characteristic的descriptor
    BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID.fromString
        (ConstanceValue.DESCRIPTOR_CONFIG),
        BluetoothGattCharacteristic.PERMISSION_WRITE);
    notificationCharacteristic.addDescriptor(descriptor);
    notificationCharacteristic.setValue("notify");
    service.addCharacteristic(notificationCharacteristic);

    gattServer.addService(service);
    Log.e(TAG, "2. initServices ok");
  }


  /**
   * 蓝牙服务端回调
   */
  private void setBluetoothGattServerCallback() {
    bluetoothGattServerCallback = new BluetoothGattServerCallback() {
      //回拨，指示远程设备何时连接或断开连接。
      @Override
      public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
        Log.i(TAG, String.format("1.onConnectionStateChange：device name = %s, address = %s",
            device.getName(), device.getAddress()));
        Log.i(TAG, String.format("1.onConnectionStateChange：status = %s, newState =%s ", status,
            newState));
        super.onConnectionStateChange(device, status, newState);
        if (newState == BluetoothProfile.STATE_CONNECTED) {
          isConnected = true;
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
          isConnected = false;
        }
      }

      //指示是否已成功添加本地服务。
      @Override
      public void onServiceAdded(int status, BluetoothGattService service) {
        super.onServiceAdded(status, service);
        Log.i(TAG, String.format("onServiceAdded：status = %s", status));
      }

      //远程客户端已请求读取本地特征。
      @Override
      public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                              BluetoothGattCharacteristic characteristic) {
        Log.e(TAG, String.format("onCharacteristicReadRequest：device name = %s, address = %s",
            device.getName(), device.getAddress()));
        Log.e(TAG, String.format("onCharacteristicReadRequest：requestId = %s, offset = %s",
            requestId, offset));
        //响应客户端的请求
        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
            characteristic.getValue());
      }

      //远程客户端已请求写入本地特征。
      @Override
      public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                               BluetoothGattCharacteristic characteristic,
                                               boolean
                                                   preparedWrite, boolean responseNeeded, int
                                                   offset, byte[] requestBytes) {
        Log.e(TAG, String.format("3.onCharacteristicWriteRequest：device name = %s, address = " +
                "%s",
            device.getName(), device.getAddress()));
        Log.e(TAG, String.format("3.onCharacteristicWriteRequest：requestId = %s, " +
                "preparedWrite=%s, " +
                "responseNeeded=%s, offset=%s, value=%s", requestId, preparedWrite,
            responseNeeded,
            offset, requestBytes.toString()));

        //响应客户端的请求
        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
            requestBytes);

        //处理写入特征值请求
        onResponseToClient(requestBytes, device, requestId, characteristic);
      }

      //远程客户端已请求写入本地描述符。
      @Override
      public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                           BluetoothGattDescriptor descriptor, boolean
                                               preparedWrite, boolean responseNeeded, int
                                               offset,
                                           byte[] value) {
        Log.e(TAG, String.format("2.onDescriptorWriteRequest：device name = %s, address = %s",
            device.getName(), device.getAddress()));
        Log.e(TAG, String.format("2.onDescriptorWriteRequest：requestId = %s, preparedWrite = " +
                "%s, " +
                "responseNeeded = %s, offset = %s, value = %s,", requestId, preparedWrite,
            responseNeeded, offset, value.toString()));

        //响应客户端请求
        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
      }

      //远程客户端已请求读取本地描述符。
      @Override
      public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                          BluetoothGattDescriptor descriptor) {
        Log.e(TAG, String.format("onDescriptorReadRequest：device name = %s, address = %s",
            device
                .getName(), device.getAddress()));
        Log.e(TAG, String.format("onDescriptorReadRequest：requestId = %s", requestId));

        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
      }

      //将通知或指示发送到远程设备时调用回调。
      @Override
      public void onNotificationSent(BluetoothDevice device, int status) {
        super.onNotificationSent(device, status);
        Log.e(TAG, String.format("5.onNotificationSent：device name = %s, address = %s", device
            .getName(), device.getAddress()));
        Log.e(TAG, String.format("5.onNotificationSent：status = %s", status));
      }

      //表示给定设备连接的MTU的回调已更改。
      @Override
      public void onMtuChanged(BluetoothDevice device, int mtu) {
        super.onMtuChanged(device, mtu);
        Log.e(TAG, String.format("onMtuChanged：mtu = %s", mtu));
      }

      //执行此设备的所有挂起写操作。
      @Override
      public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
        super.onExecuteWrite(device, requestId, execute);
        Log.e(TAG, String.format("onExecuteWrite：requestId = %s", requestId));
      }
    };
  }

  //处理特征值写入请求
  private void onResponseToClient(byte[] reqeustBytes, BluetoothDevice device, int requestId,
                                  BluetoothGattCharacteristic characteristic) {
    Log.e(TAG, String.format("4.onResponseToClient：device name = %s, address = %s", device
        .getName(), device.getAddress()));
    Log.e(TAG, String.format("4.onResponseToClient：requestId = %s", requestId));

    String str = new String(reqeustBytes);
    notificationCharacteristic.setValue(str.getBytes());
    //告诉客户端特征值已更新(confirm: true表示从客户端请求确认（指示），false表示发送通知)
    gattServer.notifyCharacteristicChanged(device, notificationCharacteristic, false);
    Log.i(TAG, "4.响应：" + str);

  }


  //通知客户端特征值已更新
  public void upDate(BluetoothGattCharacteristic characteristic, String value) {
    characteristic.setValue(value.getBytes());
    //todo 需要确定是哪个连接的蓝牙设备
    gattServer.notifyCharacteristicChanged(gattServer.getConnectedDevices().get(0),
        characteristic, false);
  }


  //通知客户端特征值已更新test
  public void upDate() {
    notificationCharacteristic.setValue("11111111".getBytes());
    if (isConnected && bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER).size()
        > 0) {
      gattServer.notifyCharacteristicChanged(bluetoothManager.getConnectedDevices
          (BluetoothProfile.GATT).get(0), notificationCharacteristic, false);
    }
  }

}
