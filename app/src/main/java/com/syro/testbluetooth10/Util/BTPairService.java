package com.syro.testbluetooth10.Util;

/************************************
 * 蓝牙配对函数 *
 **************/

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

public class BTPairService {

    /**
     * 与设备配对 参考源码：platform/packages/apps/Settings.git
     * /Settings/src/com/android/settings/bluetooth/CachedBluetoothDevice.java
     */
    public static boolean createBond(Class btClass, BluetoothDevice btDevice)
            throws Exception {
        Method createBondMethod = btClass.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }

    /**
     * 与设备解除配对 参考源码：platform/packages/apps/Settings.git
     * /Settings/src/com/android/settings/bluetooth/CachedBluetoothDevice.java
     */
    public static boolean removeBond(Class btClass, BluetoothDevice btDevice)
            throws Exception {
        Method removeBondMethod = btClass.getMethod("removeBond");
        Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }

    public static boolean setPin(Class btClass, BluetoothDevice btDevice,
                                 String str) throws Exception {
        try {
            Method setPinMethod = btClass.getDeclaredMethod("setPin",
                    new Class[] {byte[].class});
            Boolean returnValue = (Boolean) setPinMethod.invoke(btDevice,
                    new Object[] {str.getBytes()});
        } catch (SecurityException e) {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;

    }

    // 取消用户输入
    public static boolean cancelPairingUserInput(Class btClass, BluetoothDevice device)
            throws Exception {
        Method cancelPairingUserInputMethod = btClass.getMethod("cancelPairingUserInput");
        // cancelBondProcess()
        Boolean returnValue = (Boolean) cancelPairingUserInputMethod.invoke(device);
        return returnValue.booleanValue();
    }

    // 取消配对
    public static boolean cancelBondProcess(Class btClass, BluetoothDevice device)
            throws Exception {
        Method cancelBondProcessBondMethod = btClass.getMethod("cancelBondProcess");
        Boolean returnValue = (Boolean) cancelBondProcessBondMethod.invoke(device);
        return returnValue.booleanValue();
    }

    /**
     *
     * @param classToShow
     */
    public static void printAllInform(Class classToShow) {
        try {
            // 取得所有方法
            Method[] hideMethod = classToShow.getMethods();
            int i = 0;
            for (; i < hideMethod.length; i++) {
                Log.e("SyroZhang", "[" +i + "]" + "Method name: " + hideMethod[i].getName());
            }
            // 取得所有常量
            Field[] allFields = classToShow.getFields();
            for (i = 0; i < allFields.length; i++) {
                Log.e("SyroZhang", "[" +i + "]" + "Field name: " + allFields[i].getName());
            }
        } catch (SecurityException e) {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}