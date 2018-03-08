package com.tierconnect.riot.commons.serializers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by achambi on 11/7/16.
 * Class to constants used to convert between format.
 */
public class Constants {
    /**
     * Regular expression string to convert a string  to date.
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    public static final String DATE_REG_EXP =
        "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}([A-Za-z]{0,3}"
            + "|([+|-]\\d{2}:\\d{2}))$";

    public static final Map<String, String> DATE_FORMAT_REGEXPS = Collections.unmodifiableMap(
        new HashMap<String, String>() {
			private static final long serialVersionUID = 1L;
	{
            put("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}([A-Za-z]{0,3}|([+|-]\\d{2}:\\d{2}))$",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            put("^[A-Za-z]{3}\\s[A-Za-z]{3}\\s\\d{2}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[A-Z]{3}\\s\\d{4}$",
                "E MMM dd HH:mm:ss z yyyy");
            put("^\\d{8}$", "yyyyMMdd");
            put("^\\d{1,2}-\\d{1,2}-\\d{4}$", "dd-MM-yyyy");
            put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}(\\s(A|P)M)$",
                "MM/dd/yyyy hh:mm:ss a");
            put("^\\d{4}-\\d{1,2}-\\d{1,2}$", "yyyy-MM-dd");
            put("^\\d{1,2}/\\d{1,2}/\\d{4}$", "MM/dd/yyyy");
            put("^\\d{4}/\\d{1,2}/\\d{1,2}$", "yyyy/MM/dd");
            put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}$", "dd MMM yyyy");
            put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}$", "dd MMMM yyyy");
            put("^\\d{12}$", "yyyyMMddHHmm");
            put("^\\d{8}\\s\\d{4}$", "yyyyMMdd HHmm");
            put("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}$", "dd-MM-yyyy HH:mm");
            put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy-MM-dd HH:mm");
            put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}$", "MM/dd/yyyy HH:mm");
            put("^\\d{1,2}/\\d{1,2}/\\d{2}\\s\\d{1,2}:\\d{2}$", "MM/dd/yy HH:mm");
            put("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy/MM/dd HH:mm");
            put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}$", "dd MMM yyyy HH:mm");
            put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}$", "dd MMMM yyyy HH:mm");
            put("^\\d{14}$", "yyyyMMddHHmmss");
            put("^\\d{8}\\s\\d{6}$", "yyyyMMdd HHmmss");
            put("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd-MM-yyyy HH:mm:ss");
            put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$", "yyyy-MM-dd HH:mm:ss");
            put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}.\\d{1,5}$",
                "yyyy-MM-dd HH:mm:ss.S");
            put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "MM/dd/yyyy HH:mm:ss");
            put("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$", "yyyy/MM/dd HH:mm:ss");
            put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd MMM yyyy HH:mm:ss");
            put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$",
                "dd MMMM yyyy HH:mm:ss");
            put("\\d+/\\d+/\\d{4}\\s+\\d+:\\d{2}:\\d{2}", "MM/dd/yyyy HH:mm:ss");
            put("\\d+/\\d+/\\d{4}\\s+\\d+:\\d{2}", "MM/dd/yyyy HH:mm");
            put("\\d+/\\d+/\\d{4}", "MM/dd/yyyy");
            put("\\d+/\\d+/\\d{4}\\s+\\d+:\\d{2}:\\d{2}\\s+((AM)|(PM))", "MM/dd/yyyy hh:mm:ss a");
            put("\\d{1,2}+/\\d{1,2}+/\\d{4}\\s+\\d{1,2}+:\\d{1,2}((AM)|(PM))", "dd/MM/yyyy hh:mma");
            put("\\d{1,2}+/\\d{1,2}+/\\d{2}", "MM/dd/yy");
            put("^\\d{1,2}\\/\\d{1,2}\\/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[A-Z]{3}$",
                "MM/dd/yyyy HH:mm:ss z");
            put("^\\d{1,2}\\/\\d{1,2}\\/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\sGMT-\\d{1,}$",
                "MM/dd/yyyy HH:mm:ss z");
            // put("^\\d{1,2}\\/\\d{1,2}\\/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[a-z|A-Z|\\/|_|\\s]{1,}$", "MM/dd/yyyy HH:mm:ss zzzz"); TODO review how to use this format
            put("^\\d{1,2}\\/\\d{1,2}\\/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[+|-]\\d{2}:\\d{2}$",
                "MM/dd/yyyy HH:mm:ss z");
            put("^\\d{1,2}\\/\\d{1,2}\\/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[+|-]\\d{4}$",
                "MM/dd/yyyy HH:mm:ss Z");
            put("^\\d{4}-\\d{1,2}-\\d{1,2}-\\d{1,2}.\\d{2}.\\d{2}.\\d{1,6}$",
                "yyyy-MM-dd-HH.mm.ss.S");
        }});
}
