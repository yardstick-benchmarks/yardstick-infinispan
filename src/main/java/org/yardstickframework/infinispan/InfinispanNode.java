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
import org.infinispan.client.hotrod.marshall.*;
import org.infinispan.commons.api.*;
import org.infinispan.configuration.cache.*;
import org.infinispan.manager.*;
import org.infinispan.protostream.*;
import org.infinispan.query.remote.*;
import org.infinispan.server.hotrod.*;
import org.infinispan.server.hotrod.configuration.*;
import org.infinispan.transaction.*;
import org.infinispan.util.concurrent.*;
import org.jboss.netty.channel.*;
import org.yardstickframework.*;
import org.yardstickframework.infinispan.protobuf.*;

import java.io.*;
import java.net.*;

import static org.yardstickframework.BenchmarkUtils.*;

/**
 * Standalone Infinispan node.
 */
public class InfinispanNode implements BenchmarkServer {
    /** */
    private static final String NODES_ADDRESSES = "INFINISPAN_NODES_ADDRESSES";

    /** */
    private BasicCacheContainer cacheMgr;

    /** */
    private HotRodServer hotRodServer;

    /** */
    private boolean clientMode;

    /** */
    private boolean queryEnabled;

    /** */
    public InfinispanNode() {
        // No-op.
    }

    /**
     * @param clientMode Client mode.
     * @param queryEnabled Query enabled.
     */
    public InfinispanNode(boolean clientMode, boolean queryEnabled) {
        this.clientMode = clientMode;
        this.queryEnabled = queryEnabled;
    }

    /** {@inheritDoc} */
    @Override public void start(BenchmarkConfiguration cfg) throws Exception {
        configureLogging();

        InfinispanBenchmarkArguments args = new InfinispanBenchmarkArguments();

        jcommander(cfg.commandLineArguments(), args, "<infinispan-node>");

        String nodesAddresses = cfg.customProperties().get(NODES_ADDRESSES);

        if (nodesAddresses == null || nodesAddresses.isEmpty())
            throw new Exception("Property '" + NODES_ADDRESSES + "' is not defined.");

        if (clientMode) {
            org.infinispan.client.hotrod.configuration.ConfigurationBuilder builder =
                new org.infinispan.client.hotrod.configuration.ConfigurationBuilder().
                    addServers(nodesAddresses.replace(",", ";"));

            RemoteCacheManager rmtCacheManager;

            if (queryEnabled) {
                builder.marshaller(new ProtoStreamMarshaller());

                rmtCacheManager = new RemoteCacheManager(builder.build());

                SerializationContext serCtx = ProtoStreamMarshaller.getSerializationContext(rmtCacheManager);

                try (InputStream is = PersonProtobuf.class.getResourceAsStream("person.protobin")) {
                    serCtx.registerProtofile(is);
                }

                serCtx.registerMarshaller(PersonProtobuf.Person.class, new PersonMarshaller());
            }
            else
                rmtCacheManager = new RemoteCacheManager(builder.build());

            cacheMgr = rmtCacheManager;
        }
        else {
            System.setProperty("jgroups.tcpping.initial_hosts", addressesWithPorts(nodesAddresses));

            if (nodesAddresses.contains("localhost") || nodesAddresses.contains("127.0.0.1"))
                System.setProperty("jgroups.bind_addr", "localhost");

            DefaultCacheManager cacheMgr = new DefaultCacheManager(args.configuration());

            cache(args, "cache", cacheMgr);

            cache(args, "transactional", cacheMgr);

            cache(args, "queryCache", cacheMgr);

            this.cacheMgr = cacheMgr;

            if (args.clientMode())
                startHotRodServer(cfg, args, cacheMgr);

            try (InputStream is = PersonProtobuf.class.getResourceAsStream("person.protobin")) {
                cacheMgr.getGlobalComponentRegistry().getComponent(ProtobufMetadataManager.class).registerProtofile(is);
            }

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
        // HotRodServer can not start if REPEATABLE_READ is set.
        if (!args.clientMode())
            cfgBuilder.locking().isolationLevel(IsolationLevel.REPEATABLE_READ);

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

    /**
     * @param nodesAddresses Addresses.
     * @return Nodes addresses.
     */
    private static String addressesWithPorts(String nodesAddresses) {
        StringBuilder sb = new StringBuilder();

        for (String s : nodesAddresses.split(","))
            sb.append(s).append("[7800]").append(",");

        if (sb.length() > 0)
            sb.delete(sb.length() - 1, sb.length());

        return sb.toString();
    }
}
