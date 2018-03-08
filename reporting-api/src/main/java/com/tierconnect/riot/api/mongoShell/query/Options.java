package com.tierconnect.riot.api.mongoShell.query;

import com.tierconnect.riot.api.assertions.Assertions;
import com.tierconnect.riot.api.mongoShell.exception.NotImplementedException;

import java.util.Map;

/**
 * Created by achambi on 12/2/16.
 * Class with contains the basic options to "Find" method.
 */
public class Options {


    private int limit;
    private int skip;
    private String sort;

    private static final String SKIP = ".skip(%1$d)";
    private static final String LIMIT = ".limit(%1$d)";
    private static final String SORT = ".sort(%1$s)";

    public Options() {
        sort = null;
        skip = 0;
        limit = 0;
    }

    public Options(final String sort, final int skip, final int limit) {
        this.sort = sort;
        this.skip = skip;
        this.limit = limit;
    }

    /**
     * Sets the limit to apply.
     *
     * @param limit the limit, which may be null
     * @return this
     */
    public Options limit(final int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Sets the number of documents to skip.
     *
     * @param skip the number of documents to skip
     * @return this
     */
    public Options skip(final int skip) {
        this.skip = skip;
        return this;
    }

    /**
     * @param sort {@link Map}<{@link String},{@link Integer}> the sort criteria, which may be null.
     * @return this
     */
    public Options sort(final String sort) {
        this.sort = sort;
        return this;
    }

    public String getSort() {
        return sort;
    }

    @Override
    public String toString() {
        String findOptions = "";
        if (Assertions.isNotBlank(sort)) {
            findOptions = String.format(SORT, sort);
        }
        if (skip != 0) {
            findOptions += String.format(SKIP, skip);
        }
        if (limit != 0) {
            findOptions += String.format(LIMIT, limit);
        }
        return findOptions;
    }

    @SuppressWarnings("unused")
    public String toAggregate() {
        throw new NotImplementedException();
    }
}
