// Copyright (C) GridGain Systems Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache.distributed.dht;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.lang.utils.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.typedef.*;
import org.gridgain.grid.typedef.internal.*;
import org.gridgain.grid.util.future.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 *
 *
 * @author 2012 Copyright (C) GridGain Systems
 * @version 3.6.0c.13012012
 */
public final class GridDhtGetFuture<K, V> extends GridCompoundIdentityFuture<Collection<GridCacheEntryInfo<K, V>>>
    implements GridDhtFuture<Collection<GridCacheEntryInfo<K, V>>> {
    /** Logger reference. */
    private static final AtomicReference<GridLogger> logRef = new AtomicReference<GridLogger>();

    /** Message ID. */
    private long msgId;

    /** */
    private UUID reader;

    /** Reload flag. */
    private boolean reload;

    /** Context. */
    private GridCacheContext<K, V> cctx;

    /** Keys. */
    private LinkedHashMap<? extends K, Boolean> keys;

    /** Reserved partitions. */
    private Collection<GridDhtLocalPartition> parts = new GridLeanSet<GridDhtLocalPartition>(5);

    /** Future ID. */
    private GridUuid futId;

    /** Version. */
    private GridCacheVersion ver;

    /** Topology version .*/
    private long topVer;

    /** Transaction. */
    private GridCacheTxLocalEx<K, V> tx;

    private GridPredicate<? super GridCacheEntry<K, V>>[] filters;

    /** Logger. */
    private GridLogger log;

    /** Retries because ownership changed. */
    private Collection<Integer> retries = new GridLeanSet<Integer>();

    /**
     * Empty constructor required for {@link Externalizable}.
     */
    public GridDhtGetFuture() {
        // No-op.
    }

    /**
     * @param cctx Context.
     * @param msgId Message ID.
     * @param reader Reader.
     * @param keys Keys.
     * @param reload Reload flag.
     * @param tx Transaction.
     * @param topVer Topology version.
     * @param filters Filters.
     */
    public GridDhtGetFuture(
        GridCacheContext<K, V> cctx,
        long msgId,
        UUID reader,
        LinkedHashMap<? extends K, Boolean> keys,
        boolean reload,
        @Nullable GridCacheTxLocalEx<K, V> tx,
        long topVer,
        @Nullable GridPredicate<? super GridCacheEntry<K, V>>[] filters) {
        super(cctx.kernalContext(), CU.<GridCacheEntryInfo<K, V>>collectionsReducer());

        assert reader != null;
        assert cctx != null;
        assert !F.isEmpty(keys);

        this.reader = reader;
        this.cctx = cctx;
        this.msgId = msgId;
        this.keys = keys;
        this.reload = reload;
        this.filters = filters;
        this.tx = tx;
        this.topVer = topVer;

        futId = GridUuid.randomUuid();

        ver = tx == null ? cctx.versions().next() : tx.xidVersion();

        log = U.logger(ctx, logRef, GridDhtGetFuture.class);

        syncNotify(true);
    }

    /**
     * Initializes future.
     */
    void init() {
        map(keys);

        markInitialized();
    }

    /**
     * @return Keys.
     */
    Collection<? extends K> keys() {
        return keys.keySet();
    }

    /** {@inheritDoc} */
    @Override public Collection<Integer> invalidPartitions() {
        return retries;
    }

    /**
     * @return Future ID.
     */
    public GridUuid futureId() {
        return futId;
    }

    /**
     * @return Future version.
     */
    public GridCacheVersion version() {
        return ver;
    }

    /** {@inheritDoc} */
    @Override public boolean onDone(Collection<GridCacheEntryInfo<K, V>> res, Throwable err) {
        if (super.onDone(res, err)) {
            // Release all partitions reserved by this future.
            for (GridDhtLocalPartition part : parts)
                part.release();

            return true;
        }

        return false;
    }

    /**
     * @param keys Keys.
     */
    private void map(final LinkedHashMap<? extends K, Boolean> keys) {
        GridDhtFuture<Object> fut = cctx.dht().dhtPreloader().request(keys.keySet(), topVer);

        if (!F.isEmpty(fut.invalidPartitions()))
            retries.addAll(fut.invalidPartitions());

        add(new GridEmbeddedFuture<Collection<GridCacheEntryInfo<K, V>>, Object>(cctx.kernalContext(), fut,
            new GridClosure2<Object, Exception, Collection<GridCacheEntryInfo<K, V>>>() {
                @Override public Collection<GridCacheEntryInfo<K, V>> apply(Object o, Exception e) {
                    if (e != null) { // Check error first.
                        if (log.isDebugEnabled())
                            log.debug("Failed to request keys from preloader [keys=" + keys + ", err=" + e + ']');

                        onDone(e);
                    }

                    LinkedHashMap<K, Boolean> mappedKeys = new LinkedHashMap<K, Boolean>(keys.size());

                    // Assign keys to primary nodes.
                    for (Map.Entry<? extends K, Boolean> key : keys.entrySet()) {
                        int part = cctx.partition(key.getKey());

                        if (!retries.contains(part)) {
                            if (!map(key.getKey(), parts))
                                retries.add(part);
                            else
                                mappedKeys.put(key.getKey(), key.getValue());
                        }
                    }

                    // Add new future.
                    add(getAsync(mappedKeys));

                    // Finish this one.
                    return Collections.emptyList();
                }
            })
        );
    }

    /**
     * @param key Key.
     * @param parts Parts to map.
     * @return {@code True} if mapped.
     */
    private boolean map(K key, Collection<GridDhtLocalPartition> parts) {
        GridDhtLocalPartition part = topVer > 0 ?
            cache().topology().localPartition(cctx.partition(key), topVer, true) :
            cache().topology().localPartition(key, false);

        if (part == null)
            return false;

        if (!parts.contains(part)) {
            // By reserving, we make sure that partition won't be unloaded while processed.
            if (part.reserve()) {
                parts.add(part);

                return true;
            }
            else
                return false;
        }
        else
            return true;
    }

    /**
     * @param keys Keys to get.
     * @return Future for local get.
     */
    @SuppressWarnings( {"unchecked", "IfMayBeConditional"})
    private GridFuture<Collection<GridCacheEntryInfo<K, V>>> getAsync(final LinkedHashMap<? extends  K, Boolean> keys) {
        if (F.isEmpty(keys))
            return new GridFinishedFuture<Collection<GridCacheEntryInfo<K, V>>>(cctx.kernalContext(),
                Collections.<GridCacheEntryInfo<K, V>>emptyList());

        final Collection<GridCacheEntryInfo<K, V>> infos = new LinkedList<GridCacheEntryInfo<K, V>>();

        GridCompoundFuture<Boolean, Boolean> txFut = null;

        for (Map.Entry<? extends K, Boolean> k : keys.entrySet()) {
            while (true) {
                GridDhtCacheEntry<K, V> e = cache().entryExx(k.getKey(), topVer);

                try {
                    GridCacheEntryInfo<K, V> info = e.info();

                    // If entry is obsolete.
                    if (info == null)
                        continue;

                    // Register reader. If there are active transactions for this entry,
                    // then will wait for their completion before proceeding.
                    // TODO: What if any transaction we wait for actually removes this entry?
                    // TODO: In this case seems like we will be stuck with untracked near entry.
                    // TODO: To fix, check that reader is contained in the list of readers once
                    // TODO: again after the returned future completes - if not, try again.
                    // TODO: Also, why is info read before transactions are complete, and not after?
                    GridFuture<Boolean> f = k.getValue() ? e.addReader(reader, msgId) : null;

                    if (f != null) {
                        if (txFut == null)
                            txFut = new GridCompoundFuture<Boolean, Boolean>(cctx.kernalContext(), CU.boolReducer());

                        txFut.add(f);
                    }

                    infos.add(info);

                    break;
                }
                catch (GridCacheEntryRemovedException ignore) {
                    if (log.isDebugEnabled())
                        log.debug("Got removed entry when getting a DHT value: " + e);
                }
            }
        }

        if (txFut != null)
            txFut.markInitialized();

        GridFuture<Map<K, V>> fut;

        if (txFut == null || txFut.isDone()) {
            if (reload)
                fut = cache().reloadAllAsync(keys.keySet(), true, filters);
            else
                fut = tx == null ? cache().getAllAsync(keys.keySet(), filters) : tx.getAllAsync(keys.keySet(), filters);
        }
        else {
            // If we are here, then there were active transactions for some entries
            // when we were adding the reader. In that case we must wait for those
            // transactions to complete.
            fut = new GridEmbeddedFuture<Map<K, V>, Boolean>(
                txFut,
                new C2<Boolean, Exception, GridFuture<Map<K, V>>>() {
                    @Override public GridFuture<Map<K, V>> apply(Boolean b, Exception e) {
                        if (e != null)
                            throw new GridClosureException(e);

                        if (reload)
                            return cache().reloadAllAsync(keys.keySet(), true, filters);
                        else
                            return tx == null ? cache().getAllAsync(keys.keySet(), filters) :
                                tx.getAllAsync(keys.keySet(), filters);
                    }
                },
                cctx.kernalContext());
        }

        return new GridEmbeddedFuture<Collection<GridCacheEntryInfo<K, V>>, Map<K, V>>(cctx.kernalContext(), fut,
            new C2<Map<K, V>, Exception, Collection<GridCacheEntryInfo<K, V>>>() {
                @Override public Collection<GridCacheEntryInfo<K, V>> apply(Map<K, V> map, Exception e) {
                    if (e != null) {
                        onDone(e);

                        return Collections.emptyList();
                    }
                    else {
                        for (Iterator<GridCacheEntryInfo<K, V>> it = infos.iterator(); it.hasNext();) {
                            GridCacheEntryInfo<K, V> info = it.next();

                            V v = map.get(info.key());

                            if (v == null)
                                it.remove();
                            else
                                info.value(v);
                        }

                        return infos;
                    }
                }
            });
    }

    /**
     * @return DHT cache.
     */
    private GridDhtCache<K, V> cache() {
        return (GridDhtCache<K, V>)cctx.cache();
    }
}
