package com.polidea.rxandroidble.internal.util;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.os.Build;

import com.polidea.rxandroidble.internal.RxBleLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.inject.Inject;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

public class BleConnectionCompat {

    private final Context context;

    @Inject
    public BleConnectionCompat(Context context) {
        this.context = context;
    }

    public BluetoothGatt connectGatt(BluetoothDevice remoteDevice, boolean autoConnect, BluetoothGattCallback bluetoothGattCallback) {

        if (remoteDevice == null) {
            return null;
        }

        /**
         * Issue that caused a race condition mentioned below was fixed in 7.0.0_r1
         * https://android.googlesource.com/platform/frameworks/base/+/android-7.0.0_r1/core/java/android/bluetooth/BluetoothGatt.java#649
         * compared to
         * https://android.googlesource.com/platform/frameworks/base/+/android-6.0.1_r72/core/java/android/bluetooth/BluetoothGatt.java#739
         * issue: https://android.googlesource.com/platform/frameworks/base/+/d35167adcaa40cb54df8e392379dfdfe98bcdba2%5E%21/#F0
          */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N || !autoConnect) {
            return connectGattCompat(bluetoothGattCallback, remoteDevice, autoConnect);
        }

        /**
         * Some implementations of Bluetooth Stack have a race condition where autoConnect flag
         * is not properly set before calling connectGatt. That's the reason for using reflection
         * to set the flag manually.
         */

        try {
            RxBleLog.v("Trying to connectGatt using reflection.");
            Object iBluetoothGatt = getIBluetoothGatt(getIBluetoothManager());

            if (iBluetoothGatt == null) {
                RxBleLog.w("Couldn't get iBluetoothGatt object");
                return connectGattCompat(bluetoothGattCallback, remoteDevice, true);
            }

            BluetoothGatt bluetoothGatt = createBluetoothGatt(iBluetoothGatt, remoteDevice);

            if (bluetoothGatt == null) {
                RxBleLog.w("Couldn't create BluetoothGatt object");
                return connectGattCompat(bluetoothGattCallback, remoteDevice, true);
            }

            boolean connectedSuccessfully = connectUsingReflection(bluetoothGatt, bluetoothGattCallback, true);

            if (!connectedSuccessfully) {
                RxBleLog.w("Connection using reflection failed, closing gatt");
                bluetoothGatt.close();
            }

            return bluetoothGatt;
        } catch (NoSuchMethodException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException
                | InstantiationException
                | NoSuchFieldException exception) {
            RxBleLog.w(exception, "Error during reflection");
            return connectGattCompat(bluetoothGattCallback, remoteDevice, true);
        }
    }

    private BluetoothGatt connectGattCompat(BluetoothGattCallback bluetoothGattCallback, BluetoothDevice device, boolean autoConnect) {
        RxBleLog.v("Connecting without reflection");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return device.connectGatt(context, autoConnect, bluetoothGattCallback, TRANSPORT_LE);
        } else {
            return device.connectGatt(context, autoConnect, bluetoothGattCallback);
        }
    }

    private boolean connectUsingReflection(BluetoothGatt bluetoothGatt, BluetoothGattCallback bluetoothGattCallback, boolean autoConnect)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        RxBleLog.v("Connecting using reflection");
        setAutoConnectValue(bluetoothGatt, autoConnect);
        Method connectMethod = bluetoothGatt.getClass().getDeclaredMethod("connect", Boolean.class, BluetoothGattCallback.class);
        connectMethod.setAccessible(true);
        return (Boolean) (connectMethod.invoke(bluetoothGatt, true, bluetoothGattCallback));
    }

    @TargetApi(Build.VERSION_CODES.M)
    private BluetoothGatt createBluetoothGatt(Object iBluetoothGatt, BluetoothDevice remoteDevice)
            throws IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor bluetoothGattConstructor = BluetoothGatt.class.getDeclaredConstructors()[0];
        bluetoothGattConstructor.setAccessible(true);
        RxBleLog.v("Found constructor with args count = " + bluetoothGattConstructor.getParameterTypes().length);

        if (bluetoothGattConstructor.getParameterTypes().length == 4) {
            return (BluetoothGatt) (bluetoothGattConstructor.newInstance(context, iBluetoothGatt, remoteDevice, TRANSPORT_LE));
        } else {
            return (BluetoothGatt) (bluetoothGattConstructor.newInstance(context, iBluetoothGatt, remoteDevice));
        }
    }

    private Object getIBluetoothGatt(Object iBluetoothManager)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        if (iBluetoothManager == null) {
            return null;
        }

        Method getBluetoothGattMethod = getMethodFromClass(iBluetoothManager.getClass(), "getBluetoothGatt");
        return getBluetoothGattMethod.invoke(iBluetoothManager);
    }

    private Object getIBluetoothManager() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            return null;
        }

        Method getBluetoothManagerMethod = getMethodFromClass(bluetoothAdapter.getClass(), "getBluetoothManager");
        return getBluetoothManagerMethod.invoke(bluetoothAdapter);
    }

    private Method getMethodFromClass(Class<?> cls, String methodName) throws NoSuchMethodException {
        Method method = cls.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method;
    }

    private void setAutoConnectValue(BluetoothGatt bluetoothGatt, boolean autoConnect) throws NoSuchFieldException, IllegalAccessException {
        Field autoConnectField = bluetoothGatt.getClass().getDeclaredField("mAutoConnect");
        autoConnectField.setAccessible(true);
        autoConnectField.setBoolean(bluetoothGatt, autoConnect);
    }
}
