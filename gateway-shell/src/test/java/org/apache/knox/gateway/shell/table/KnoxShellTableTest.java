/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.knox.gateway.shell.table;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Collections;

import javax.swing.SortOrder;

import org.apache.commons.io.FileUtils;
import org.easymock.IAnswer;
import org.junit.Test;

public class KnoxShellTableTest {
  @Test
  public void testSimpleTableRendering() {
    String expectedResult = "+------------+------------+------------+\n"
        + "|  Column A  |  Column B  |  Column C  |\n" + "+------------+------------+------------+\n"
        + "|    123     |    456     |     3      |\n" + "+------------+------------+------------+\n";

    KnoxShellTable table = new KnoxShellTable();

    table.header("Column A").header("Column B").header("Column C");

    table.row().value("123").value("456").value("3");

    assertEquals(expectedResult, table.toString());
  }

  @Test
  public void testValueLongerThanHeader() {
    String expectedResult = "+------------+------------+--------------+\n"
        + "|  Column A  |  Column B  |   Column C   |\n" + "+------------+------------+--------------+\n"
        + "|    123     |    456     |  344444444   |\n" + "+------------+------------+--------------+\n";

    KnoxShellTable table = new KnoxShellTable();

    table.header("Column A").header("Column B").header("Column C");

    table.row().value("123").value("456").value("344444444");

    assertEquals(expectedResult, table.toString());
  }

  @Test
  public void testMultipleRowsTableRendering() {
    String expectedResult = "+------------+------------+--------------+\n"
        + "|  Column A  |  Column B  |   Column C   |\n" + "+------------+------------+--------------+\n"
        + "|    123     |    456     |  344444444   |\n"
        + "|    789     |    012     |  844444444   |\n" + "+------------+------------+--------------+\n";

    KnoxShellTable table = new KnoxShellTable();

    table.header("Column A").header("Column B").header("Column C");

    table.row().value("123").value("456").value("344444444");
    table.row().value("789").value("012").value("844444444");

    assertEquals(expectedResult, table.toString());
  }

  @Test
  public void testTableSelect() {
    String expectedResult = "+------------+------------+--------------+\n"
        + "|  Column A  |  Column B  |   Column C   |\n" + "+------------+------------+--------------+\n"
        + "|    123     |    456     |  344444444   |\n"
        + "|    789     |    012     |  844444444   |\n" + "+------------+------------+--------------+\n";

    String expectedResult2 = "+------------+--------------+\n"
        + "|  Column A  |   Column C   |\n" + "+------------+--------------+\n"
        + "|    123     |  344444444   |\n"
        + "|    789     |  844444444   |\n" + "+------------+--------------+\n";

    KnoxShellTable table = new KnoxShellTable();

    table.header("Column A").header("Column B").header("Column C");

    table.row().value("123").value("456").value("344444444");
    table.row().value("789").value("012").value("844444444");

    assertEquals(expectedResult, table.toString());

    table = table.select("Column A,Column C");
    assertEquals(expectedResult2, table.toString());
  }

  @Test
  public void testMultipleRowsTableNoHeadersRendering() {
    String expectedResult = "+--------+--------+--------------+\n"
        + "|  123   |  456   |  344444444   |\n"
        + "|  789   |  012   |  844444444   |\n" + "+--------+--------+--------------+\n";

    KnoxShellTable table = new KnoxShellTable();

    table.row().value("123").value("456").value("344444444");
    table.row().value("789").value("012").value("844444444");

    assertEquals(expectedResult, table.toString());

    assertEquals(expectedResult, table.toString());
  }

  @Test(expected = IllegalStateException.class)
  public void testMultipleRowsTableMismatchedColAndHeadersCountError() {
    KnoxShellTable table = new KnoxShellTable();

    table.header("Column A").header("Column B");

    table.row().value("123").value("456").value("344444444");
    table.row().value("789").value("012").value("844444444");

    assertNull(table.toString());
    table.toString();
  }

