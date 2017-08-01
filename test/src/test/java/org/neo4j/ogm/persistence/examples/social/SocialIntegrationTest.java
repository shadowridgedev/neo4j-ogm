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

package org.neo4j.ogm.persistence.examples.social;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.domain.social.Individual;
import org.neo4j.ogm.domain.social.Mortal;
import org.neo4j.ogm.domain.social.Person;
import org.neo4j.ogm.domain.social.User;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Luanne Misquitta
 */
public class SocialIntegrationTest extends MultiDriverTestClass {

    private static SessionFactory sessionFactory;

    private Session session;

    @BeforeClass
    public static void oneTimeSetUp() {
        sessionFactory = new SessionFactory(driver, "org.neo4j.ogm.domain.social");
    }

    @Before
    public void init() throws IOException {
        session = sessionFactory.openSession();
    }

    @After
    public void clearDatabase() {
        session.purgeDatabase();
    }

    /**
     * @see DATAGRAPH-594
     */
    @Test
    public void shouldFetchOnlyPeopleILike() {
        session.query("CREATE (p1:Person {name:'A'}) CREATE (p2:Person {name:'B'}) CREATE (p3:Person {name:'C'})" +
                " CREATE (p4:Person {name:'D'}) CREATE (p1)-[:LIKES]->(p2) CREATE (p1)-[:LIKES]->(p3) CREATE (p4)-[:LIKES]->(p1)", Collections.EMPTY_MAP);

        Person personA = session.loadAll(Person.class, new Filter("name", ComparisonOperator.EQUALS, "A")).iterator().next();
        assertThat(personA).isNotNull();
        assertThat(personA.getPeopleILike()).hasSize(2);

        Person personD = session.loadAll(Person.class, new Filter("name", ComparisonOperator.EQUALS, "D")).iterator().next();
        assertThat(personD).isNotNull();
        assertThat(personD.getPeopleILike()).hasSize(1);
        assertThat(personD.getPeopleILike().get(0)).isEqualTo(personA);
    }

    /**
     * @see DATAGRAPH-594
     */
    @Test
    public void shouldFetchFriendsInBothDirections() {
        session.query("CREATE (p1:Individual {name:'A'}) CREATE (p2:Individual {name:'B'}) CREATE (p3:Individual {name:'C'})" +
                " CREATE (p4:Individual {name:'D'}) CREATE (p1)-[:FRIENDS]->(p2) CREATE (p1)-[:FRIENDS]->(p3) CREATE (p4)-[:FRIENDS]->(p1)", Collections.EMPTY_MAP);

        Individual individualA = session.loadAll(Individual.class, new Filter("name", ComparisonOperator.EQUALS, "A")).iterator().next();
        assertThat(individualA).isNotNull();
        assertThat(individualA.getFriends()).hasSize(2);
    }

    /**
     * @see DATAGRAPH-594
     */
    @Test
    public void shouldFetchFriendsForUndirectedRelationship() {
        session.query("CREATE (p1:User {name:'A'}) CREATE (p2:User {name:'B'}) CREATE (p3:User {name:'C'})" +
                " CREATE (p4:User {name:'D'}) CREATE (p1)-[:FRIEND]->(p2) CREATE (p1)-[:FRIEND]->(p3) CREATE (p4)-[:FRIEND]->(p1)", Collections.EMPTY_MAP);

        User userA = session.loadAll(User.class, new Filter("name", ComparisonOperator.EQUALS, "A")).iterator().next();
        assertThat(userA).isNotNull();
        assertThat(userA.getFriends()).hasSize(3);

        User userB = session.loadAll(User.class, new Filter("name", ComparisonOperator.EQUALS, "B")).iterator().next();
        assertThat(userB).isNotNull();
        assertThat(userB.getFriends()).hasSize(1);
        assertThat(userB.getFriends().get(0)).isEqualTo(userA);

        User userD = session.loadAll(User.class, new Filter("name", ComparisonOperator.EQUALS, "D")).iterator().next();
        assertThat(userD).isNotNull();
        assertThat(userD.getFriends()).hasSize(1);
        assertThat(userD.getFriends().get(0)).isEqualTo(userA);
    }

    /**
     * @see DATAGRAPH-594
     */
    @Test
    public void shouldSaveUndirectedFriends() {
        User userA = new User("A");
        User userB = new User("B");
        User userC = new User("C");
        User userD = new User("D");

        userA.getFriends().add(userB);
        userA.getFriends().add(userC);
        userD.getFriends().add(userA);

        session.save(userA);
        session.save(userB);
        session.save(userC);
        session.save(userD);

        session.clear();

        userA = session.loadAll(User.class, new Filter("name", ComparisonOperator.EQUALS, "A")).iterator().next();
        assertThat(userA).isNotNull();
        assertThat(userA.getFriends()).hasSize(3);

        userB = session.loadAll(User.class, new Filter("name", ComparisonOperator.EQUALS, "B")).iterator().next();
        assertThat(userB).isNotNull();
        assertThat(userB.getFriends()).hasSize(1);
        assertThat(userB.getFriends().get(0).getName()).isEqualTo(userA.getName());

        userD = session.loadAll(User.class, new Filter("name", ComparisonOperator.EQUALS, "D")).iterator().next();
        assertThat(userD).isNotNull();
        assertThat(userD.getFriends()).hasSize(1);
        assertThat(userD.getFriends().get(0).getName()).isEqualTo(userA.getName());
    }

