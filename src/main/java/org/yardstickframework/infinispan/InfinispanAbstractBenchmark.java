/*
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package org.yardstickframework.infinispan;

import org.infinispan.commons.api.*;
import org.infinispan.manager.*;
import org.infinispan.notifications.*;
import org.infinispan.notifications.cachelistener.annotation.*;
import org.infinispan.notifications.cachelistener.event.*;
import org.yardstickframework.*;

import java.util.concurrent.*;

import static org.yardstickframework.BenchmarkUtils.*;

/**
 * Abstract class for Infinispan benchmarks.
 */
public abstract class InfinispanAbstractBenchmark extends BenchmarkDriverAdapter {
    /** Cache name. */
    private final String cacheName;

    /** Arguments. */
    protected final InfinispanBenchmarkArguments args = new InfinispanBenchmarkArguments();

    /** Node. */
    private InfinispanNode node;

    /** Cache. */
    protected BasicCache<Object, Object> cache;

    /** */
    private final CountDownLatch nodesStartedLatch = new CountDownLatch(1);

    /**
     * @param cacheName Cache name.
     */
    protected InfinispanAbstractBenchmark(String cacheName) {
        this.cacheName = cacheName;
    }

    /** {@inheritDoc} */
    @Override public void setUp(BenchmarkConfiguration cfg) throws Exception {
        super.setUp(cfg);

        jcommander(cfg.commandLineArguments(), args, "<infinispan-driver>");

        node = new InfinispanNode(args.clientMode());

        node.start(cfg);

        cache = node.cacheContainer().getCache(cacheName);

        assert cache != null;

        if (!args.clientMode())
            addListener();
    }

    /** {@inheritDoc} */
    @Override public void tearDown() throws Exception {
        node.stop();
    }

    /** {@inheritDoc} */
    @Override public String description() {
        return cfg.description().isEmpty() ?
            cfg.driverName() + args.description() + cfg.defaultDescription() : cfg.description();
    }

    /** {@inheritDoc} */
    @Override public String usage() {
        return BenchmarkUtils.usage(args);
    }

    /**
     * @throws Exception If failed.
     */
    private void addListener() throws Exception {
        ((Listenable)cache).addListener(new TopologyListener());

        if (!nodesStarted((EmbeddedCacheManager)node.cacheContainer())) {
            println(cfg, "Waiting for " + (args.nodes() - 1) + " nodes to start...");

            nodesStartedLatch.await();
        }
    }

    /**
     * @param cacheMgr Cache manager.
     * @return {@code True} if all nodes are started, {@code false} otherwise.
     */
    private boolean nodesStarted(EmbeddedCacheManager cacheMgr) {
        return cacheMgr.getMembers().size() >= args.nodes();
    }

    /** */
    @Listener
    private class TopologyListener {
        /**
         * @param evt Event.
         */
        @TopologyChanged
        public void onEvent(TopologyChangedEvent evt) {
            if (!evt.isPre() && nodesStarted(evt.getCache().getCacheManager()))
                nodesStartedLatch.countDown();
        }
    }

    /**
     * @param max Key range.
     * @return Next key.
     */
    protected int nextRandom(int max) {
        return ThreadLocalRandom.current().nextInt(max);
    }

    /**
     * @param min Minimum key in range.
     * @param max Maximum key in range.
     * @return Next key.
     */
    protected int nextRandom(int min, int max) {
        return ThreadLocalRandom.current().nextInt(max - min) + min;
    }
}
