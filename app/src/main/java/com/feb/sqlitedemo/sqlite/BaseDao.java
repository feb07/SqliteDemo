package com.feb.sqlitedemo.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.feb.sqlitedemo.sqlite.annotion.DBField;
import com.feb.sqlitedemo.sqlite.annotion.DBTable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by lilichun on 2019/3/7.
 */
public class BaseDao<T> implements IBaseDao<T> {

    private static final String TAG = BaseDao.class.getSimpleName();

    private SQLiteDatabase database;

    private Class<T> entity;

    private boolean isInit = false;//是否初始化

    private String tableName;//表名

    private HashMap<String, Field> cacheMap;//列名，成员变量之前的映射

    private ExecutorService service = Executors.newFixedThreadPool(100);

    long insertResult;
    int updateResult;
    int deleteResult;

    public synchronized boolean init(Class<T> entity, SQLiteDatabase database) {
        if (!isInit) {
            this.database = database;
            this.entity = entity;
            tableName = entity.getAnnotation(DBTable.class).value();
            if (!database.isOpen()) {
                return false;
            }
            if (!autoCreateTable()) {
                return false;
            }
            isInit = true;
        }

        initCacheMap();
        return isInit;
    }

    /**
     * 初始化
     */
    private void initCacheMap() {
        cacheMap = new HashMap<>();
        //根据实际表中的字段，映射，查表格
        String sql = "select * from " + tableName + " limit 1,0";
        Cursor cursor = database.rawQuery(sql, null);
        //得到字段名数组
        String[] columns = cursor.getColumnNames();
        Field[] fields = entity.getDeclaredFields();
        for (String column : columns) {
            Field result = null;
            for (Field field : fields) {
                if (column.equalsIgnoreCase(field.getAnnotation(DBField.class).value())) {
                    result = field;
                    break;
                }
            }
            cacheMap.put(column, result);
        }
        cursor.close();
    }

    /**
     * 自动创建table
     *
     * @return
     */
    private boolean autoCreateTable() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("CREATE TABLE IF NOT EXISTS ");
        stringBuffer.append(tableName + " ( ");
        Field[] fields = entity.getDeclaredFields();
        //TODO 未处理表存在，增加字段，删除字段的情况
        for (Field field : fields) {
            Class type = field.getType();
            if (type == String.class) {
                stringBuffer.append(field.getAnnotation(DBField.class).value());
                stringBuffer.append(" TEXT,");
            } else if (type == Long.class) {
                stringBuffer.append(field.getAnnotation(DBField.class).value());
                stringBuffer.append(" BIGINT,");
            } else if (type == Double.class) {
                stringBuffer.append(field.getAnnotation(DBField.class).value());
                stringBuffer.append(" DOUBLE,");
            } else if (type == Integer.class) {
                stringBuffer.append(field.getAnnotation(DBField.class).value());
                stringBuffer.append(" INTEGER,");
            } else if (type == byte[].class) {
                stringBuffer.append(field.getAnnotation(DBField.class).value());
                stringBuffer.append(" BLOB,");
            } else {
                //不支持的数据类型
                continue;
            }
        }
        if (stringBuffer.charAt(stringBuffer.length() - 1) == ',') {
            stringBuffer.deleteCharAt(stringBuffer.length() - 1);
        }
        stringBuffer.append(")");
        Log.d(TAG, "autoCreateTable:" + stringBuffer.toString());
        try {
            this.database.execSQL(stringBuffer.toString());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
        return true;
    }


    @Override
    public Long insert(T entity) {
        service.execute(new InsertThread(entity));
        return insertResult;
    }

    @Override
    public List<T> query(T where) {
        return query(where, null);
    }


    @Override
    public List<T> query(T where, String orderBy) {
        return query(where, null, null, null);
    }

