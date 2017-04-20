/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */
package org.neo4j.ogm.utils;

import static net.bytebuddy.matcher.ElementMatchers.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.TypeCache;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import org.apache.commons.collections4.CollectionUtils;
import org.neo4j.ogm.metadata.ClassInfo;
import org.neo4j.ogm.metadata.FieldInfo;
import org.neo4j.ogm.metadata.MetaData;

/**
 * The utility methods here will all throw a <code>NullPointerException</code> if invoked with <code>null</code>.
 *
 * @author Luanne Misquitta
 */
public class EntityUtils {

    public static Long identity(Object entity, MetaData metaData) {

        if (entity instanceof MyProxyInterface) {
            MyProxyInterface e1 = (MyProxyInterface) entity;
            if (e1.getOgmNativeDbId() == null) {
                return Long.valueOf(-System.identityHashCode(e1.getProxied()));
            } else {
                return e1.getOgmNativeDbId();
            }
        }
        ClassInfo classInfo = metaData.classInfo(entity);
        Object id = classInfo.identityField().readProperty(entity);
        return (id == null ? -System.identityHashCode(entity) : (Long) id);
    }

    /**
     * Returns the full set of labels, both static and dynamic, if any, to apply to a node.
     */
    public static Collection<String> labels(Object entity, MetaData metaData) {
        ClassInfo classInfo = metaData.classInfo(entity);
        Collection<String> staticLabels = classInfo.staticLabels();
        FieldInfo labelFieldInfo = classInfo.labelFieldOrNull();
        if (labelFieldInfo != null) {
            Collection<String> labels = (Collection<String>) labelFieldInfo.readProperty(entity);
            return CollectionUtils.union(staticLabels, labels);
        }
        return staticLabels;
    }

    public static Long getEntityId(MetaData metaData, Object entity) {
        if (metaData.classInfo(entity).identityFieldOrNull() == null && entity instanceof MyProxyInterface) {
            return ((MyProxyInterface) entity).getOgmNativeDbId();
        }
        return (Long) metaData.classInfo(entity).identityField().readProperty(entity);
    }

    public static void setIdentity(MetaData metaData, Object entity, Long identity) {
        if (entity instanceof MyProxyInterface) {
            ((MyProxyInterface) entity).setOgmNativeDbId(identity);
        } else {
            ClassInfo classInfo = metaData.classInfo(entity);
            Field identityField = classInfo.getField(classInfo.identityField());
            FieldInfo.write(identityField, entity, identity);
        }
    }

    public static Object getWrapper(Object entity, MetaData metadata) {

        if (entity instanceof MyProxyInterface) {
            // already a proxy
            return entity;
        }

        // needs proxy only if not id field defined
        if (metadata.classInfo(entity) != null && metadata.classInfo(entity).identityFieldOrNull() != null) {
            return entity;
        }

        Class<?> wrappedClass = entity.getClass();
        Class<?> proxyClass = getWrapperClass(wrappedClass);
        MyProxyInterface proxy = null;
        try {
            proxy = (MyProxyInterface) proxyClass.newInstance();
            proxy.setProxied(entity);
            proxy.setInvocationHandler(new MyHandler());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return proxy;
    }

    private static Class<?> getWrapperClass(Class<?> wrappedClass) {
        Set<Class<?>> key = new HashSet<>();
        key.add(wrappedClass);

        return CACHE.findOrInsert(wrappedClass.getClassLoader(), new TypeCache.SimpleKey(key), new Callable<Class<?>>() {
            @Override
            public Class<?> call() throws Exception {
                return new ByteBuddy()
                        .subclass(wrappedClass)
                        .defineField("invocationHandler", MyHandler.class)
                        .defineField("proxied", Object.class)
                        .defineField("ogmNativeDbId", Long.class)
                        .implement(MyProxyInterface.class)
                        .intercept(FieldAccessor.ofBeanProperty())
                        .method(not(isDeclaredBy(MyProxyInterface.class)
//                        not((named("setInvocationHandler"))
//                        .or(named("setProxied"))
//                        .or(named("getProxied"))
//                        .or(named("getOgmNativeDbId"))
//                        .or(named("setOgmNativeDbId"))
                                        .or(isEquals())
                                        .or(isHashCode())
                                        .or(isClone())
                        ))
//                .intercept(MethodDelegation.toField("proxied"))
                        .intercept(InvocationHandlerAdapter.toField("invocationHandler"))
                        .make()
                        .load(wrappedClass.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                        .getLoaded();
            }
        }, CACHE);
    }

    public interface MyProxyInterface {
        void setInvocationHandler(MyHandler handler);

        Object getProxied();
        void setProxied(Object proxied);

        Long getOgmNativeDbId();
        void setOgmNativeDbId(Long ogmNativeDbId);

    }

    public static class MyHandler implements InvocationHandler {
        @Override
        public Object invoke(final Object o, final Method method, final Object[] objects) throws Throwable {
            return method.invoke(((MyProxyInterface) o).getProxied(), objects);
        }
    }

    private static final TypeCache<TypeCache.SimpleKey> CACHE =
            new TypeCache.WithInlineExpunction<TypeCache.SimpleKey>(TypeCache.Sort.SOFT);
}
