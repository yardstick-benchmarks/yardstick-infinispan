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

package org.yardstickframework.infinispan.protobuf;

import org.infinispan.protostream.*;

import java.io.*;

import static org.yardstickframework.infinispan.protobuf.PersonProtobuf.*;

/**
 * Infinispan Person marshaller.
 */
public class PersonMarshaller implements MessageMarshaller<Person> {
    /** {@inheritDoc} */
    @Override public String getTypeName() {
        return "org.yardstickframework.infinispan.Person";
    }

    /** {@inheritDoc} */
    @Override public Class<? extends Person> getJavaClass() {
        return Person.class;
    }

    /** {@inheritDoc} */
    @Override public void writeTo(ProtoStreamWriter writer, Person person) throws IOException {
        writer.writeInt("id", person.getId());
        writer.writeInt("orgId", person.getOrdId());
        writer.writeString("firstName", person.getFirstName());
        writer.writeString("lastName", person.getLastName());
        writer.writeDouble("salary", person.getSalary());
    }

    /** {@inheritDoc} */
    @Override public Person readFrom(ProtoStreamReader reader) throws IOException {
        int id = reader.readInt("id");
        int orgId = reader.readInt("orgId");
        String firstName = reader.readString("firstName");
        String lastName = reader.readString("lastName");
        double salary = reader.readDouble("salary");

        return Person.newBuilder().setId(id).setOrdId(orgId).setFirstName(firstName).
            setLastName(lastName).setSalary(salary).build();
    }
}
