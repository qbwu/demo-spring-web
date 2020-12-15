/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/9/1 21:22
 */

package com.xxxxx.xxxxxxxx.project.cache.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class KryoRedisSerializer<T> implements RedisSerializer<T> {
    private static final Logger logger = LoggerFactory.getLogger(KryoRedisSerializer.class);

    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private final Boolean compression;

    private final Map<Class, Class<? extends Serializer>> defaultSerializers = new HashMap<>();

    private final List<Class> registeredClasses = new ArrayList<>();

    private final Class<T> clazz;

    // Kryo is not thread-safe
    private final ThreadLocal<Kryo> kryos = ThreadLocal.withInitial(
            () -> {
                Kryo kryo = new Kryo();
                kryo.setRegistrationRequired(false);
                // support cycle reference
                kryo.setReferences(true);

                defaultSerializers.forEach(kryo::addDefaultSerializer);
                return kryo;
            });

    public KryoRedisSerializer(Class<T> clazz, Boolean compression) {
        this.clazz = clazz;
        this.compression = compression;
    }

    public void addDefaultSerializer(Class type, Class<? extends Serializer> serializerClass) {
        defaultSerializers.put(type, serializerClass);
    }

    public void register(Class ... registeredClasses) {
        this.registeredClasses.addAll(Stream.of(registeredClasses).collect(Collectors.toList()));
    }

    @Override
    public byte[] serialize(T t) {
        if (t == null) {
            return EMPTY_BYTE_ARRAY;
        }
        Kryo kryo = kryos.get();
        try {
            Output output = new Output(512, 4096);

            ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
            if (compression) {
                output.setOutputStream(new DeflaterOutputStream(baos));
            } else {
                output.setOutputStream(baos);
            }

            kryo.writeObjectOrNull(output, t, clazz);
            output.close();
            return baos.toByteArray();
        } catch (Exception e) {
            logger.warn("Failed to serialize the object, class: {}, reason: {}",
                    clazz.getSimpleName(), e.getMessage(), e);
        }
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public T deserialize(byte[] bytes) {
        if (bytes == null || bytes.length <= 0) {
            return null;
        }
        Kryo kryo = kryos.get();
        try {
            Input input;
            if (compression) {
                input = new Input(512);
                input.setInputStream(new InflaterInputStream(new ByteArrayInputStream(bytes)));
            } else {
                input = new Input(bytes);
            }
            return kryo.readObjectOrNull(input, clazz);
        } catch (Exception e) {
            logger.warn("Failed to deserialize the object, class: {}, reason: {}",
                    clazz.getSimpleName(), e.getMessage(), e);
        }
        return null;
    }
}
