/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/29 2:04
 */

package com.xxxxx.xxxxxxxx.project.utility;

import org.springframework.cglib.beans.BeanMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectMapUtils {

    public static <T> Map<String, Object> objectToMap(T bean) {
        Map<String, Object> map = new HashMap<>();
        if (bean != null) {
            BeanMap beanMap = BeanMap.create(bean);
            for (Object key : beanMap.keySet()) {
                map.put(key.toString(), beanMap.get(key));
            }
        }
        return map;
    }

    public static <T> T mapToObject(Map<String, Object> map, Class<T> clazz)
            throws IllegalAccessException, InstantiationException {
        T bean = clazz.newInstance();
        BeanMap beanMap = BeanMap.create(bean);
        beanMap.putAll(map);
        return bean;
    }

    public static <T> List<Map<String, Object>> objectsToMaps(List<T> objList) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (objList != null && objList.size() > 0) {
            for (T bean : objList) {
                Map<String, Object> map = objectToMap(bean);
                list.add(map);
            }
        }
        return list;
    }

    public static <T> List<T> mapsToObjects(List<Map<String, Object>> maps, Class<T> clazz)
            throws InstantiationException, IllegalAccessException {
        List<T> list = new ArrayList<>();
        if (maps != null && maps.size() > 0) {
            for (Map<String, Object> map : maps) {
                T bean = mapToObject(map, clazz);
                list.add(bean);
            }
        }
        return list;
    }
}
