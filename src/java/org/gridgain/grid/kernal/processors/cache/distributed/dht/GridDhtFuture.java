// Copyright (C) GridGain Systems Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache.distributed.dht;

import org.gridgain.grid.*;

import java.util.*;

/**
 * Keys to retry.
 *
 * @author 2012 Copyright (C) GridGain Systems
 * @version 3.6.0c.13012012
 */
public interface GridDhtFuture<T> extends GridFuture<T> {
    /**
     * Node that future should be able to provide keys to retry before
     * it completes, so it's not necessary to wait till future is done
     * to get retry keys.
     *
     * @return Keys to retry because this node is no longer a primary or backup.
     */
    public Collection<Integer> invalidPartitions();
}
