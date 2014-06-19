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

import org.infinispan.*;
import org.infinispan.query.*;
import org.infinispan.query.dsl.*;
import org.yardstickframework.infinispan.querymodel.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Infinispan benchmark that performs put and query operations.
 */
public class InfinispanSqlQueryPutBenchmark extends InfinispanAbstractBenchmark {
    /** */
    public InfinispanSqlQueryPutBenchmark() {
        super("queryCache");
    }

    /** {@inheritDoc} */
    @Override public void test() throws Exception {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        if (rnd.nextBoolean()) {
            double salary = rnd.nextDouble() * args.range() * 1000;

            double maxSalary = salary + 1000;

            Collection<Person> persons = executeQuery(salary, maxSalary);

            for (Person p : persons)
                if (p.getSalary() < salary || p.getSalary() > maxSalary)
                    throw new Exception("Invalid person retrieved [min=" + salary + ", max=" + maxSalary +
                        ", person=" + p + ']');
        }
        else {
            int i = rnd.nextInt(args.range());

            cache.put(i, new Person(i, "firstName" + i, "lastName" + i, i * 1000));
        }
    }

    /**
     * @param minSalary Min salary.
     * @param maxSalary Max salary.
     * @return Query results.
     * @throws Exception If failed.
     */
    private Collection<Person> executeQuery(double minSalary, double maxSalary) throws Exception {
        SearchManager searchMgr = Search.getSearchManager((Cache<Object, Object>)cache);

        QueryFactory qf = searchMgr.getQueryFactory();

        Query qry = qf.from(Person.class).having("salary").between(minSalary, maxSalary).toBuilder().build();

        return qry.list();
    }
}
