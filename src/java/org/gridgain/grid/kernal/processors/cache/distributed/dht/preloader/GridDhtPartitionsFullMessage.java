// Copyright (C) GridGain Systems Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache.distributed.dht.preloader;

import org.gridgain.grid.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;

import java.io.*;

/**
 * Information about partitions of all nodes in topology.
 *
 * @author 2012 Copyright (C) GridGain Systems
 * @version 3.6.0c.13012012
 */
public class GridDhtPartitionsFullMessage<K, V> extends GridDhtPartitionsAbstractMessage<K, V> {
    /** */
    @GridToStringInclude
    private GridDhtPartitionFullMap parts;

    /**
     * Required by {@link Externalizable}.
     */
    public GridDhtPartitionsFullMessage() {
        // No-op.
    }

    /**
     * @param id Exchange ID.
     * @param parts Partitions.
     */
    GridDhtPartitionsFullMessage(GridDhtPartitionExchangeId id, GridDhtPartitionFullMap parts) {
        super(id);

        assert parts != null;

        this.parts = parts;
    }

    /**
     * @return Local partitions.
     */
    public GridDhtPartitionFullMap partitions() {
        return parts;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);

        out.writeObject(parts);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        parts = (GridDhtPartitionFullMap)in.readObject();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridDhtPartitionsFullMessage.class, this, "partCnt", parts.size(), "super", super.toString());
    }
}
