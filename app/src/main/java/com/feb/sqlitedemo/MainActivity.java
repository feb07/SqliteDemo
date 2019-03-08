package com.feb.sqlitedemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.feb.sqlitedemo.sqlite.BaseDao;
import com.feb.sqlitedemo.sqlite.BaseDaoFactory;
import com.feb.sqlitedemo.sqlite.IBaseDao;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    IBaseDao<User> baseDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        baseDao = BaseDaoFactory.getInstance().getBaseDao(User.class);

    }


    public void insert(View view) {
        long start = System.currentTimeMillis();
        User user = new User();
        user.name = "lily";
        Long insert = baseDao.insert(user);
        long end = System.currentTimeMillis() - start;
        Log.e(TAG, "耗时：" + end);
        Toast.makeText(getApplicationContext(), "插入了" + insert + "条数据", Toast.LENGTH_SHORT).show();
    }

    public void update(View view) {
        User user = new User();
        user.name = "lucy";
        User where = new User();
        where.name = "lily";

        Integer update = baseDao.update(user, where);
        Toast.makeText(getApplicationContext(), "修改了" + update + "条数据", Toast.LENGTH_SHORT).show();
    }

    public void delete(View view) {
        User user = new User();
        user.name = "lily";
        Integer delete = baseDao.delete(user);
        Toast.makeText(getApplicationContext(), "删除了" + delete + "条数据", Toast.LENGTH_SHORT).show();
    }

    public void query(View view) {
        List<User> list = baseDao.query(new User());
        int query = list == null ? 0 : list.size();
        Toast.makeText(getApplicationContext(), "查出了" + query + "条数据", Toast.LENGTH_SHORT).show();
        for (User user : list) {
            System.out.println(user.name);
        }
    }
}
