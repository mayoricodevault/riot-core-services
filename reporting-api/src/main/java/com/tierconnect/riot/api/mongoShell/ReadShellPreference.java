package com.tierconnect.riot.api.mongoShell;


import com.tierconnect.riot.api.assertions.Assertions;

/**
 * A class that represents preferred replica set members to which a query or command can be sent.
 * <p>
 * Created by achambi on 11/24/16.
 */
public abstract class ReadShellPreference {

    /**
     * Gets the name of this read preference.
     *
     * @return the name
     */
    public abstract String getName();

    /**
     * Gets a read preference that forces read to the primary.
     *
     * @return ReadPreference which reads from primary only
     */
    public static ReadShellPreference primary() {
        return PRIMARY;
    }

    /**
     * Gets a read preference that forces reads to the primary if available, otherwise to a secondary.
     *
     * @return ReadPreference which reads primary if available.
     */
    static ReadShellPreference primaryPreferred() {
        return PRIMARY_PREFERRED;
    }

    /**
     * Gets a read preference that forces reads to a secondary.
     *
     * @return ReadPreference which reads secondary.
     */
    public static ReadShellPreference secondary() {
        return SECONDARY;
    }

    /**
     * Gets a read preference that forces reads to a secondary if one is available, otherwise to the primary.
     *
     * @return ReadPreference which reads secondary if available, otherwise from primary.
     */
    static ReadShellPreference secondaryPreferred() {
        return SECONDARY_PREFERRED;
    }

    /**
     * Gets a read preference that forces reads to a primary or a secondary.
     *
     * @return ReadPreference which reads nearest
     */
    static ReadShellPreference nearest() {
        return NEAREST;
    }

    static {
        PRIMARY = new ReadShellPreference.PrimaryReadPreference();
        SECONDARY = new ReadShellPreference.SecondaryReadPreference();
        SECONDARY_PREFERRED = new ReadShellPreference.SecondaryPreferredReadPreference();
        PRIMARY_PREFERRED = new ReadShellPreference.PrimaryPreferredReadPreference();
        NEAREST = new ReadShellPreference.NearestReadPreference();
    }

    /**
     * Preference to read from primary only. Cannot be combined with tags.
     */
    private static final class PrimaryReadPreference extends ReadShellPreference {
        private PrimaryReadPreference() {
        }

        @Override
        public String getName() {
            return "primary";
        }

        @Override
        public String toString() {
            return "db.getMongo().setReadPref('primary');";
        }

        @Override
        public boolean equals(final Object o) {
            return o != null && getClass() == o.getClass();
        }
    }

    /**
     * Preference to read from primary only. Cannot be combined with tags.
     */
    private static final class PrimaryPreferredReadPreference extends ReadShellPreference {
        private PrimaryPreferredReadPreference() {
        }

        @Override
        public String getName() {
            return "primaryPreferred";
        }

        @Override
        public String toString() {
            return "db.getMongo().setReadPref('primaryPreferred');";
        }

        @Override
        public boolean equals(final Object o) {
            return o != null && getClass() == o.getClass();
        }
    }

    /**
     * Preference to read from primary only. Cannot be combined with tags.
     */
    private static final class SecondaryReadPreference extends ReadShellPreference {
        private SecondaryReadPreference() {
        }

        @Override
        public String getName() {
            return "secondary";
        }

        @Override
        public String toString() {
            return "db.getMongo().setReadPref('secondary');";
        }

        @Override
        public boolean equals(final Object o) {
            return o != null && getClass() == o.getClass();
        }
    }

    /**
     * Preference to read from primary only. Cannot be combined with tags.
     */
    private static final class SecondaryPreferredReadPreference extends ReadShellPreference {
        private SecondaryPreferredReadPreference() {
        }

        @Override
        public String getName() {
            return "secondaryPreferred";
        }

        @Override
        public String toString() {
            return "db.getMongo().setReadPref('secondaryPreferred');";
        }

        @Override
        public boolean equals(final Object o) {
            return o != null && getClass() == o.getClass();
        }
    }

    /**
     * Preference to read from primary only. Cannot be combined with tags.
     */
    private static final class NearestReadPreference extends ReadShellPreference {
        private NearestReadPreference() {
        }

        @Override
        public String getName() {
            return "nearest";
        }

        @Override
        public String toString() {
            return "db.getMongo().setReadPref('nearest');";
        }

        @Override
        public boolean equals(final Object o) {
            return o != null && getClass() == o.getClass();
        }
    }

    /**
     * Creates a read preference from the given read preference name.
     *
     * @param name the name of the read preference
     * @return the read preference
     */
    public static ReadShellPreference valueOf(final String name) {

        Assertions.voidNotNull("Name", name);

        String nameToCheck = name.toLowerCase();

        if (nameToCheck.equals(PRIMARY.getName().toLowerCase())) {
            return PRIMARY;
        }
        if (nameToCheck.equals(SECONDARY.getName().toLowerCase())) {
            return SECONDARY;
        }
        if (nameToCheck.equals(SECONDARY_PREFERRED.getName().toLowerCase())) {
            return SECONDARY_PREFERRED;
        }
        if (nameToCheck.equals(PRIMARY_PREFERRED.getName().toLowerCase())) {
            return PRIMARY_PREFERRED;
        }
        if (nameToCheck.equals(NEAREST.getName().toLowerCase())) {
            return NEAREST;
        }

        throw new IllegalArgumentException("No match for read preference of " + name);
    }


    private static final ReadShellPreference PRIMARY;
    private static final ReadShellPreference PRIMARY_PREFERRED;
    private static final ReadShellPreference SECONDARY;
    private static final ReadShellPreference SECONDARY_PREFERRED;
    private static final ReadShellPreference NEAREST;

}


