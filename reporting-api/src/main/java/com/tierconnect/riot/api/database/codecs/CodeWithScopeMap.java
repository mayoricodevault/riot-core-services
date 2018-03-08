package com.tierconnect.riot.api.database.codecs;

import org.bson.types.Code;

/**
 * A representation of the JavaScript Code with Scope BSON type.
 */
public class CodeWithScopeMap extends Code {

    private static final long serialVersionUID = -1711373886669268117L;

    private final MapResult scope;

    /**
     * Construct an instance.
     *
     * @param code  the code
     * @param scope the scope
     */
    public CodeWithScopeMap(final String code, final MapResult scope) {
        super(code);
        this.scope = scope;
    }

    /**
     * Gets the scope, which is is a mapping from identifiers to values, representing the scope in which the code should be evaluated.
     *
     * @return the scope
     */
    public MapResult getScope() {
        return scope;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        CodeWithScopeMap that = (CodeWithScopeMap) o;
        return scope != null ? scope.equals(that.scope) : that.scope == null;
    }

    @Override
    public int hashCode() {
        return getCode().hashCode() ^ scope.hashCode();
    }
}
