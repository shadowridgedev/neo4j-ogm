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

package org.neo4j.ogm.metadata;

import static org.junit.Assert.*;

import org.junit.Test;
import org.neo4j.ogm.idsupport.EntityWithoutExplicitId;
import org.neo4j.ogm.utils.EntityUtils;

public class EntityWrapperTest {

	@Test
	public void shouldWrapObject() throws Exception {
		EntityWithoutExplicitId entity = new EntityWithoutExplicitId();
		entity.surname = "aa";

		EntityWithoutExplicitId wrapped = (EntityWithoutExplicitId) EntityUtils.getWrapper(entity, new MetaData("org.neo4j.ogm.metadata.test"));

		System.out.println("hc entity " + System.identityHashCode(entity));
		System.out.println("hc wrapped " + System.identityHashCode(wrapped));
		System.out.println("hc wrapped " + wrapped.hashCode());

		assertEquals("aa", wrapped.getSurname());
		wrapped.getClass().getFields()[0].get(entity);
	}

}
