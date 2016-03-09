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
import java.util.concurrent.ThreadLocalRandom;
import org.infinispan.Cache;
import org.infinispan.query.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.yardstickframework.infinispan.protobuf.PersonProtobuf;
import org.yardstickframework.infinispan.querymodel.Person;

/**
 * Infinispan benchmark that performs put and query operations.
 */
public class InfinispanSqlQueryPutBenchmark extends InfinispanAbstractBenchmark {
    /** {@inheritDoc} */
    @Override public boolean test(Map<Object, Object> test) throws Exception {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        if (rnd.nextBoolean()) {
            double salary = rnd.nextDouble() * args.range() * 1000;

            double maxSalary = salary + 1000;

            QueryFactory qf = Search.getQueryFactory((Cache<Object, Object>) cache);

            Query qry = qf.from(Person.class).having("salary").between(salary, maxSalary).toBuilder().build();

            for (Person p : qry.<Person>list()) {
                if (p.getSalary() < salary || p.getSalary() > maxSalary)
                    throw new Exception("Invalid person retrieved [min=" + salary + ", max=" + maxSalary +
                            ", person=" + p + ']');
            }
        }
        else {
            int i = rnd.nextInt(args.range());

            cache.put(i, createPerson(i, "firstName" + i, "lastName" + i, i * 1000));
        }

        return true;
    }

    /**
     * @param id Id.
     * @param firstName First name.
     * @param lastName Last name.
     * @param salary Salary.
     * @return Person.
     */
    private Object createPerson(int id, String firstName, String lastName, double salary) {
        if (args.clientMode())
            return PersonProtobuf.Person.newBuilder().
                setId(id).
                setFirstName(firstName).
                setLastName(lastName).
                setSalary(salary).
                build();
        else
            return new Person(id, firstName, lastName, salary);
    }

    /** {@inheritDoc} */
    @Override protected String cacheName() {
        return "queryCache";
    }
}
