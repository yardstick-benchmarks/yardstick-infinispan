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

import io.netty.channel.ChannelException;
import java.io.InputStream;
import java.net.BindException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.infinispan.util.concurrent.IsolationLevel;
import org.yardstickframework.BenchmarkConfiguration;
import org.yardstickframework.BenchmarkServer;
import org.yardstickframework.BenchmarkUtils;
import org.yardstickframework.infinispan.protobuf.PersonMarshaller;
import org.yardstickframework.infinispan.protobuf.PersonProtobuf;

import static org.yardstickframework.BenchmarkUtils.jcommander;
import static org.yardstickframework.BenchmarkUtils.println;

/**
 * Standalone Infinispan node.
 */
public class InfinispanNode implements BenchmarkServer {
    /** */
    private BasicCacheContainer cacheMgr;

    /** */
    private HotRodServer hotRodSrv;

    /** */
    private boolean clientMode;

    /** */
    private boolean qryEnabled;

    /** */
    public InfinispanNode() {
        // No-op.
    }

    /**
     * @param clientMode Client mode.
     * @param qryEnabled Query enabled.
     */
    public InfinispanNode(boolean clientMode, boolean qryEnabled) {
        this.clientMode = clientMode;
        this.qryEnabled = qryEnabled;
    }

    /** {@inheritDoc} */
    @Override public void start(BenchmarkConfiguration cfg) throws Exception {
        configureLogging();

        InfinispanBenchmarkArguments args = new InfinispanBenchmarkArguments();

        jcommander(cfg.commandLineArguments(), args, "<infinispan-node>");

        String nodesAddrs = cfg.customProperties().get("DRIVER_HOSTS") + ','
            + cfg.customProperties().get("SERVER_HOSTS");

        initEc2Variables();

        if (clientMode) {
            org.infinispan.client.hotrod.configuration.ConfigurationBuilder builder =
                new org.infinispan.client.hotrod.configuration.ConfigurationBuilder().
                    addServers(nodesAddrs.replace(",", ";"));

            RemoteCacheManager rmtCacheMgr;

            if (qryEnabled) {
                builder.marshaller(new ProtoStreamMarshaller());

                rmtCacheMgr = new RemoteCacheManager(builder.build());

                SerializationContext serCtx = ProtoStreamMarshaller.getSerializationContext(rmtCacheMgr);

                FileDescriptorSource fileDescSrc = new FileDescriptorSource();

                try (InputStream is = PersonProtobuf.class.getResourceAsStream("person.protobin")) {
                    fileDescSrc.addProtoFile("person", is);
                }

                serCtx.registerProtoFiles(fileDescSrc);

                serCtx.registerMarshaller(new PersonMarshaller());
            }
            else
                rmtCacheMgr = new RemoteCacheManager(builder.build());

            cacheMgr = rmtCacheMgr;
        }
        else {
            System.setProperty("jgroups.tcpping.initial_hosts", addressesWithPorts(nodesAddrs));

            if (nodesAddrs.contains("localhost") || nodesAddrs.contains("127.0.0.1"))
                System.setProperty("jgroups.bind_addr", "localhost");

            DefaultCacheManager cacheMgr = new DefaultCacheManager(args.configuration());

            cache(args, "cache", cacheMgr, cfg);

            cache(args, "transactional", cacheMgr, cfg);

            cache(args, "queryCache", cacheMgr, cfg);

            this.cacheMgr = cacheMgr;

            if (args.clientMode())
                startHotRodServer(cfg, args, cacheMgr);

            println(cfg, "Infinispan node started.");
        }
    }

    /**
     * Init ec2 properties.
     */
    private void initEc2Variables() {
        System.setProperty("java.net.preferIPv4Stack" , "true");

        String ipAddr = System.getenv("LOCAL_IP");

        if (ipAddr != null)
            System.setProperty("jgroups.tcp.address", ipAddr);

        String awsAccessKey = System.getenv("AWS_ACCESS_KEY");

        if (awsAccessKey != null)
            System.setProperty("jgroups.s3.access_key", awsAccessKey);

        String awsSecretKey = System.getenv("AWS_SECRET_KEY");

        if (awsSecretKey != null)
            System.setProperty("jgroups.s3.secret_access_key", awsSecretKey);

        String awsBucketName = System.getenv("AWS_BUCKET_NAME");

        if (awsBucketName != null)
            System.setProperty("jgroups.s3.bucket", awsBucketName);
    }

    /**
     * @param args Arguments.
     * @param cacheName Cache name.
     * @param cacheMgr Default cache manager.
     * @param bcfg Benchmark configuration.
     * @return Cache.
     */
    private Cache<Object, Object> cache(InfinispanBenchmarkArguments args, String cacheName,
        DefaultCacheManager cacheMgr, BenchmarkConfiguration bcfg) {
        Configuration cfg = cacheMgr.getCacheConfiguration(cacheName);

        ConfigurationBuilder cfgBuilder = new ConfigurationBuilder().read(cfg);

        cfgBuilder.clustering().cacheMode(args.async() ? CacheMode.DIST_ASYNC : CacheMode.DIST_SYNC);

        cfgBuilder.clustering().hash().numOwners(args.backups() + 1);

        // HotRodServer can not start if REPEATABLE_READ is set.
        if (!args.clientMode()) {
            if (args.txIsolation() == IsolationLevel.SERIALIZABLE)
                println(bcfg, "[WARNING] Infinispan doesn't actually support the SERIALIZABLE isolation level, " +
                    "instead automatically it downgrades to REPEATABLE_READ.");

            cfgBuilder.locking().isolationLevel(args.txIsolation());
        }

        cfgBuilder.transaction().lockingMode(args.txConcurrency());

        cacheMgr.defineConfiguration(cacheName, cfgBuilder.build());

        Cache cache = cacheMgr.getCache(cacheName);

        Configuration ccfg = cache.getCacheConfiguration();

        println(bcfg, "Started cache [name=" + cacheName + ", txMode=" + ccfg.transaction().transactionMode()
            + ", lockingMode=" + ccfg.transaction().lockingMode() + ", isolationMode="
            + ccfg.locking().isolationLevel() + ", indexing=" + ccfg.indexing().index() + ", fullCacheCfg=" + ccfg);

        return cache;
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

                hotRodSrv = new HotRodServer();

                hotRodSrv.start(builder.build(), cacheMgr);

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

        if (hotRodSrv != null)
            hotRodSrv.stop();
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
