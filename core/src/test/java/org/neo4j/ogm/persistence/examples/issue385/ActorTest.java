/*
 * Copyright (c) 2002-2016 "Neo Technology,"
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

package org.neo4j.ogm.persistence.examples.issue385;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.annotation.Transient;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;

/**
 * @author vince
 */
@Transient
public class ActorTest extends MultiDriverTestClass {

    private SessionFactory sessionFactory = new SessionFactory("org.neo4j.ogm.persistence.examples.issue385");

    private Actor actor;

    @Before
    public void init() {

        Movie movie = new Movie();
        movie.title=("Star Wars: Episode IV");

        actor = new Actor();
        actor.name=("Harrison Ford");

        Role role1 = new Role();
        role1.actor=(actor);
        role1.movie=(movie);
        role1.title=("Han Solo");

        Role role2 = new Role();
        role2.actor=(actor);
        role2.movie=(movie);
        role2.title=("Unknown");

        Collection collection = new ArrayList();
        collection.add(role1);
        collection.add(role2);

        actor.playedIn = collection;

        Assert.assertEquals(2, actor.playedIn.size());

        sessionFactory.openSession().save(actor);
    }

    @Test
    /**
     * @see issues/385 - reading back into Collection of REs from graph.
     */
    public void findActor() {
        Actor retrievedActor = sessionFactory.openSession().load(Actor.class, actor.id);
        Assert.assertNotNull(retrievedActor);
        Assert.assertEquals(2, retrievedActor.playedIn.size());
    }
}
