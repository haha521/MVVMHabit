package me.goldze.mvvmhabit.base;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.functions.Consumer;
import me.goldze.mvvmhabit.R;
import me.goldze.mvvmhabit.utils.WIFIUtils;

import static android.view.View.generateViewId;


/**
 * 盛装Fragment的一个容器(代理)Activity
 * 普通界面只需要编写Fragment,使用此Activity盛装,这样就不需要每个界面都在AndroidManifest中注册一遍
 */
public class ContainerActivity extends RxAppCompatActivity {
    private static final String FRAGMENT_TAG = "content_fragment_tag";
    public static final String FRAGMENT = "fragment";
    public static final String BUNDLE = "bundle";
    private String wifiName = "yidatest";
    protected WeakReference<Fragment> mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = null;
        if (savedInstanceState != null) {
            fragment = fm.getFragment(savedInstanceState, FRAGMENT_TAG);
        }
        if (fragment == null) {
            fragment = initFromIntent(getIntent());
        }
        FragmentTransaction trans = getSupportFragmentManager()
                .beginTransaction();
        trans.replace(R.id.content, fragment);
        trans.commitAllowingStateLoss();
        mFragment = new WeakReference<>(fragment);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getSupportFragmentManager().putFragment(outState, FRAGMENT_TAG, mFragment.get());
    }

    protected Fragment initFromIntent(Intent data) {
        if (data == null) {
            throw new RuntimeException(
                    "you must provide a page info to display");
        }
        try {
            String fragmentName = data.getStringExtra(FRAGMENT);
            if (fragmentName == null || "".equals(fragmentName)) {
                throw new IllegalArgumentException("can not find page fragmentName");
            }
            Class<?> fragmentClass = Class.forName(fragmentName);
            Fragment fragment = (Fragment) fragmentClass.newInstance();
            Bundle args = data.getBundleExtra(BUNDLE);
            if (args != null) {
                fragment.setArguments(args);
            }
            return fragment;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("fragment initialization failed!");
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content);
        if (fragment instanceof BaseFragment) {
            if (!((BaseFragment) fragment).isBackPressed()) {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (isShouldHideKeyboard(v, ev)) {
                hideKeyboard(v.getWindowToken());
            }
        }
        return super.dispatchTouchEvent(ev);
    }


    private boolean isShouldHideKeyboard(View v, MotionEvent event) {
        if (v != null && (v instanceof EditText)) {
            int[] l = {0, 0};
            v.getLocationInWindow(l);
            int left = l[0],
                    top = l[1],
                    bottom = top + v.getHeight(),
                    right = left + v.getWidth();
            if (event.getX() > left && event.getX() < right
                    && event.getY() > top && event.getY() < bottom) {
                // 点击EditText的事件，忽略它。
                return false;
            } else {
                return true;
            }
        }
        // 如果焦点不是EditText则忽略，这个发生在视图刚绘制完，第一个焦点不在EditText上，和用户用轨迹球选择其他的焦点
        return false;
    }

    private void hideKeyboard(IBinder token) {
        if (token != null) {
            InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            im.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }


    private boolean isShouldHideKeyboard(View v) {
        if (v != null && (v instanceof EditText)) {
            int[] l = {0, 0};
            v.getLocationInWindow(l);
            int left = l[0],
                    top = l[1],
                    bottom = top + v.getHeight(),
                    right = left + v.getWidth();
            return true;
        }
        // 如果焦点不是EditText则忽略，这个发生在视图刚绘制完，第一个焦点不在EditText上，和用户用轨迹球选择其他的焦点
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(onKeyHandler!=null){
            onKeyHandler.onKeyDown(keyCode,event);
        }
        View v = getCurrentFocus();
        if (v!=null) {
            v.requestFocusFromTouch();
            if(isShouldHideKeyboard(v))hideKeyboard(v.getWindowToken());
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(onKeyHandler!=null){
            onKeyHandler.onKeyUp(keyCode,event);
        }
        return super.onKeyUp(keyCode, event);
    }

    private OnKeyHandler onKeyHandler;


    public OnKeyHandler getOnKeyHandler() {
        return onKeyHandler;
    }

    public void setOnKeyHandler(OnKeyHandler onKeyHandler) {
        this.onKeyHandler = onKeyHandler;
    }

    private WIFIUtils wifiUtils;

    @Override
    protected void onStart() {
        super.onStart();
        RxPermissions rxPermission = new RxPermissions(this);
        rxPermission.requestEach(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(new Consumer<Permission>() {
                    @Override
                    public void accept(Permission permission) {
                        if (permission.granted) {// 用户已经同意该权限
                            if(wifiUtils==null){
                                wifiUtils = new WIFIUtils((WifiManager) ContainerActivity.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE),ContainerActivity.this,wifiName);
                            }
                            if (!wifiUtils.isLocationServiceEnable(getApplicationContext())) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(ContainerActivity.this);
                                builder.setMessage("系统未打开位置信息服务，是否打开位置服务，以实现自动连接WiFi功能？");
                                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                        startActivityForResult(intent, 10000);
                                    }
                                });
                                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                AlertDialog dialog = builder.create();
                                dialog.show();
                                Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                                button.setTextSize(28);
                                Button button2 = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                                button2.setTextSize(28);
                                button.setTextColor(getResources().getColor(R.color.blue));
                                button2.setTextColor(getResources().getColor(R.color.blue));
                                try {
                                    //获取mAlert对象
                                    Field mAlert = AlertDialog.class.getDeclaredField("mAlert");
                                    mAlert.setAccessible(true);
                                    Object mAlertController = mAlert.get(dialog);
                                    //获取mMessageView并设置大小颜色
                                    Field mMessage = mAlertController.getClass().getDeclaredField("mMessageView");
                                    mMessage.setAccessible(true);
                                    TextView mMessageView = (TextView) mMessage.get(mAlertController);
//                mMessageView.setTextColor(Color.BLUE);
                                    mMessageView.setTextSize(30);
                                    //获取mTitleView并设置大小颜色
                                } catch (NoSuchFieldException e) {
                                    e.printStackTrace();
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                wifiUtils.startWifiListener();
                            }
                        } else {

                        }
                    }
                });
    }

    @Override
    protected void onStop() {
        if(wifiUtils!=null){
            wifiUtils.unRegisterReceiver();
        }
        super.onStop();
    }

    private OnTouchHandler onTouchHandler;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(onTouchHandler!=null){
            onTouchHandler.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==10000){
            if(resultCode==RESULT_OK&&wifiUtils!=null){
                wifiUtils.startWifiListener();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public String getWifiName() {
        return wifiName;
    }

    public void setWifiName(String wifiName) {
        this.wifiName = wifiName;
    }
}
