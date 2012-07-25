// Copyright (C) GridGain Systems Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache.distributed.dht;

/**
 * Exception thrown whenever entry is created for invalid partition.
 *
 * @author 2012 Copyright (C) GridGain Systems
 * @version 3.6.0c.13012012
 */
public class GridDhtInvalidPartitionException extends RuntimeException {
    /** Partition. */
    private final int part;

    /**
     * @param part Partition.
     * @param message Message.
     */
    public GridDhtInvalidPartitionException(int part, String message) {
        super(message);

        this.part = part;
    }

    /**
     * @return Partition.
     */
    public int partition() {
        return part;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return getClass() + " [part=" + part + ", msg=" + getMessage() + ']';
    }
}
