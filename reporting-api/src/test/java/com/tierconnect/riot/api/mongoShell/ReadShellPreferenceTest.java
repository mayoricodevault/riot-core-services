package com.tierconnect.riot.api.mongoShell;

import com.tierconnect.riot.api.mongoShell.ReadShellPreference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by achambi on 11/24/16.
 * Test To validate shell command Read preference.
 */
@RunWith(value = Parameterized.class)
public class ReadShellPreferenceTest {

    private String shellPreference;
    private String shellPreferenceShellCommand;
    private ReadShellPreference readShellPreference;

    @Parameterized.Parameters(name = "ReadPreference {index}: with ReadShellPreferenceValue=\"{0}\", " +
            "ReadShellPreferenceShellCommand=\"{1}\", serverShellAddressValid=\"{2}\"")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {
                        "primary",
                        "db.getMongo().setReadPref('primary');",
                        ReadShellPreference.primary()
                },
                {
                        "primaryPreferred",
                        "db.getMongo().setReadPref('primaryPreferred');",
                        ReadShellPreference.primaryPreferred()
                },
                {
                        "secondary",
                        "db.getMongo().setReadPref('secondary');",
                        ReadShellPreference.secondary()
                },
                {
                        "secondaryPreferred",
                        "db.getMongo().setReadPref('secondaryPreferred');",
                        ReadShellPreference.secondaryPreferred()
                },
                {
                        "nearest",
                       "db.getMongo().setReadPref('nearest');",
                        ReadShellPreference.nearest()
                }
        });
    }

    /**
     * Constructor to set data examples.
     * @param shellPreference the read preference [primary, primaryPreferred, secondary, secondaryPreferred, nearest]
     * @param shellPreferenceShellCommand the read preference in shell format: [
     *                                      db.getMongo().setReadPref('primary');
     *                                      db.getMongo().setReadPref('secondary');
     *                                      db.getMongo().setReadPref('secondaryPreferred');
     *                                      db.getMongo().setReadPref('nearest');
     *                                    ]
     * @param readShellPreference a instance of readShellPreference.
     */
    public ReadShellPreferenceTest(String shellPreference,
                                   String shellPreferenceShellCommand,
                                   ReadShellPreference readShellPreference) {
        this.shellPreference = shellPreference;
        this.shellPreferenceShellCommand = shellPreferenceShellCommand;
        this.readShellPreference = readShellPreference;
    }

    @Test
    public void valueOf() throws Exception {
        ReadShellPreference readShellPreference = ReadShellPreference.valueOf(shellPreference);
        assertEquals(readShellPreference.equals(this.readShellPreference), true);
    }

    @Test
    public void getName() throws Exception {
        assertEquals(readShellPreference.toString(), shellPreferenceShellCommand);
    }
}