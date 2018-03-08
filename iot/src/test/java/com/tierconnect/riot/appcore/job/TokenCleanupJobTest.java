package com.tierconnect.riot.appcore.job;

import com.tierconnect.riot.appcore.utils.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by vizix on 6/29/16.
 */
public class TokenCleanupJobTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testInit() throws Exception {

    }

    @Test
    public void testExecute() throws Exception {
        JobUtils.startQuartz();
        TokenCleanupJob.init(Configuration.getProperty("cronSchedule.TokenCleanUp"));
    }
}