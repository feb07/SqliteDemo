package com.feb.sqlitedemo;

import com.feb.sqlitedemo.sqlite.annotion.DBField;
import com.feb.sqlitedemo.sqlite.annotion.DBTable;

/**
 * Created by lilichun on 2019/3/7.
 */
@DBTable("user")
public class User {
    @DBField(value = "name",length = 255)
    public String name;

    @DBField(value = "age",length = 255)
    public Integer age;
}
