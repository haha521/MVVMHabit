package me.goldze.mvvmhabit.base;

import android.view.KeyEvent;

public interface OnKeyHandler {

    boolean onKeyDown(int keyCode, KeyEvent event);

    boolean onKeyUp(int keyCode, KeyEvent event);

}
