// Copyright (C) GridGain Systems Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache.distributed.dht.preloader;

import org.gridgain.grid.typedef.internal.*;

import java.io.*;
import java.util.*;

/**
 * Full partition map.
 *
 * @author 2012 Copyright (C) GridGain Systems
 * @version 3.6.0c.13012012
 */
public class GridDhtPartitionFullMap extends HashMap<UUID, GridDhtPartitionMap>
    implements Comparable<GridDhtPartitionFullMap>, Externalizable {
    /** Node ID. */
    private UUID nodeId;

    /** Node order. */
    private long nodeOrder;

    /** Update sequence number. */
    private long updateSeq;

    /**
     * @param nodeId Node ID.
     * @param nodeOrder Node order.
     * @param updateSeq Update sequence number.
     */
    public GridDhtPartitionFullMap(UUID nodeId, long nodeOrder, long updateSeq) {
        assert nodeId != null;
        assert nodeOrder > 0;
        assert updateSeq > 0;

        this.nodeId = nodeId;
        this.nodeOrder = nodeOrder;
        this.updateSeq = updateSeq;
    }

    /**
     * @param nodeId Node ID.
     * @param nodeOrder Node order.
     * @param updateSeq Update sequence number.
     * @param m Map to copy.
     * @param onlyActive If {@code true}, then only active partitions will be included.
     */
    public GridDhtPartitionFullMap(UUID nodeId, long nodeOrder, long updateSeq, Map<UUID, GridDhtPartitionMap> m,
        boolean onlyActive) {
        assert nodeId != null;
        assert updateSeq > 0;
        assert nodeOrder > 0;

        this.nodeId = nodeId;
        this.nodeOrder = nodeOrder;
        this.updateSeq = updateSeq;

        for (Map.Entry<UUID, GridDhtPartitionMap> e : m.entrySet()) {
            GridDhtPartitionMap part = e.getValue();

            if (onlyActive)
                put(e.getKey(), new GridDhtPartitionMap(part.nodeId(), part.updateSequence(), part, true));
            else
                put(e.getKey(), part);
        }
    }

    /**
     * @param m Map to copy.
     * @param updateSeq Update sequence.
     */
    public GridDhtPartitionFullMap(GridDhtPartitionFullMap m, long updateSeq) {
        super(m);

        nodeId = m.nodeId;
        nodeOrder = m.nodeOrder;
        this.updateSeq = updateSeq;
    }

    /**
     * Empty constructor required for {@link Externalizable}.
     */
    public GridDhtPartitionFullMap() {
        // No-op.
    }

    /**
     * @return {@code True} if properly initialized.
     */
    public boolean valid() {
        return nodeId != null && nodeOrder > 0;
    }

    /**
     * @return Node ID.
     */
    public UUID nodeId() {
        return nodeId;
    }

    /**
     * @return Node order.
     */
    public long nodeOrder() {
        return nodeOrder;
    }

    /**
     * @return Update sequence.
     */
    public long updateSequence() {
        return updateSeq;
    }

    /**
     * @param updateSeq New update sequence value.
     * @return Old update sequence value.
     */
    public long updateSequence(long updateSeq) {
        long old = this.updateSeq;

        assert updateSeq >= old : "Invalid update sequence [cur=" + old + ", new=" + updateSeq +
            ", partMap=" + toFullString() + ']';

        this.updateSeq = updateSeq;

        return old;
    }

    /** {@inheritDoc} */
    @Override public int compareTo(GridDhtPartitionFullMap o) {
        assert nodeId == null || (nodeOrder != o.nodeOrder && !nodeId.equals(o.nodeId)) ||
            (nodeOrder == o.nodeOrder && nodeId.equals(o.nodeId)): "Inconsistent node order and ID [id1=" + nodeId +
                ", order1=" + nodeOrder + ", id2=" + o.nodeId + ", order2=" + o.nodeOrder + ']';

        if (nodeId == null && o.nodeId != null)
            return -1;
        else if (nodeId != null && o.nodeId == null)
            return 1;
        else if (nodeId == null && o.nodeId == null)
            return 0;

        return nodeOrder < o.nodeOrder ? -1 : nodeOrder > o.nodeOrder ? 1 :
            updateSeq < o.updateSeq ? -1 : updateSeq == o.updateSeq ? 0 : 1;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        U.writeUuid(out, nodeId);

        out.writeLong(nodeOrder);
        out.writeLong(updateSeq);

        U.writeMap(out, this);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        nodeId = U.readUuid(in);

        nodeOrder = in.readLong();
        updateSeq = in.readLong();

        putAll(U.<UUID, GridDhtPartitionMap>readMap(in));
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        GridDhtPartitionFullMap other = (GridDhtPartitionFullMap)o;

        return other.nodeId.equals(nodeId) && other.updateSeq == updateSeq;
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return 31 * nodeId.hashCode() + (int)(updateSeq ^ (updateSeq >>> 32));
    }

    /**
     * @return Map string representation.
     */
    public String map2string() {
        Iterator<Map.Entry<UUID, GridDhtPartitionMap>> it = entrySet().iterator();

        if (!it.hasNext())
            return "{}";

        StringBuilder buf = new StringBuilder();

        buf.append('{');

        while(true) {
            Map.Entry<UUID, GridDhtPartitionMap> e = it.next();

            UUID nodeId = e.getKey();

            GridDhtPartitionMap partMap = e.getValue();

            buf.append(nodeId).append('=').append(partMap.toFullString());

            if (!it.hasNext())
                return buf.append('}').toString();

            buf.append(", ");
        }
    }

    /**
     * @return Full string representation.
     */
    public String toFullString() {
        return S.toString(GridDhtPartitionFullMap.class, this, "size", size(), "map", map2string());
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridDhtPartitionFullMap.class, this, "size", size());
    }
}
