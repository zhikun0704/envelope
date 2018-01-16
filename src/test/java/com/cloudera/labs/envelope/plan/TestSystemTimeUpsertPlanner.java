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
package com.cloudera.labs.envelope.plan;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cloudera.labs.envelope.spark.Contexts;
import com.cloudera.labs.envelope.spark.RowWithSchema;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import scala.Tuple2;

public class TestSystemTimeUpsertPlanner {

  private static Dataset<Row> dataFrame;

  @BeforeClass
  public static void beforeClass() {
    StructType schema = DataTypes.createStructType(Lists.newArrayList(
        DataTypes.createStructField("key", DataTypes.StringType, false)));
    Row row = new RowWithSchema(schema, "a");
    dataFrame = Contexts.getSparkSession().createDataFrame(Lists.newArrayList(row), schema);
  }

  @Test
  public void testPlansUpserts() {
    Config config = ConfigFactory.empty();
    BulkPlanner planner = new SystemTimeUpsertPlanner();
    planner.configure(config);

    List<Tuple2<MutationType, Dataset<Row>>> planned = planner.planMutationsForSet(dataFrame);

    assertEquals(planned.size(), 1);
    assertEquals(planned.get(0)._1(), MutationType.UPSERT);
    assertEquals(planned.get(0)._2().count(), 1);
  }

  @Test
  public void testNoLastUpdated() {
    Config config = ConfigFactory.empty();
    BulkPlanner planner = new SystemTimeUpsertPlanner();
    planner.configure(config);

    List<Tuple2<MutationType, Dataset<Row>>> planned = planner.planMutationsForSet(dataFrame);

    assertEquals(planned.size(), 1);

    Dataset<Row> plannedDF = planned.get(0)._2();

    assertEquals(plannedDF.count(), 1);

    Row plannedRow = plannedDF.collectAsList().get(0);
    assertEquals(plannedRow.length(), 1);
  }

  @Test
  public void testLastUpdated() {
    Map<String, Object> configMap = Maps.newHashMap();
    configMap.put(SystemTimeUpsertPlanner.LAST_UPDATED_FIELD_NAME_CONFIG_NAME, "lastupdated");
    Config config = ConfigFactory.parseMap(configMap);

    BulkPlanner planner = new SystemTimeUpsertPlanner();
    planner.configure(config);

    List<Tuple2<MutationType, Dataset<Row>>> planned = planner.planMutationsForSet(dataFrame);

    assertEquals(planned.size(), 1);

    Dataset<Row> plannedDF = planned.get(0)._2();

    assertEquals(plannedDF.count(), 1);

    Row plannedRow = plannedDF.collectAsList().get(0);
    assertEquals(plannedRow.length(), 2);
  }

}