    @Override
    public List<T> query(T where, String orderBy, Integer page, Integer pageCount) {
        List<T> list = null;
        Cursor cursor = null;
        try {
            String limit = null;
            if (page != null && pageCount != null) {
                int startIndex = --page;
                limit = (startIndex < 0 ? 0 : startIndex) + "," + pageCount;
            }

            if (where != null) {
                Map<String, String> whereMap = getStringValues(where);
                Condition condition = new Condition(whereMap);
                cursor = database.query(tableName, null, condition.whereClause, condition.whereArgs, null, null, orderBy, limit);
            } else {
                cursor = database.query(tableName, null, null, null, null, null, orderBy, limit);
            }

            // 将查询出来的表数据转成对象集合
            list = getDataList(cursor, where);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
        return list;
    }


    @Override
    public Integer update(T entity, T where) {
        service.execute(new UpdateThread(entity, where));
        return updateResult;
    }


    @Override
    public Integer delete(T entity) {
        service.execute(new DeleteThread(entity));
        return deleteResult;
    }


    /**
     * 插入
     */
    class InsertThread extends BaseSqlThread {

        public InsertThread(T entity) {
            super(entity);
        }

        @Override
        public void run() {
            super.run();
            ContentValues contentValues = getValues(entity);
            insertResult = database.insert(tableName, null, contentValues);
        }
    }


    /**
     * 通过游标，将表中数据转成对象集合
     */
    private List<T> getDataList(Cursor cursor, T where) throws IllegalAccessException, InstantiationException {
        if (cursor != null) {
            List<T> result = new ArrayList<>();
            Object item = null;
            // 遍历游标，获取表中一行行的数据
            while (cursor.moveToNext()) {
                // 创建对象
                if (where != null) {
                    item = where.getClass().newInstance();
                }
                // 遍历表字段，使用游标一个个取值，赋值给新创建的对象。
                Iterator<Map.Entry<String, Field>> iterator = cacheMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Field> entry = iterator.next();
                    // 找到表字段
                    String columnName = entry.getKey();
                    // 找到表字段对应的类属性
                    Field field = entry.getValue();

                    // 根据类属性类型，使用游标获取表中的值
                    Object val = null;
                    Class<?> fieldType = field.getType();
                    if (fieldType == String.class) {
                        val = cursor.getString(cursor.getColumnIndex(columnName));
                    } else if (fieldType == int.class || fieldType == Integer.class) {
                        val = cursor.getInt(cursor.getColumnIndex(columnName));
                    } else if (fieldType == double.class || fieldType == Double.class) {
                        val = cursor.getDouble(cursor.getColumnIndex(columnName));
                    } else if (fieldType == float.class || fieldType == Float.class) {
                        val = cursor.getFloat(cursor.getColumnIndex(columnName));
                    } else if (fieldType == byte[].class) {
                        val = cursor.getBlob(cursor.getColumnIndex(columnName));
                    }

                    // 反射给对象属性赋值
                    field.set(item, val);
                }
                // 将对象添加到集合中
                result.add((T) item);
            }
            return result;
        }
        return null;
    }

    /**
     * update
     */
    class UpdateThread extends Thread {
        T entity;
        T where;

        public UpdateThread(T entity, T where) {
            this.entity = entity;
            this.where = where;
        }

        @Override
        public void run() {
            super.run();
            try {
                ContentValues cv = getValues(entity);
                Map<String, String> whereMap = getStringValues(where);
                Condition condition = new Condition(whereMap);
                updateResult = database.update(tableName, cv, condition.whereClause, condition.whereArgs);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 删除
     */
    class DeleteThread extends BaseSqlThread {

        public DeleteThread(T entity) {
            super(entity);
        }


        @Override
        public void run() {
            super.run();
            try {
                Map<String, String> whereMap = getStringValues(entity);
                Condition condition = new Condition(whereMap);
                deleteResult = database.delete(tableName, condition.whereClause, condition.whereArgs);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    class BaseSqlThread extends Thread {
        protected T entity;

        public BaseSqlThread(T entity) {
            this.entity = entity;
        }
    }

    private ContentValues getValues(T entity) {
        ContentValues contentValues = new ContentValues();
        Iterator<Map.Entry<String, Field>> iterator = cacheMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Field> fieldEntry = iterator.next();
            Field field = fieldEntry.getValue();
            String key = fieldEntry.getKey();
            //反射拿到成员变量的值
            field.setAccessible(true);
            try {
                Object object = field.get(entity);
                Class type = field.getType();
                if (type == String.class) {
                    contentValues.put(key, (String) object);
                } else if (type == Long.class) {
                    contentValues.put(key, (Long) object);
                } else if (type == Double.class) {
                    contentValues.put(key, (Double) object);
                } else if (type == Integer.class) {
                    contentValues.put(key, (Integer) object);
                } else if (type == byte[].class) {
                    contentValues.put(key, (byte[]) object);
                } else {
                    continue;
                }

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return contentValues;
    }

    /**
     * 将对象中的属性转成键值对（列名--值）
     */
    private Map<String, String> getStringValues(T entity) throws IllegalAccessException {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Field> entry : cacheMap.entrySet()) {
            Object value = entry.getValue().get(entity);
            result.put(entry.getKey(), value == null ? "" : value.toString());
        }
        return result;
    }

    class Condition {
        String whereClause;
        String[] whereArgs;

        public Condition(Map<String, String> whereMap) {

            StringBuilder sb = new StringBuilder();
            List<String> list = new ArrayList<>();

            for (Map.Entry<String, String> entry : whereMap.entrySet()) {
                if (!TextUtils.isEmpty(entry.getValue())) {
                    sb.append("and " + entry.getKey() + "=? ");
                    list.add(entry.getValue());
                }
            }
            this.whereClause = sb.delete(0, 4).toString();
            this.whereArgs = list.toArray(new String[list.size()]);
        }
    }
}
