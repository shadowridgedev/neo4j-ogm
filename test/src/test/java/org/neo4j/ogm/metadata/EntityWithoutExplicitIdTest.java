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

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.metadata.test.EntityWithoutExplicitId;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

//@Ignore
public class EntityWithoutExplicitIdTest {

	private Session session;
	private SessionFactory sessionFactory;

	@Before
	public void setUp() throws Exception {
		sessionFactory = new SessionFactory("org.neo4j.ogm.metadata.test");
		session = sessionFactory.openSession();
	}

	@After
	public void tearDown() throws Exception {
		sessionFactory.close();
	}

	@Test
	public void name() throws Exception {

		EntityWithoutExplicitId entity = new EntityWithoutExplicitId();
		entity.name = "aa";
		session.save(entity);
		session.clear();
		Collection<EntityWithoutExplicitId> entities = session.loadAll(EntityWithoutExplicitId.class);
		assertEquals(1, entities.size());

	}

}
