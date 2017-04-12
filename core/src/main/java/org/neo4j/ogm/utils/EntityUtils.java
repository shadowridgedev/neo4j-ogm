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
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.TypeCache;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.collections4.CollectionUtils;
import org.neo4j.ogm.context.EntityWrapper;
import org.neo4j.ogm.context.ProxyConfiguration;
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

        if (entity instanceof EntityWrapper) {
            EntityWrapper e1 = (EntityWrapper) entity;
            if (e1.getOgmNativeDbId() == null) {
                return Long.valueOf(-System.identityHashCode(entity));
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
        if (entity instanceof EntityWrapper) {
            return ((EntityWrapper) entity).getOgmNativeDbId();
        }
        return (Long) metaData.classInfo(entity).identityField().readProperty(entity);
    }

    public static void setIdentityId(MetaData metaData, Object entity, Long identity) {
        if (entity instanceof EntityWrapper) {
            ((EntityWrapper) entity).setOgmNativeDbId(identity);
        } else {
            ClassInfo classInfo = metaData.classInfo(entity);
            Field identityField = classInfo.getField(classInfo.identityField());
            FieldInfo.write(identityField, entity, identity);
        }
    }

    public static Object getWrapper(Object entity) {
        if (entity instanceof EntityWrapper) {
            return entity;
        }
            //        try {
//            EntityWrapper delegator = (EntityWrapper) buildProxy(entity.getClass(), new Class[]{EntityWrapper.class}).newInstance();
//            ProxyConfiguration.Interceptor interceptor = new ProxyConfiguration.Interceptor() {
//                @Override
//                public Object intercept(Object instance, Method method, Object[] arguments) throws Throwable {
//                    System.out.println("********************************");
//                    return method.invoke(instance, arguments);
//                }
//            };
//            ( (ProxyConfiguration) delegator ).$$_hibernate_set_interceptor( interceptor );
//            entity = delegator;
//        } catch (InstantiationException | IllegalAccessException e) {
//            e.printStackTrace();
//        }
            Class<?> proxy = new ByteBuddy()
                    .subclass(entity.getClass())
                    .implement(EntityWrapper.class)
                    .defineField("ogmNativeDbId", Long.class, Visibility.PUBLIC)
                    .method(isDeclaredBy(EntityWrapper.class))
                    .intercept(FieldAccessor.ofBeanProperty()/*FixedValue.value(1l)*/)
                    .method(not(isDeclaredBy(EntityWrapper.class)))
                    .intercept(InvocationHandlerAdapter.of(new ProxyInvocationHandler(entity)))
                    .make()
                    .load(entity.getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                    .getLoaded();
            try {
                EntityWrapper oi = (EntityWrapper) proxy.newInstance();
                oi.getOgmNativeDbId();
                entity = oi;
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
//        ((EntityWrapper)entity).setOgmNativeDbId(2L);
//        ((EntityWrapper)entity).getOgmNativeDbId();
            return entity;
        }

        public static class ProxyInvocationHandler implements InvocationHandler {

            private Object delegate;

            public ProxyInvocationHandler(Object user) {
                this.delegate = user;
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return method.invoke(delegate, args);
            }
        }


        private static final TypeCache<TypeCache.SimpleKey> CACHE =
                new TypeCache.WithInlineExpunction<TypeCache.SimpleKey>(TypeCache.Sort.SOFT);

    public static Class buildProxy(
            final Class persistentClass,
            final Class[] interfaces) {

//        Class<?> cls = new ByteBuddy()
////                .with(new NamingStrategy.SuffixingRandom("OGM"))
//                .subclass(persistentClass)
//                .implement(EntityWrapper.class, Delegator.class)
//                .defineField("ogmNativeDbId", Long.class, Visibility.PUBLIC)
//                .method(isDeclaredBy(EntityWrapper.class)).intercept(FieldAccessor.ofBeanProperty())
//                .method(isDeclaredBy(persistentClass)).intercept(InvocationHandlerAdapter.toField("target"))
//                .make()
//                .load(persistentClass.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
//                .getLoaded();
//        if (true) return cls;

        Set<Class<?>> key = new HashSet<Class<?>>();
        if ( interfaces.length == 1 ) {
            key.add( persistentClass );
        }
        key.addAll( Arrays.<Class<?>>asList( interfaces ) );

        return CACHE.findOrInsert(persistentClass.getClassLoader(), new TypeCache.SimpleKey(key), new Callable<Class<?>>() {
            @Override
            public Class<?> call() throws Exception {
                return new ByteBuddy()
                        .with(TypeValidation.DISABLED)
                        .with(new NamingStrategy.SuffixingRandom("HibernateProxy"))
                        .subclass(interfaces.length == 1 ? persistentClass : Object.class, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                        .implement((Type[]) interfaces)
                        .method(ElementMatchers.isVirtual().and(not(ElementMatchers.isFinalizer())))
                        .intercept(MethodDelegation.to(ProxyConfiguration.InterceptorDispatcher.class))
                        .method(ElementMatchers.nameStartsWith("$$_hibernate_").and(ElementMatchers.isVirtual()))
                        .intercept(SuperMethodCall.INSTANCE)
                        .defineField(ProxyConfiguration.INTERCEPTOR_FIELD_NAME, ProxyConfiguration.Interceptor.class, Visibility.PRIVATE)
                        .implement(ProxyConfiguration.class)
                        .intercept(FieldAccessor.ofField(ProxyConfiguration.INTERCEPTOR_FIELD_NAME).withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                        .make()
                        .load(persistentClass.getClassLoader())
                        .getLoaded();
            }
        }, CACHE);
    }
}
