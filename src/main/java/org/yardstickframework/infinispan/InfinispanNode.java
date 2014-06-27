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

import org.apache.log4j.*;
import org.infinispan.*;
import org.infinispan.client.hotrod.*;
import org.infinispan.commons.api.*;
import org.infinispan.configuration.cache.*;
import org.infinispan.manager.*;
import org.infinispan.server.hotrod.*;
import org.infinispan.server.hotrod.configuration.*;
import org.infinispan.transaction.*;
import org.jboss.netty.channel.*;
import org.yardstickframework.*;

import java.net.*;

import static org.yardstickframework.BenchmarkUtils.*;

/**
 * Standalone Infinispan node.
 */
public class InfinispanNode implements BenchmarkServer {
    /** */
    public static final String INITIAL_HOSTS = "INFINISPAN_INITIAL_HOSTS";

    /** */
    private BasicCacheContainer cacheMgr;

    /** */
    private HotRodServer hotRodServer;

    /** Client mode. */
    private boolean clientMode;

    /** */
    public InfinispanNode() {
        // No-op.
    }

    /** */
    public InfinispanNode(boolean clientMode) {
        this.clientMode = clientMode;
    }

    /** {@inheritDoc} */
    @Override public void start(BenchmarkConfiguration cfg) throws Exception {
        configureLogging();

        InfinispanBenchmarkArguments args = new InfinispanBenchmarkArguments();

        jcommander(cfg.commandLineArguments(), args, "<infinispan-node>");

        if (clientMode)
            cacheMgr = new RemoteCacheManager();
        else {
            String hosts = cfg.customProperties().get(INITIAL_HOSTS);

            if (hosts != null && !hosts.isEmpty())
                System.setProperty("jgroups.tcpping.initial_hosts", hosts);

            DefaultCacheManager cacheMgr = new DefaultCacheManager(args.configuration());

            cache(args, "cache", cacheMgr);

            cache(args, "transactional", cacheMgr);

            cache(args, "queryCache", cacheMgr);

            this.cacheMgr = cacheMgr;

            startHotRodServer(cfg, args, cacheMgr);

            println(cfg, "Infinispan node started.");
        }
    }

    /**
     * @param args Arguments.
     * @param cacheName Cache name.
     * @param cacheMgr Default cache manager.
     * @return Cache.
     */
    private Cache<Object, Object> cache(InfinispanBenchmarkArguments args, String cacheName, DefaultCacheManager cacheMgr) {
        Configuration cfg = cacheMgr.getCacheConfiguration(cacheName);

        ConfigurationBuilder cfgBuilder = new ConfigurationBuilder().read(cfg);

        cfgBuilder.clustering().cacheMode(args.async() ? CacheMode.DIST_ASYNC : CacheMode.DIST_SYNC);

        cfgBuilder.clustering().hash().numOwners(args.backups() + 1);

        // READ_COMMITTED isolation level is used by default.
/*
        cfgBuilder.locking().isolationLevel(IsolationLevel.REPEATABLE_READ);
*/
        // By default, transactional cache is optimistic.
        cfg.transaction().lockingMode(args.txPessimistic() ? LockingMode.PESSIMISTIC : LockingMode.OPTIMISTIC);

        cacheMgr.defineConfiguration(cacheName, cfgBuilder.build());

        return cacheMgr.getCache(cacheName);
    }

    /**
     * @param cfg Config.
     * @param args Arguments.
     * @param cacheMgr Cache manager.
     */
    private void startHotRodServer(BenchmarkConfiguration cfg, InfinispanBenchmarkArguments args, DefaultCacheManager cacheMgr) {
        String host = cacheMgr.getTransport().getPhysicalAddresses().get(0).toString().split(":")[0];

        // Try to start server beginning with default port, it is needed when several nodes are run on one machine.
        for (int i = 0, n = Math.max(args.nodes(), 10); i < n; i++) {
            int port = 11222 + i;

            println(cfg, "Trying to start HotRodServer on host " + host + ", port " + port + "...");

            try {
                HotRodServerConfigurationBuilder builder = new HotRodServerConfigurationBuilder().port(port).host(host);

                hotRodServer = new HotRodServer();

                hotRodServer.start(builder.build(), cacheMgr);

                println(cfg, "HotRodServer is started on host " + host + ", port " + port + ".");

                break;
            }
            catch (ChannelException e) {
                if (!(e.getCause() instanceof BindException))
                    throw e;
                else
                    println(cfg, "Port is already in use " + port + ", let's try next port.");
            }
        }
    }

    /** */
    private static void configureLogging() {
        Logger rootLog = Logger.getRootLogger();

        PatternLayout layout = new PatternLayout("[%d{dd-MM-yyyy HH:mm:ss}][%-5p][%t][%c{1}] %m%n");

        rootLog.addAppender(new ConsoleAppender(layout));
        rootLog.setLevel(Level.INFO);
    }

    /** {@inheritDoc} */
    @Override public void stop() throws Exception {
        if (cacheMgr != null)
            cacheMgr.stop();

        if (hotRodServer != null)
            hotRodServer.stop();
    }

    /** {@inheritDoc} */
    @Override public String usage() {
        return BenchmarkUtils.usage(new InfinispanBenchmarkArguments());
    }

    /**
     * @return BasicCacheContainer.
     */
    public BasicCacheContainer cacheContainer() {
        return cacheMgr;
    }
}
