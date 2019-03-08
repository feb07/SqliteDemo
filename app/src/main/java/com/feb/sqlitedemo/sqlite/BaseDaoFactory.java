package com.feb.sqlitedemo.sqlite;

import android.database.sqlite.SQLiteDatabase;

import com.feb.sqlitedemo.MyApplication;

/**
 * Created by lilichun on 2019/3/7.
 */
public class BaseDaoFactory {
    private static final BaseDaoFactory ourInstance = new BaseDaoFactory();
    private String PATH;
    private SQLiteDatabase sqLiteDatabase;

    public static BaseDaoFactory getInstance() {
        return ourInstance;
    }

    private BaseDaoFactory() {
        PATH = MyApplication.getInstance().getCacheDir().getAbsolutePath() + "/app.db";
        sqLiteDatabase = SQLiteDatabase.openOrCreateDatabase(PATH, null);

    }

    public synchronized <T> BaseDao<T> getBaseDao(Class<T> entity) {
        BaseDao<T> baseDao = null;
        try {
            baseDao = BaseDao.class.newInstance();
            baseDao.init(entity, sqLiteDatabase);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return baseDao;
    }
}
