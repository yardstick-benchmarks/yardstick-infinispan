# Yardstick Infinispan Benchmarks
Yardstick Infinispan is a set of <a href="http://infinispan.org" target="_blank">Infinispan Data Grid</a> benchmarks written on top of Yardstick framework.

## Yardstick Framework
Visit <a href="https://github.com/gridgain/yardstick" target="_blank">Yardstick Repository</a> for detailed information on how to run Yardstick benchmarks and how to generate graphs.

The documentation below describes configuration parameters in addition to standard Yardstick parameters.

## Installation
1. Create a local clone of Yardstick Infinispan repository
2. Import Yardstick Infinispan POM file into your project
3. Run `mvn package` command

## Provided Benchmarks
The following benchmarks are provided:

1. `InfinispanPutBenchmark` - benchmarks atomic distributed cache put operation
2. `InfinispanPutGetBenchmark` - benchmarks atomic distributed cache put and get operations together
3. `InfinispanPutTxBenchmark` - benchmarks transactional distributed cache put operation
4. `InfinispanPutGetTxBenchmark` - benchmarks transactional distributed cache put and get operations together
5. `InfinispanSqlQueryBenchmark` - benchmarks distributed SQL query over cached data
6. `InfinispanSqlQueryPutBenchmark` - benchmarks distributed SQL query with simultaneous cache updates

## Writing Infinispan Benchmarks
All benchmarks extend `InfinispanAbstractBenchmark` class. A new benchmark should also extend this abstract class and implement `test` method. This is the method that is actually benchmarked.

## Running Infinispan Benchmarks
Before running Infinispan benchmarks, run `mvn package` command. This command will compile the project and also will unpack scripts from `yardstick-resources.zip` file to `bin` directory.

### Properties And Command Line Arguments
> Note that this section only describes configuration parameters specific to Infinispan benchmarks, and not for Yardstick framework. To run Infinispan benchmarks and generate graphs, you will need to run them using Yardstick framework scripts in `bin` folder.

> Refer to [Yardstick Documentation](https://github.com/gridgain/yardstick) for common Yardstick properties and command line arguments for running Yardstick scripts.

The following Infinispan benchmark properties can be defined in the benchmark configuration:

* `-nn <num>` or `--nodeNumber <num>` - Number of nodes (automatically set in `benchmark.properties`), used to wait for the specified number of nodes to start
* `-b <num>` or `--backups <num>` - Number of backups for every key
* `-iscfg <path>` or `--isConfig <path>` - Path to Infinispan configuration file
* `-as` or `--async` - Flag indicating whether asynchronous communication is used, synchronous is a default
* `-cm` or `--clientMode` - Flag indicating whether Infinispan client is used
* `-r <num>` or `--range <num>` - Range of keys that are randomly generated for cache operations
* `-txp` or `--txPessimistic` - Flag indicating whether pessimistic transaction concurrency is used, optimistic is a default

For example if we need to run 2 `InfinispanNode` servers on localhost with `InfinispanPutBenchmark` benchmark on localhost, with number of backups set to 1, then the following configuration should be specified in `benchmark.properties` file:

```
HOSTS=localhost,localhost
    
# Note that -dn and -sn, which stand for data node and server node, are 
# native Yardstick parameters and are documented in Yardstick framework.
CONFIGS="-b 1 -dn InfinispanPutBenchmark -sn InfinispanNode"
```

## Issues
Use GitHub [issues](https://github.com/gridgain/yardstick-infinispan/issues) to file bugs.

## License
Yardstick Infinispan is available under [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) Open Source license.
