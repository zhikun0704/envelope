/**
 * Licensed to Cloudera, Inc. under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Cloudera, Inc. licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.labs.envelope.derive.dq;

import com.cloudera.labs.envelope.spark.RowWithSchema;
import com.cloudera.labs.envelope.utils.RowUtils;
import com.typesafe.config.Config;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.expressions.Aggregator;

import java.util.Map;

public class DatasetRowRuleWrapper implements DatasetRule {

  private String name;
  private RowRule rowRule;

  @Override
  public void configure(String name, Config config) {
    this.name = name;
    this.rowRule = RowRuleFactory.create(name, config);
  }

  @Override
  public Dataset<Row> check(Dataset<Row> dataset, Map<String, Dataset<Row>> stepDependencies) {
    return dataset.map(new CheckRule(rowRule, name), RowEncoder.apply(SCHEMA)).select(new BooleanAggregator(name).toColumn());
  }

  private class CheckRule implements MapFunction<Row, Row> {

    private RowRule theRule;
    private String name;

    CheckRule(RowRule theRule, String name) {
      this.theRule = theRule;
      this.name = name;
    }

    @Override
    public Row call(Row row) throws Exception {
      return new RowWithSchema(SCHEMA, name, theRule.check(row));
    }

  }

  private class BooleanAggregator extends Aggregator<Row,Row,Row> {

    private String name;

    BooleanAggregator(String name) {
      this.name = name;
    }

    @Override
    public Row zero() {
      return new RowWithSchema(SCHEMA, name, true);
    }

    @Override
    public Row reduce(Row a, Row b) {
      return new RowWithSchema(SCHEMA, name,
          RowUtils.<Boolean>getAs(a, "result") && RowUtils.<Boolean>getAs(b, "result"));
    }

    @Override
    public Row merge(Row a, Row b) {
      return new RowWithSchema(SCHEMA, name,
          RowUtils.<Boolean>getAs(a, "result") && RowUtils.<Boolean>getAs(b, "result"));
    }

    @Override
    public Row finish(Row reduction) {
      return reduction;
    }

    @Override
    public Encoder<Row> bufferEncoder() {
      return RowEncoder.apply(SCHEMA);
    }

    @Override
    public Encoder<Row> outputEncoder() {
      return RowEncoder.apply(SCHEMA);
    }

  }

}
