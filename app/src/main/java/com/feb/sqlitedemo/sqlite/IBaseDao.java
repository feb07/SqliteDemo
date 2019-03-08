package com.feb.sqlitedemo.sqlite;

import java.util.List;

/**
 * Created by lilichun on 2019/3/7.
 */
public interface IBaseDao<T> {
    Long insert(T entity);

    List<T> query(T where);

    Integer update(T entity, T where);

    Integer delete(T entity);

    List<T> query(T where, String orderBy);

    List<T> query(T where, String orderBy, Integer page, Integer pageCount);
}
