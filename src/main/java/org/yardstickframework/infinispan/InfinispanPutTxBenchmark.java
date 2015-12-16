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

import java.util.Map;
import javax.transaction.TransactionManager;
import org.infinispan.Cache;
import org.yardstickframework.BenchmarkConfiguration;

/**
 * Infinispan benchmark that performs transactional put operations.
 */
public class InfinispanPutTxBenchmark extends InfinispanAbstractBenchmark {
    /** {@inheritDoc} */
    @Override public void setUp(BenchmarkConfiguration cfg) throws Exception {
        super.setUp(cfg);

        if (args.clientMode())
            throw new IllegalStateException("HotRod Client does not support transactions.");
    }

    /** {@inheritDoc} */
    @Override protected String cacheName() {
        return "transactional";
    }

    /** {@inheritDoc} */
    @Override public boolean test(Map<Object, Object> ctx) throws Exception {
        int key = nextRandom(args.range());

        TransactionManager tm = ((Cache)cache).getAdvancedCache().getTransactionManager();

        tm.begin();

        try {
            cache.put(key, new SampleValue(key));

            tm.commit();
        }
        catch (Exception e) {
            e.printStackTrace(cfg.error());

            tm.rollback();
        }

        return true;
    }
}
