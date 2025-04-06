package org.eclipse.daanse.olap.api;

import java.util.List;

public non-sealed interface KeyIdentifierSegment extends IdentifierSegment {

    /**
     * Returns the key components, if this IdentifierSegment is a key. (That is, if
     * {@link #getQuoting()} returns {@link Quoting#KEY}.)
     *
     * Returns null otherwise.
     *
     * @return Components of key, or null if this IdentifierSegment is not a key
     */
    List<NameIdentifierSegment> getKeyParts();

}
