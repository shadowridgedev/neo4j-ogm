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
 *  conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.entity.io;

import java.lang.reflect.Array;
import java.util.*;

import org.neo4j.ogm.session.Utils;


/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public abstract class EntityAccess implements PropertyWriter, RelationalWriter {


    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Object merge(Class<?> parameterType, Object newValues, Object[] currentValues, Class elementType) {
        if (currentValues != null) {
            return merge(parameterType, newValues, Arrays.asList(currentValues), elementType);
        } else {
            return merge(parameterType, newValues, new ArrayList(), elementType);
        }
    }


    /**
     * Merges the contents of <em>newValues</em> with <em>currentValues</em> ensuring no duplicates and returns the
     * result as an instance of the specified concrete containerType or its default concrete implementation if
     * containerType is an interface.
     *
     * @param containerType The type of Collection or Array to return, which may be a concrete class or an interface
     * @param newValues The objects to merge into a Collection of the given containerType, which may not necessarily be
     *        of a type assignable from <em>containerType</em> already, e.g. merge of Set and List
     * @param currentValues The Collection to merge into, which may be <code>null</code> if a new container needs
     *        to be created
     * @param elementType  The type of the element in the Array or Collection
     * @return The result of the merge, as an instance of the specified concrete containerType or its default concrete
     *         implementation if containerType is an interface.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Object merge(Class<?> containerType, Object newValues, Collection currentValues, Class elementType) {

        //While we expect newValues to be an iterable, there are a couple of exceptions
        if (newValues != null) {
            //1. A primitive array cannot be cast directly to Iterable
            newValues = boxPrimitiveArray(newValues);

            //2. A char[] may come in as a String or an array of String[]
            newValues = stringToCharacterIterable(newValues, containerType, elementType);
        }

        if (containerType.isArray()) {
            Class type = containerType.getComponentType();
            List<Object> objects = new ArrayList<>(union((Collection) newValues, currentValues, elementType));

            Object array = Array.newInstance(type, objects.size());
            for (int i = 0; i < objects.size(); i++) {
                Array.set(array, i, objects.get(i));
            }
            return array;
        }

        // create the desired type of collection and use it for the merge
        return createCollection(containerType, (Collection) newValues, currentValues, elementType);

    }

    private static Collection<?> createCollection(Class<?> containerType, Collection collection, Collection hydrated, Class elementType) {
        if (Vector.class.isAssignableFrom(containerType)) {
            return new Vector<>(union(collection, hydrated, elementType));
        }
        if (List.class.isAssignableFrom(containerType)) {
            return new ArrayList<>(union(collection, hydrated, elementType));
        }
        if (SortedSet.class.isAssignableFrom(containerType)) {
            return new TreeSet<>(union(collection, hydrated, elementType));
        }
        if (Set.class.isAssignableFrom(containerType)) {
            return new LinkedHashSet<>(union(collection, hydrated, elementType));
        }
        /**
         * @see #385
         */
        if (Iterable.class.isAssignableFrom(containerType)) {
            return new LinkedHashSet<>(union(collection, hydrated, elementType));
        }

        throw new RuntimeException("Unsupported: " + containerType.getName());
    }

    public static Collection<Object> union(Collection newValues, Collection existingValues, Class elementType) {

        // no new values - return previously hydrated collection
        if (newValues == null || newValues.size() == 0) {
            return existingValues;
        }

        // no existing values - return a new ArrayList containing the new values
        if (existingValues==null || existingValues.size() == 0) {
            Collection<Object> result = new ArrayList<>(newValues.size());
            for (Object object : newValues) {
                result.add(Utils.coerceTypes(elementType, object));
            }
            return result;
        }

        // some new values and some existing values - combine them, preserving insertion order
        Collection<Object> result = new LinkedHashSet<>(newValues.size() + existingValues.size());

        addToCollection(existingValues, result, elementType);
        addToCollection(newValues, result, elementType);

        return result;
    }

    private static void addToCollection(Collection add, Collection<Object> addTo, Class elementType) {
        for (Object object : add) {
            addTo.add(Utils.coerceTypes(elementType, object));
		}
    }


    /**
     * Convert to an Iterable of Character if the value is a String
     * @param value the object, which may be a String, String[], Collection of String
     * @return List of Character if the value is a String, or the value unchanged
     */
    private static Object stringToCharacterIterable(Object value, Class parameterType, Class elementType) {
        boolean convertCharacters = false;
        if (value instanceof String) {
            char[] chars = ((String) value).toCharArray();
            List<Character> characters = new ArrayList<>(chars.length);
            for (char c : chars) {
                characters.add(c);
            }
            return characters;
        }

        if (parameterType.getComponentType() != null) {
            if (parameterType.getComponentType().equals(Character.class)) {
                convertCharacters = true;
            }
        }
        else {
            if (elementType == Character.class || elementType == char.class) {
                convertCharacters = true;
            }
        }

        if (value.getClass().isArray() &&  convertCharacters && value.getClass().getComponentType().equals(String.class)) {
            String[] strings = (String[]) value;
            List<Character> characters = new ArrayList<>(strings.length);
            for (String s : strings) {
                characters.add(s.toCharArray()[0]);
            }
            return characters;
        }

        if (value.getClass().isArray() && (elementType == String.class || elementType.isEnum())) {
            String[] strings = (String[]) value;
            return Arrays.asList(strings);
        }
        return value;
    }

    private static Object boxPrimitiveArray(Object value) {
        if (value.getClass().isArray() && value.getClass().getComponentType().isPrimitive()) {
            switch (value.getClass().getComponentType().toString()) {
                case "int":
                    int[] intArray = (int[]) value;
                    List<Integer> boxedIntList = new ArrayList<>(intArray.length);
                    for (int i : intArray) {
                        boxedIntList.add(i);
                    }
                    return boxedIntList;

                case "float":
                    float[] floatArray = (float[]) value;
                    List<Float> boxedFloatList = new ArrayList<>(floatArray.length);
                    for (float f : floatArray) {
                        boxedFloatList.add(f);
                    }
                    return boxedFloatList;

                case "long":
                    long[] longArray = (long[]) value;
                    List<Long> boxedLongList = new ArrayList<>(longArray.length);
                    for (long l : longArray) {
                        boxedLongList.add(l);
                    }
                    return boxedLongList;

                case "double":
                    double[] dblArray = (double[]) value;
                    List<Double> boxedDoubleList = new ArrayList<>(dblArray.length);
                    for (double d : dblArray) {
                        boxedDoubleList.add(d);
                    }
                    return boxedDoubleList;

                case "boolean":
                    boolean[] booleanArray = (boolean[]) value;
                    List<Boolean> boxedBooleanList = new ArrayList<>(booleanArray.length);
                    for (boolean b : booleanArray) {
                        boxedBooleanList.add(b);
                    }
                    return boxedBooleanList;

                case "char":
                    char[] charArray = (char[]) value;
                    List<Character> boxedCharList = new ArrayList<>(charArray.length);
                    for (char c : charArray) {
                        boxedCharList.add(c);
                    }
                    return boxedCharList;
            }
        }
        return value;
    }


}
