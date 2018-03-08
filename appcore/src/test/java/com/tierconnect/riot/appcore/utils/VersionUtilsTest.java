package com.tierconnect.riot.appcore.utils;


import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.runners.Parameterized.*;

/**
 * Created by achambi on 1/20/17.
 */
@RunWith(Parameterized.class)
public class VersionUtilsTest {

    private int versionNumber;

    private String versionName;

    public VersionUtilsTest(int versionNumber, String versionName) {
        this.versionNumber = versionNumber;
        this.versionName = versionName;
    }

    @Parameters(name = "Version Test {index}: with versionNumber=\"{0}\", versionName=\"{1}\"")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {30001, "3.0.1"},
                {30101, "3.1.1"},
                {30102, "3.1.2"},
                {30200, "3.2.0"},
                {30201, "3.2.1"},
                {30300, "3.3.0"},
                {30301, "3.3.1"},
                {30302, "3.3.2"},
                {30303, "3.3.3"},
                {30305, "3.3.5"},
                {30400, "3.4.0"},
                {30402, "3.4.2"},
                {40000, "4.0.0"},
                {40100, "4.1.0"},
                {40200, "4.2.0"},
                {4030001, "4.3.0_RC1"},
                {4030011, "4.3.0_RC11"},
                {4030012, "4.3.0_RC12"},
                {4030013, "4.3.0_RC13"},
                {4050001, "4.5.0_RC1"},
                {4050002, "4.5.0_RC2"}
        });
    }

    @Test
    public void getAppVersionString() throws Exception {
        Assert.assertEquals(versionName, VersionUtils.getAppVersion(versionNumber));
    }
}