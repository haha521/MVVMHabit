package me.goldze.mvvmhabit.utils;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v4.app.ActivityCompat;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class WIFIUtils {

    private BroadcastReceiver wifiScanReceiver;
    private WifiManager wifiManager;
    private Timer timer;
    private MyTimerTask task;
    private Activity activity;
    private String wifiName;

    public WIFIUtils(WifiManager wifiManager, Activity activity,String wifiName) {
        this.wifiManager = wifiManager;
        this.activity = activity;
        this.wifiName = wifiName;
    }

    public void startWifiListener() {
        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                boolean success = intent.getBooleanExtra(
                        WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    scanSuccess();
                } else {
                    // scan failure handling
                    scanFailure();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        activity.registerReceiver(wifiScanReceiver, intentFilter);

        task = new MyTimerTask();
        timer = new Timer();
        timer.schedule(task, 1000, 10000);
    }


    public boolean isLocationServiceEnable(Context context) {
        LocationManager locationManager = (LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            return true;
        }
        return false;
    }

    private void scanSuccess() {
        System.out.println("wifi扫描成功！！！！！");
        connectWifi();
    }

    private void connectWifi(){
        if (!wifiManager.isWifiEnabled()) {
            System.out.println("wifi未打开！！！！！");
            wifiManager.setWifiEnabled(true);
        }
        if (!isConnect()) {
            System.out.println("wifi未连接！！！！！");
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
            if (configuredNetworks != null && configuredNetworks.size() > 0) {
                WifiConfiguration wifiConfiguration = IsExsits(wifiName);
                if(wifiConfiguration==null){
//                    if(configuredNetworks.get(0).status==WifiConfiguration.Status.DISABLED){
//                        wifiManager.enableNetwork(configuredNetworks.get(0).networkId, true);
//                    }
                }else {
                    for (WifiConfiguration w:configuredNetworks
                    ) {
                        if(!w.SSID.equals(wifiConfiguration.SSID)&&w.status!= WifiConfiguration.Status.DISABLED){
                            wifiManager.disableNetwork(w.networkId);
                        }
                    }
//                    wifiManager.reconnect();
                    wifiManager.enableNetwork(wifiConfiguration.networkId, true);
                    wifiManager.reconnect();
                    System.out.println("重新连接wifi");
                }
            }
        }
    }

    private WifiConfiguration IsExsits(String SSID) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs)
        {
            if (existingConfig.SSID.equals("\""+SSID+"\""))
            {
                return existingConfig;
            }
        }
        return null;
    }

    private boolean isConnect() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        System.out.println(wifiInfo==null);
        if (wifiInfo != null) {
            System.out.println(wifiInfo);
            if(wifiInfo.getSSID().equals("\""+wifiName+"\"")){
                return true;
            }
            return false;
        }
        return false;
    }

    private void scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        System.out.println("wifi扫描失败！！！！！");
        List<ScanResult> results = wifiManager.getScanResults();
    }

    public void unRegisterReceiver(){
        if(task!=null){
            task.cancel();
        }
        if(timer!=null){
            timer.cancel();
        }
        task = null;
        timer = null;
        if(wifiScanReceiver!=null&&activity!=null){
            activity.unregisterReceiver(wifiScanReceiver);
        }
    }

    class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            try {
                System.out.println("启动扫描！！！！！");
                if (!wifiManager.isWifiEnabled()) {
                    System.out.println("wifi未打开！！！！！");
                    wifiManager.setWifiEnabled(true);
                }else {
                    wifiManager.startScan();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
