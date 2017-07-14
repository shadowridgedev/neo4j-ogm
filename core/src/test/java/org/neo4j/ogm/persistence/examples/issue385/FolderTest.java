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

import java.util.Arrays;

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
public class FolderTest extends MultiDriverTestClass {

    private SessionFactory sessionFactory = new SessionFactory("org.neo4j.ogm.persistence.examples.issue385");

    private Folder folder;

    @Before
    public void init() {

        Document doc1 = new Document();
        doc1.name = "doc1";

        Document doc2 = new Document();
        doc2.name = "doc2";

        folder = new Folder();
        folder.name = "folder";

        folder.documents = Arrays.asList(doc1, doc2);

        Assert.assertEquals(2, folder.documents.size());

        sessionFactory.openSession().save(folder);
    }

    @Test
    /**
     * @see issues/385 - reading back into Collection of REs from graph.
     */
    public void findFolder() {
        Folder retrieved = sessionFactory.openSession().load(Folder.class, folder.id);
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(2, retrieved.documents.size());
    }
}
