package com.tierconnect.riot.commons.utils;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fflores on 4/21/17.
 */
public class Timer {
    private static Timer instance;
    private Map<String, Time> times;

    public Timer() {
        this.times = new HashMap<>();
    }
//
//    /**
//     * Gets the instance of Timer.
//     *
//     * @return the instance of Timer
//     */
//    public static Timer getInstance() {
//        if (instance == null) {
//            synchronized (Timer.class) {
//                if (instance == null) {
//                    instance = new Timer();
//                }
//            }
//        }
//
//        return instance;
//    }

    /**
     * Start the timer for a specific label
     *
     * @param label the value of label
     */
    public Timer start(final String label) {
        this.start(label, System.currentTimeMillis());
        return this;
    }

    /**
     * Start the timer for a specific label
     *
     * @param label the value of label
     * @param value the value
     */
    public void start(final String label,
                      final long value) {
        Preconditions.checkNotNull(label, "label is null");

        Time time = this.times.get(label);
        if (time == null) {
            time = new Time();
        }

        time.setStart(value);
        this.times.put(label, time);
    }

    /**
     * Gets the value of start
     *
     * @param label the value of label
     * @return the value of start
     */
    public long getStart(final String label) {
        Preconditions.checkNotNull(label, "label is null");
        long start = 0;
        Time time = this.times.get(label);
        if (time != null) {
            start = time.getStart();
        }

        return start;
    }

    /**
     * Stop the timer for a specific label
     *
     * @param label the value of label
     */
    public void stop(final String label) {
        this.stop(label, System.currentTimeMillis());
    }

    /**
     * Stop the timer for a specific label
     *
     * @param label the value of label
     * @param value the value
     */
    public void stop(final String label,
                     final long value) {
        Preconditions.checkNotNull(label, "label is null");

        Time time = this.times.get(label);
        if (time == null) {
            time = new Time();
        }

        time.setEnd(value);
        this.times.put(label, time);
    }

    /**
     * Gets the value of end
     *
     * @param label the value of label
     * @return the value of end
     */
    public long getEnd(final String label) {
        Preconditions.checkNotNull(label, "label is null");
        long end = 0;
        Time time = this.times.get(label);
        if (time != null) {
            end = time.getEnd();
        }

        return end;
    }

    /**
     * Gets the value of total.
     *
     * @param label the value of label
     * @return the value of total (end - start)
     */
    public long getTotal(final String label) {
        Preconditions.checkNotNull(label, "label is null");
        long total = 0;
        Time time = this.times.get(label);
        if (time != null) {
            total = time.getTotal();
        }

        return total;
    }

    /**
     * Reset the times.
     */
    public void reset() {
        this.times.clear();
    }

    /**
     * Time class.
     */
    private static class Time {
        private long start;
        private long end;

        /**
         * Gets the vale of start.
         *
         * @return the value of start
         */
        public long getStart() {
            return start;
        }

        /**
         * Sets the value of start
         *
         * @param start the new value of start
         */
        public void setStart(long start) {
            this.start = start;
        }

        /**
         * Gets the value of end.
         *
         * @return the value of end
         */
        public long getEnd() {
            return end;
        }

        /**
         * Sets the value of end
         *
         * @param end the new value of end
         */
        public void setEnd(long end) {
            this.end = end;
        }

        /**
         * Gets the value of total
         *
         * @return the total
         */
        public long getTotal() {
            return end - start;
        }
    }
}