  @Test
  public void testLoadCSVToAndFromURL() {
    KnoxShellTable table = new KnoxShellTable();
    table.title("From URL");

    table.header("Column A").header("Column B").header("Column C");

    table.row().value("123").value("456").value("344444444");
    table.row().value("789").value("012").value("844444444");

    String csv = table.toCSV();
    try {
      // write file to /tmp to read back in
      FileUtils.writeStringToFile(new File("/tmp/testtable.csv"), csv, StandardCharsets.UTF_8);

      KnoxShellTable urlTable = KnoxShellTable.builder().csv()
          .withHeaders()
          .url("file:///tmp/testtable.csv");
          //.url("http://samplecsvs.s3.amazonaws.com/Sacramentorealestatetransactions.csv");
      urlTable.title("From URL");
      assertEquals(urlTable.toString(), table.toString());

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testToAndFromJSON() throws IOException {
    KnoxShellTable table = new KnoxShellTable();

    table.header("Column A").header("Column B").header("Column C");

    table.row().value("123").value("456").value("344444444");
    table.row().value("789").value("012").value("844444444");

    String json = table.toJSON();

    KnoxShellTable table2 = KnoxShellTable.builder().json().fromJson(json);
    assertEquals(table.toString(), table2.toString());
  }

  @Test
  public void testFromJSONUsingCallHistory() throws IOException {
    final String jsonPath = KnoxShellTableCallHistoryTest.class.getClassLoader().getResource("knoxShellTableCallHistoryWithFiltering.json").getPath();
    final String csvPath = "file://" + KnoxShellTableCallHistoryTest.class.getClassLoader().getResource("knoxShellTableLocationsWithZipLessThan14.csv").getPath();
    String json = (FileUtils.readFileToString(new File(jsonPath), StandardCharsets.UTF_8));
    json = json.replaceAll("CSV_FILE_PATH_PLACEHOLDER", csvPath);
    // filtered table where ZIPs are greater than "5" (in CSV everything is String)
    final KnoxShellTable table = KnoxShellTable.builder().json().fromJson(json);
    assertNotNull(table);
    assertEquals(4, table.rows.size()); // only 4 rows with ZIP of 6, 7, 8 and 9
  }

  @Test
  public void testSort() throws IOException {
    KnoxShellTable table = new KnoxShellTable();

    table.header("Column A").header("Column B").header("Column C");

    table.row().value("789").value("012").value("844444444");
    table.row().value("123").value("456").value("344444444");

    KnoxShellTable table2 = table.sort("Column A");
    assertEquals(table2.getRows().get(0).get(0), "123");
    assertEquals(table2.getRows().get(1).get(0), "789");

    table2 = table.sort("Column A", SortOrder.DESCENDING);
    assertEquals(table2.getRows().get(0).get(0), "789");
    assertEquals(table2.getRows().get(1).get(0), "123");

    table2 = table.sort("Column B", SortOrder.DESCENDING);
    assertEquals(table2.getRows().get(0).get(1), "456");
    assertEquals(table2.getRows().get(1).get(1), "012");
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testCells() throws IOException {
    KnoxShellTable table = new KnoxShellTable();

    table.header("Column A").header("Column B").header("Column C");

    table.row().value("123").value("456").value("344444444");
    table.row().value("789").value("012").value("844444444");

    KnoxShellTableCell cell = table.cell(1, 1);
    assertEquals(cell.header, "Column B");
    assertEquals(cell.value, "012");
    cell.header("Column Beeee");
    cell.value("234");
    table.apply(cell);
    assertEquals(table.cell(1, 1).value, "234");
    assertEquals(table.cell(1, 1).header, "Column Beeee");
  }

  @Test
  public void testFilterTable() throws KnoxShellTableFilterException {
    KnoxShellTable table = new KnoxShellTable();

    table.header("Column A").header("Column B").header("Column C");

    table.row().value("123").value("456").value("344444444");
    table.row().value("789").value("012").value("844444444");

    KnoxShellTable filtered = table.filter().name("Column A").regex("123");

    assertEquals(filtered.getRows().size(), 1);
  }

  @Test
  public void testJoinTables() throws IOException {
    KnoxShellTable table = new KnoxShellTable();

    table.title("Left Table").header("Column A").header("Column B").header("Column C").header("Column D");

    table.row().value("123").value("456").value("344444444").value("2");
    table.row().value("789").value("012").value("844444444").value("2");
    table.row().value("980").value("2").value("844444444").value("2");

    KnoxShellTable table2 = new KnoxShellTable();

    table2.title("Right Table").header("Column D").header("Column E").header("Column F").header("Column G");

    table2.row().value("123").value("367").value("244444444").value("2");
    table2.row().value("780").value("908").value("944444444").value("3");

    KnoxShellTable joined = KnoxShellTable.builder().join().title("Joined Table").left(table).right(table2).on(0, 0);

    assertEquals(joined.getRows().size(), 1);
    assertEquals(joined.getTitle(), "Joined Table");
    assertEquals(joined.cell(0, 0).value, "123");
    String json = joined.toJSON();

    KnoxShellTable zombie = KnoxShellTable.builder().json().fromJson(json);
    zombie.title("Zombie Table");

    assertEquals(zombie.getRows().size(), 1);
    assertEquals(zombie.getTitle(), "Zombie Table");
    assertEquals(zombie.cell(0, 0).value, "123");
    KnoxShellTable joined2 = KnoxShellTable.builder().join().title("Joined Table 2").left(table).right(table2).on(1, 3);
    assertEquals(1, joined2.getRows().size());
  }

  @Test
  public void testJDBCBuilderUnManagedConnection() throws Exception {
    Connection connection = createMock(Connection.class);
    Statement statement = createMock(Statement.class);
    ResultSet resultSet = createMock(ResultSet.class);
    ResultSetMetaData metadata = createMock(ResultSetMetaData.class);
    try {
    expect(connection.createStatement()).andReturn(statement);
    expect(statement.executeQuery("select * from book")).andReturn(resultSet);
    expect(resultSet.getMetaData()).andReturn(metadata);
    if (resultSet.next()) {
      // intentionally empty in order to pass PMC and checkstyle!
    }
    expectLastCall().andAnswer(new IAnswer<Boolean>() {
      int called;
      @Override
      public Boolean answer() {
        called++;
        if (called < 2) {
          return true;
        }
        return false;
      }
    }).times(2);
    expect(resultSet.getObject("BOOK_ID", Comparable.class)).andReturn("1").times(1);
    expect(resultSet.getObject("TITLE", Comparable.class)).andReturn("Apache Knox: The Definitive Guide").times(1);
    expect(metadata.getTableName(1)).andReturn("BOOK");
    expect(metadata.getColumnCount()).andReturn(2);
    expect(metadata.getColumnName(1)).andReturn("BOOK_ID").anyTimes();
    expect(metadata.getColumnName(2)).andReturn("TITLE").anyTimes();
    resultSet.close();
    expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() {
          return null;
      }
    }).times(2);
    // should close the statement
    statement.close();
    expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() {
          return null;
      }
    }).times(2);
    connection.close();
    expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() {
          return null;
      }
    });
    replay(connection, statement, resultSet, metadata);

    KnoxShellTable table = KnoxShellTable.builder().jdbc().connection(connection).sql("select * from book");
    assertEquals(1, table.getRows().size());
    }
    finally {
      connection.close();
//      expectLastCall().andAnswer(new IAnswer<Object>() {
//        @Override
//        public Object answer() {
//            return null;
//        }
//      });
      statement.close();
      resultSet.close();
    }
    verify(connection, statement, resultSet, metadata);
  }

  @Test (expected = IllegalArgumentException.class)
  public void testConversion() throws IllegalArgumentException {
      KnoxShellTable TABLE = new KnoxShellTable();
      TABLE.header("Column Integer").header("Column String").header("Column Null");
      TABLE.row().value(10).value("John").value(null);

      System.out.println(TABLE.mean("Column String"));
      System.out.println(TABLE.mean("Column Null"));

  }

  @Test
  public void testMean() throws Exception {

      Byte byte10 = 10;
      Byte byte20 = 20;
      Byte byte30 = 30;

      Short short10 = 10;
      Short short20 = 20;
      Short short30 = 30;

      Long long10 = (long) 100000000;
      Long long20 = (long) 200000000;
      Long long30 = (long) 300000000;

      String string10 = "1023.98000000000";
      String string20 = "2089743.6";
      String string30 = "-3";

      KnoxShellTable TABLE = new KnoxShellTable();
      TABLE.header("Column Integer").header("Column Double").header("Column Float").header("Column Byte").header("Column Short").header("Column Long").header("Column String");
      TABLE.row().value(10).value(20d).value(20.9999).value(byte10).value(short10).value(long10).value(string10);
      TABLE.row().value(30).value(40d).value(40.9999).value(byte20).value(short20).value(long20).value(string20);
      TABLE.row().value(27).value(60d).value(60.9999).value(byte30).value(short30).value(long30).value(string30);

      assertEquals(22.3, TABLE.mean("Column Integer"), 0.1);
      assertEquals(40, TABLE.mean("Column Double"), 0.1);
      assertEquals(22.3, TABLE.mean(0), 0.1);
      assertEquals(40, TABLE.mean(1), 0.1);
      assertEquals(40.9999, TABLE.mean("Column Float"), 0.1);
      assertEquals(40.9999, TABLE.mean(2), 0.1);
      assertEquals(20, TABLE.mean("Column Byte"), 0.1);
      assertEquals(20, TABLE.mean("Column Short"), 0.1);
      assertEquals(2.0E8, TABLE.mean("Column Long"), 0.1);
      assertEquals(696921.52666666, TABLE.mean("Column String"), 0.1);
  }

  @Test
  public void testMedian() throws Exception {

      KnoxShellTable TABLE = new KnoxShellTable();
      TABLE.header("Column Integer").header("Column Double").header("Column Float");
      TABLE.row().value(10).value(20d).value(40.9999);
      assertEquals(10, TABLE.median("Column Integer"), 0.1);
      assertEquals(10, TABLE.median(0), 0.1);
      assertEquals(20, TABLE.median("Column Double"), 0.1);
      assertEquals(20, TABLE.median(1), 0.1);
      assertEquals(40.9999, TABLE.mean("Column Float"), 0.1);
      assertEquals(40.9999, TABLE.mean(2), 0.1);

      KnoxShellTable TABLE2 = new KnoxShellTable();
      TABLE2.header("Column Integer").header("Column Double").header("Column Float");
      TABLE2.row().value(10).value(20d).value(20.9999);
      TABLE2.row().value(30).value(40d).value(40.9999);
      TABLE2.row().value(50).value(41d).value(60.9999);
      assertEquals(30, TABLE2.median("Column Integer"), 0.1);
      assertEquals(30, TABLE2.median(0), 0.1);
      assertEquals(40.9999, TABLE2.mean("Column Float"), 0.1);
      assertEquals(40.9999, TABLE2.mean(2), 0.1);

      KnoxShellTable TABLE3 = new KnoxShellTable();
      TABLE3.header("Column Integer").header("Column Double").header("Column Float");
      TABLE3.row().value(10).value(20d).value(0.0000);
      TABLE3.row().value(30).value(40d).value(30.0000);
      TABLE3.row().value(31).value(41d).value(31.0000);
      TABLE3.row().value(50).value(60d).value(70.0000);
      assertEquals(30.5, TABLE3.median("Column Integer"), 0.1);
      assertEquals(30.5, TABLE3.median(0), 0.01);
      assertEquals(40.5, TABLE3.median("Column Double"), 0.01);
      assertEquals(40.5, TABLE3.median(1), 0.01);
      assertEquals(30.5000, TABLE3.median("Column Float"), 0.1);
      assertEquals(30.5000, TABLE3.median(2), 0.1);
  }

  @Test
  public void testMode() throws Exception {

      KnoxShellTable TABLE = new KnoxShellTable();
      TABLE.header("Column Integer").header("Column Double").header("Column Float");
      TABLE.row().value(10).value(20d).value(40.9999);
      assertEquals(10, TABLE.mode("Column Integer"), 0.1);
      assertEquals(10, TABLE.mode(0), 0.1);
      assertEquals(40.9999, TABLE.mean("Column Float"), 0.1);
      assertEquals(40.9999, TABLE.mean(2), 0.1);


      KnoxShellTable TABLE2 = new KnoxShellTable();
      TABLE2.header("Column Integer").header("Column Double").header("Column Float");
      TABLE2.row().value(10).value(20d).value(20.9999);
      TABLE2.row().value(30).value(40d).value(20.9999);
      TABLE2.row().value(30).value(40d).value(30.9999);
      assertEquals(30, TABLE2.median("Column Integer"), 0.1);
      assertEquals(30, TABLE2.median(0), 0.1);
      assertEquals(40, TABLE2.median("Column Double"), 0.1);
      assertEquals(40, TABLE2.median(1), 0.1);
      assertEquals(20.9999, TABLE2.median("Column Float"), 0.1);
      assertEquals(20.9999, TABLE2.median(2), 0.1);
  }

  @Test
  public void testSum() throws Exception {

      KnoxShellTable TABLE = new KnoxShellTable();
      TABLE.header("Column Integer").header("Column Double").header("Column Float");
      TABLE.row().value(10).value(20d).value(20.5000);
      TABLE.row().value(90).value(100d).value(20.0500);
      assertEquals(100, TABLE.sum("Column Integer"), 0.1);
      assertEquals(100, TABLE.sum(0), 0.1);
      assertEquals(120, TABLE.sum("Column Double"), 0.1);
      assertEquals(120, TABLE.sum(1), 0.1);
      assertEquals(40.5500, TABLE.sum("Column Float"), 0.1);
      assertEquals(40.5500, TABLE.sum(2), 0.1);
  }

  @Test
  public void testMax() throws Exception {

      KnoxShellTable TABLE = new KnoxShellTable();
      TABLE.header("Column Integer").header("Column Double").header("Column Float");
      TABLE.row().value(10).value(20d).value(30.9998);
      TABLE.row().value(20).value(50d).value(30.9999);
      assertEquals(20, TABLE.max("Column Integer"), 0.1);
      assertEquals(20, TABLE.max(0), 0.1);
      assertEquals(50, TABLE.max("Column Double"), 0.1);
      assertEquals(50, TABLE.max(1), 0.1);
      assertEquals(30.9999, TABLE.max("Column Float"), 0.1);
      assertEquals(30.9999, TABLE.max(2), 0.1);
  }

  @Test
  public void testMin() throws Exception {

      KnoxShellTable TABLE = new KnoxShellTable();
      TABLE.header("Column Integer").header("Column Double").header("Column Float");
      TABLE.row().value(10).value(20d).value(20.9998);
      TABLE.row().value(20).value(50d).value(20.9999);
      assertEquals(10, TABLE.min("Column Integer"), 0.1);
      assertEquals(10, TABLE.min(0), 0.1);
      assertEquals(20, TABLE.min("Column Double"), 0.1);
      assertEquals(20, TABLE.min(1), 0.1);
      assertEquals(20.9998, TABLE.min("Column Float"), 0.1);
      assertEquals(20.9998, TABLE.min(2), 0.1);
  }

  @Test
  public void shouldReturnDifferentCallHistoryForDifferentTables() throws Exception {
    final KnoxShellTable table1 = new KnoxShellTable();
    KnoxShellTableCallHistory.getInstance().saveCall(table1.getId(), new KnoxShellTableCall("class1", "method1", true, Collections.singletonMap("param1", String.class)));
    final KnoxShellTable table2 = new KnoxShellTable();
    KnoxShellTableCallHistory.getInstance().saveCall(table2.getId(), new KnoxShellTableCall("class2", "method2", false, Collections.singletonMap("param2", String.class)));
    assertNotEquals(table1.getCallHistoryList(), table2.getCallHistoryList());
  }
}
