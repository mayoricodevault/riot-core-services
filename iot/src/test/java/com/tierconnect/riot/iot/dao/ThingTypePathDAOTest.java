package com.tierconnect.riot.iot.dao;

import com.mysema.query.jpa.JPQLQuery;
import com.tierconnect.riot.appcore.core.BaseTestIOT;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;


/**
 * Created by achambi on 7/14/17.
 * test for ThingTypePathDAO
 */
@RunWith(MockitoJUnitRunner.class)
public class ThingTypePathDAOTest extends BaseTestIOT {

    @Mock
    private JPQLQuery query;

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {
    }
}