/**
 * Copyright 2018 Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package io.confluent.ksql.serde.tls;

import io.confluent.ksql.GenericRow;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.kafka.common.serialization.Serializer;

public class ThreadLocalSerializer implements Serializer<GenericRow> {
  private final List<Serializer<GenericRow>> createdSerializers;
  private final ThreadLocal<Serializer<GenericRow>> serializer;

  public ThreadLocalSerializer(final Supplier<Serializer<GenericRow>> initialValueSupplier) {
    this.createdSerializers = new LinkedList<>();
    this.serializer = new ThreadLocal<Serializer<GenericRow>>() {
      @Override
      protected synchronized Serializer<GenericRow> initialValue() {
        createdSerializers.add(initialValueSupplier.get());
        return createdSerializers.get(createdSerializers.size() - 1);
      }
    };
  }

  @Override
  public void configure(Map<String, ?> properties, boolean isKey) {
    serializer.get().configure(properties, isKey);
  }

  @Override
  public byte[] serialize(String topicName, GenericRow record) {
    return serializer.get().serialize(topicName, record);
  }

  @Override
  public void close() {
    createdSerializers.forEach(Serializer::close);
  }
}
