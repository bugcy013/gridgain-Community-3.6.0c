// Copyright (C) GridGain Systems Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.closure;

import org.jetbrains.annotations.*;

/**
 * This enumeration defines different types of closure
 * processing by the closure processor.
 *
 * @author 2012 Copyright (C) GridGain Systems
 * @version 3.6.0c.13012012
 */
public enum GridClosurePolicy {
    /** Public execution pool. */
    PUBLIC_POOL,

    /** P2P execution pool. */
    P2P_POOL,

    /** System execution pool. */
    SYSTEM_POOL;

    /** Enum values. */
    private static final GridClosurePolicy[] VALS = values();

    /**
     * Efficiently gets enumerated value from its ordinal.
     *
     * @param ord Ordinal value.
     * @return Enumerated value.
     */
    @Nullable public static GridClosurePolicy fromOrdinal(int ord) {
        return ord >= 0 && ord < VALS.length ? VALS[ord] : null;
    }
}
