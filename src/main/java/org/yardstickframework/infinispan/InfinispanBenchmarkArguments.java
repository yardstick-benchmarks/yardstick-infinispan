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

import com.beust.jcommander.*;

/**
 * Input arguments for Infinispan benchmarks.
 */
@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
public class InfinispanBenchmarkArguments {
    /** */
    @Parameter(names = {"-nn", "--nodeNumber"}, description = "Node number")
    private int nodes = 1;

    /** */
    @Parameter(names = {"-b", "--backups"}, description = "Backups")
    private int backups;

    /** */
    @Parameter(names = {"-iscfg", "--isConfig"}, description = "Configuration file")
    private String isCfg = "config/infinispan-config.xml";

    /** */
    @Parameter(names = {"-as", "--async"}, description = "Asynchronous communication")
    private boolean async;

    /** */
    @Parameter(names = {"-cm", "--clientMode"}, description = "Client mode")
    private boolean clientMode;

    /** */
    @Parameter(names = {"-r", "--range"}, description = "Key range")
    private int range = 1_000_000;

    /** */
    @Parameter(names = {"-txp", "--txPessimistic"}, description = "Pessimistic transaction concurrency")
    private boolean txPessimistic;

    /**
     * @return {@code True} whether communication is asynchronous or not.
     */
    public boolean async() {
        return async;
    }

    /**
     * @return Client mode.
     */
    public boolean clientMode() {
        return clientMode;
    }

    /**
     * @return Backups.
     */
    public int backups() {
        return backups;
    }

    /**
     * @return Nodes.
     */
    public int nodes() {
        return nodes;
    }

    /**
     * @return Key range, from {@code 0} to this number.
     */
    public int range() {
        return range;
    }

    /**
     * @return Configuration file.
     */
    public String configuration() {
        return isCfg;
    }

    /**
     * @return {@code True} whether transaction concurrency is pessimistic or not.
     */
    public boolean txPessimistic() {
        return txPessimistic;
    }

    /**
     * @return Description.
     */
    public String description() {
        return "-nn=" + nodes + "-b=" + backups + "-as=" + async + "-cm=" + clientMode + "-txp=" + txPessimistic;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return getClass().getSimpleName() + " [" +
            "nodes=" + nodes +
            ", backups=" + backups +
            ", isConfig='" + isCfg + '\'' +
            ", async=" + async +
            ", clientMode=" + clientMode +
            ", txPessimistic=" + txPessimistic +
            ", range=" + range +
            ']';
    }
}
