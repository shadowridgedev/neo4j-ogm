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

package org.neo4j.ogm.typeconversion;

import static org.assertj.core.api.Assertions.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.Test;
import org.neo4j.ogm.annotation.typeconversion.DateString;
import org.neo4j.ogm.metadata.ClassInfo;
import org.neo4j.ogm.metadata.FieldInfo;
import org.neo4j.ogm.metadata.MetaData;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class DateConversionTest {

    private static final MetaData metaData = new MetaData("org.neo4j.ogm.domain.convertible.date");
    private static final ClassInfo memoInfo = metaData.classInfo("Memo");
    SimpleDateFormat simpleDateISO8601format = new SimpleDateFormat(DateString.ISO_8601);

    @Test
    public void assertFieldDateConversionToISO8601FormatByDefault() {
        FieldInfo fieldInfo = memoInfo.propertyField("recorded");
        assertThat(fieldInfo.hasPropertyConverter()).isTrue();
        AttributeConverter attributeConverter = fieldInfo.getPropertyConverter();
        assertThat(attributeConverter.getClass().isAssignableFrom(DateStringConverter.class)).isTrue();
        assertThat(attributeConverter.toGraphProperty(new Date(0))).isEqualTo("1970-01-01T00:00:00.000Z");
    }

    @Test
    public void assertFieldDateConversionWithUserDefinedFormat() {
        FieldInfo fieldInfo = memoInfo.propertyField("actioned");
        assertThat(fieldInfo.hasPropertyConverter()).isTrue();
        AttributeConverter attributeConverter = fieldInfo.getPropertyConverter();
        assertThat(attributeConverter.getClass().isAssignableFrom(DateStringConverter.class)).isTrue();
        assertThat(attributeConverter.toGraphProperty(new Date(0))).isEqualTo("1970-01-01");
    }

    @Test
    public void assertFieldDateLongConversion() {
        FieldInfo fieldInfo = memoInfo.propertyField("closed");
        assertThat(fieldInfo.hasPropertyConverter()).isTrue();
        AttributeConverter attributeConverter = fieldInfo.getPropertyConverter();
        assertThat(attributeConverter.getClass().isAssignableFrom(DateLongConverter.class)).isTrue();
        Date date = new Date(0);
        Long value = (Long) attributeConverter.toGraphProperty(date);
        assertThat(value).isEqualTo(new Long(0));
    }

    @Test
    public void assertFieldCustomTypeConversion() {
        FieldInfo fieldInfo = memoInfo.propertyField("approved");
        assertThat(fieldInfo.hasPropertyConverter()).isTrue();
        AttributeConverter attributeConverter = fieldInfo.getPropertyConverter();
        assertThat(attributeConverter.toGraphProperty(new Date(1234567890123L))).isEqualTo("20090213113130");
    }

    @Test
    public void assertConvertingNullGraphPropertyWorksCorrectly() {
        FieldInfo methodInfo = memoInfo.propertyField("approved");
        assertThat(methodInfo.hasPropertyConverter()).isTrue();
        AttributeConverter attributeConverter = methodInfo.getPropertyConverter();
        assertThat(attributeConverter.toEntityAttribute(null)).isEqualTo(null);
    }

    @Test
    public void assertConvertingNullAttributeWorksCorrectly() {
        FieldInfo methodInfo = memoInfo.propertyField("approved");
        assertThat(methodInfo.hasPropertyConverter()).isTrue();
        AttributeConverter attributeConverter = methodInfo.getPropertyConverter();
        assertThat(attributeConverter.toGraphProperty(null)).isEqualTo(null);
    }

    /**
     * @see DATAGRAPH-550
     */
    @Test
    public void assertArrayFieldDateConversionToISO8601FormatByDefault() {//here
        simpleDateISO8601format.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date[] dates = new Date[]{new Date(0), new Date(20000)};
        FieldInfo fieldInfo = memoInfo.propertyField("escalations");
        assertThat(fieldInfo.hasPropertyConverter()).isTrue();
        AttributeConverter attributeConverter = fieldInfo.getPropertyConverter();
        assertThat(attributeConverter.getClass().isAssignableFrom(DateArrayStringConverter.class)).isTrue();
        String[] converted = (String[]) attributeConverter.toGraphProperty(dates);
        assertThat(converted[0].equals("1970-01-01T00:00:00.000Z") || converted[1].equals("1970-01-01T00:00:00.000Z")).isTrue();
        assertThat(converted[0].equals(simpleDateISO8601format.format(new Date(20000))) || converted[1].equals(simpleDateISO8601format.format(new Date(20000)))).isTrue();
    }

    /**
     * @see DATAGRAPH-550
     */
    @Test
    public void assertConvertingNullArrayGraphPropertyWorksCorrectly() {
        FieldInfo methodInfo = memoInfo.propertyField("escalations");
        assertThat(methodInfo.hasPropertyConverter()).isTrue();
        AttributeConverter attributeConverter = methodInfo.getPropertyConverter();
        assertThat(attributeConverter.toEntityAttribute(null)).isEqualTo(null);
    }

    /**
     * @see DATAGRAPH-550
     */
    @Test
    public void assertConvertingNullArrayAttributeWorksCorrectly() {
        FieldInfo methodInfo = memoInfo.propertyField("escalations");
        assertThat(methodInfo.hasPropertyConverter()).isTrue();
        AttributeConverter attributeConverter = methodInfo.getPropertyConverter();
        assertThat(attributeConverter.toGraphProperty(null)).isEqualTo(null);
    }

    /**
     * @see DATAGRAPH-550
     */
    @Test
    public void assertCollectionFieldDateConversionToISO8601FormatByDefault() {//here
        simpleDateISO8601format.setTimeZone(TimeZone.getTimeZone("UTC"));
        List<Date> dates = new ArrayList<>();
        dates.add(new Date(0));
        dates.add(new Date(20000));
        FieldInfo fieldInfo = memoInfo.propertyField("implementations");
        assertThat(fieldInfo.hasPropertyConverter()).isTrue();
        AttributeConverter attributeConverter = fieldInfo.getPropertyConverter();
        assertThat(attributeConverter.getClass().isAssignableFrom(DateCollectionStringConverter.class)).isTrue();
        String[] converted = (String[]) attributeConverter.toGraphProperty(dates);
        assertThat(converted[0].equals("1970-01-01T00:00:00.000Z") || converted[1].equals("1970-01-01T00:00:00.000Z")).isTrue();
        assertThat(converted[0].equals(simpleDateISO8601format.format(new Date(20000))) || converted[1].equals(simpleDateISO8601format.format(new Date(20000)))).isTrue();
    }


    /**
     * @see DATAGRAPH-550
     */
    @Test
    public void assertConvertingNullCollectionGraphPropertyWorksCorrectly() {
        FieldInfo methodInfo = memoInfo.propertyField("implementations");
        assertThat(methodInfo.hasPropertyConverter()).isTrue();
        AttributeConverter attributeConverter = methodInfo.getPropertyConverter();
        assertThat(attributeConverter.toEntityAttribute(null)).isEqualTo(null);
    }

    /**
     * @see DATAGRAPH-550
     */
    @Test
    public void assertConvertingNullCollectionAttributeWorksCorrectly() {
        FieldInfo methodInfo = memoInfo.propertyField("implementations");
        assertThat(methodInfo.hasPropertyConverter()).isTrue();
        AttributeConverter attributeConverter = methodInfo.getPropertyConverter();
        assertThat(attributeConverter.toGraphProperty(null)).isEqualTo(null);
    }
}
