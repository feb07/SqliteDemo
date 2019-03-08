package com.feb.sqlitedemo;

import android.app.Application;


/**
 * Created by lilichun on 2019/3/7.
 */
public class MyApplication extends Application {
    private static MyApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static MyApplication getInstance() {
        return instance;
    }
}