    /**
     * @see DATAGRAPH-594
     */
    @Test
    public void shouldSaveUndirectedFriendsInBothDirections() {
        Person userA = new Person("A");
        Person userB = new Person("B");

        userA.getPeopleILike().add(userB);
        userB.getPeopleILike().add(userA);

        session.save(userA);

        session.clear();
        userA = session.loadAll(Person.class, new Filter("name", ComparisonOperator.EQUALS, "A")).iterator().next();
        assertThat(userA).isNotNull();
        assertThat(userA.getPeopleILike()).hasSize(1);
        session.clear();
        userB = session.loadAll(Person.class, new Filter("name", ComparisonOperator.EQUALS, "B")).iterator().next();
        assertThat(userB).isNotNull();
        assertThat(userB.getPeopleILike()).hasSize(1);
    }

    /**
     * @see DATAGRAPH-594
     */
    @Test
    public void shouldSaveIncomingKnownMortals() {
        Mortal mortalA = new Mortal("A");
        Mortal mortalB = new Mortal("B");
        Mortal mortalC = new Mortal("C");
        Mortal mortalD = new Mortal("D");

        mortalA.getKnownBy().add(mortalB);
        mortalA.getKnownBy().add(mortalC);
        mortalD.getKnownBy().add(mortalA);

        session.save(mortalA);
        session.save(mortalB);
        session.save(mortalC);
        session.save(mortalD);

        session.clear();

        mortalA = session.loadAll(Mortal.class, new Filter("name", ComparisonOperator.EQUALS, "A")).iterator().next();
        assertThat(mortalA).isNotNull();
        assertThat(mortalA.getKnownBy()).hasSize(2);

        mortalB = session.loadAll(Mortal.class, new Filter("name", ComparisonOperator.EQUALS, "B")).iterator().next();
        assertThat(mortalB).isNotNull();
        assertThat(mortalB.getKnownBy()).isEmpty();

        mortalC = session.loadAll(Mortal.class, new Filter("name", ComparisonOperator.EQUALS, "C")).iterator().next();
        assertThat(mortalC).isNotNull();
        assertThat(mortalC.getKnownBy()).isEmpty();

        mortalD = session.loadAll(Mortal.class, new Filter("name", ComparisonOperator.EQUALS, "D")).iterator().next();
        assertThat(mortalD).isNotNull();
        assertThat(mortalD.getKnownBy()).hasSize(1);
        assertThat(mortalD.getKnownBy().iterator().next().getName()).isEqualTo("A");
    }

    /**
     * @see DATAGRAPH-594
     */
    @Test
    public void shouldFetchIncomingKnownMortals() {
        session.query("CREATE (m1:Mortal {name:'A'}) CREATE (m2:Mortal {name:'B'}) CREATE (m3:Mortal {name:'C'})" +
                " CREATE (m4:Mortal {name:'D'}) CREATE (m1)<-[:KNOWN_BY]-(m2) CREATE (m1)<-[:KNOWN_BY]-(m3) CREATE (m4)<-[:KNOWN_BY]-(m1)", Collections.EMPTY_MAP);

        Mortal mortalA = session.loadAll(Mortal.class, new Filter("name", ComparisonOperator.EQUALS, "A")).iterator().next();
        assertThat(mortalA).isNotNull();
        assertThat(mortalA.getKnownBy()).hasSize(2);

        Mortal mortalB = session.loadAll(Mortal.class, new Filter("name", ComparisonOperator.EQUALS, "B")).iterator().next();
        assertThat(mortalB).isNotNull();
        assertThat(mortalB.getKnownBy()).isEmpty();

        Mortal mortalC = session.loadAll(Mortal.class, new Filter("name", ComparisonOperator.EQUALS, "C")).iterator().next();
        assertThat(mortalC).isNotNull();
        assertThat(mortalC.getKnownBy()).isEmpty();

        Mortal mortalD = session.loadAll(Mortal.class, new Filter("name", ComparisonOperator.EQUALS, "D")).iterator().next();
        assertThat(mortalD).isNotNull();
        assertThat(mortalD.getKnownBy()).hasSize(1);
        assertThat(mortalD.getKnownBy().iterator().next().getName()).isEqualTo("A");
    }


    @Test
    public void shouldFetchFriendsUndirected() {

        User adam = new User("Adam");
        User daniela = new User("Daniela");
        User michal = new User("Michal");
        User vince = new User("Vince");

        adam.befriend(daniela);
        daniela.befriend(michal);
        michal.befriend(vince);

        session.save(adam);

        session.clear();
        adam = session.load(User.class, adam.getId());
        assertThat(adam.getFriends()).hasSize(1);

        daniela = session.load(User.class, daniela.getId());
        assertThat(daniela.getFriends()).hasSize(2);
        List<String> friendNames = new ArrayList<>();
        for (User friend : daniela.getFriends()) {
            friendNames.add(friend.getName());
        }
        assertThat(friendNames.contains("Adam")).isTrue();
        assertThat(friendNames.contains("Michal")).isTrue();

        session.clear();

        michal = session.load(User.class, michal.getId());
        assertThat(michal.getFriends()).hasSize(2);

        session.clear();
        vince = session.load(User.class, vince.getId());
        assertThat(vince.getFriends()).hasSize(1);
    }
}
