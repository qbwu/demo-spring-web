/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/9/13 0:11
 */

package com.xxxxx.xxxxxxxx.project.cache.serializer;

import com.xxxxx.xxxxxxxx.project.utility.framework.SpringApplicationContextHolder;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;

public class AutowireSerializer<T> extends FieldSerializer<T> {
    public AutowireSerializer(Kryo kryo, Class type) {
        super(kryo, type);
    }

    @Override
    public T read(Kryo kryo, Input input, Class<T> aClass) {
        T target = super.read(kryo, input, aClass);
        // Fill in the autowired fields
        SpringApplicationContextHolder.getContext()
                .getAutowireCapableBeanFactory().autowireBean(target);
        return target;
    }

    @Override
    protected void initializeCachedFields () {
        CachedField[] cachedFields = getFields();

        // Ignore the fields annotated with @Autowoired
        for (CachedField field : cachedFields) {
            if (isAutowired(field.getField())) {
                removedFields.add(field);
            }
        }
    }
    private boolean isAutowired(Field field) {
        return field.getAnnotation(Autowired.class) != null;
    }
}
