package com.tierconnect.riot.appcore.utils;

import java.util.Arrays;

/**
 * Created by agutierrez on 2/24/15.
 */
public class VersionUtils {

    /*Static Methods*/

    public static int[] getAppVersionInt(int intVersion) {
        if (intVersion > 99999) {
            intVersion = intVersion / 100;
        }
        int release = intVersion % 100;
        int minor = (intVersion / 100) % 100;
        int major = (intVersion / 10000);
        return new int[]{major, minor, release};
    }

    public static String getAppVersion(int intVersion) {
        int releaseCandidate = 0;
        if (intVersion > 99999) {
            releaseCandidate = intVersion % 100;
            intVersion = intVersion / 100;
        }
        int release = intVersion % 100;
        int minor = (intVersion / 100) % 100;
        int major = (intVersion / 10000);
        return major + "." + minor + "." + release + ((releaseCandidate != 0) ? "_RC" + releaseCandidate : "");
    }

    public static int[] getAppVersionInt(String stringVersion) {
        String[] parts = stringVersion.split("\\.");
        int release = Integer.parseInt(parts[2]);
        int minor = Integer.parseInt(parts[1]);
        int major = Integer.parseInt(parts[0]);
        return new int[]{major, minor, release};
    }

    public static String getAppVersionString(int version) {
        return getAppVersionString(getAppVersionInt(version));
    }


    public static String getAppVersionString(int[] versionArray) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < versionArray.length; i++) {
            s.append(versionArray[i]);
            if (i < versionArray.length - 1) {
                s.append(".");
            }
        }
        return s.toString();
    }
}
