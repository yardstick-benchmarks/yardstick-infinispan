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
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Infinispan benchmark that performs put operations.
 */
public class InfinispanPutAllBenchmark extends InfinispanAbstractBenchmark {
    /** {@inheritDoc} */
    @Override public boolean test(Map<Object, Object> ctx) throws Exception {
        SortedMap<Integer, Integer> vals = new TreeMap<>();

        for (int i = 0; vals.size() < args.batch(); i++) {
            int key = nextRandom(args.range());

            vals.put(key, key);
        }

        cache.putAll(vals);

        return true;
    }

    /** {@inheritDoc} */
    @Override protected String cacheName() {
        return "cache";
    }
}
