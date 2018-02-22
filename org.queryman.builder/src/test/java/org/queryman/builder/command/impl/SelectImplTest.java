package org.queryman.builder.command.impl;

import org.junit.jupiter.api.Test;
import org.queryman.builder.command.select.SelectFromStep;
import org.queryman.builder.command.select.SelectJoinStep;
import org.queryman.builder.token.Expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.queryman.builder.Operators.EQUAL;
import static org.queryman.builder.Operators.IN;
import static org.queryman.builder.Operators.LT;
import static org.queryman.builder.Operators.NE2;
import static org.queryman.builder.Operators.NOT_IN;
import static org.queryman.builder.PostgreSQL.asConstant;
import static org.queryman.builder.PostgreSQL.asFunc;
import static org.queryman.builder.PostgreSQL.asName;
import static org.queryman.builder.PostgreSQL.asNumber;
import static org.queryman.builder.PostgreSQL.asQuotedName;
import static org.queryman.builder.PostgreSQL.asString;
import static org.queryman.builder.PostgreSQL.asSubQuery;
import static org.queryman.builder.PostgreSQL.condition;
import static org.queryman.builder.PostgreSQL.conditionBetween;
import static org.queryman.builder.PostgreSQL.fromOnly;
import static org.queryman.builder.PostgreSQL.max;
import static org.queryman.builder.PostgreSQL.operator;
import static org.queryman.builder.PostgreSQL.select;
import static org.queryman.builder.PostgreSQL.selectAll;
import static org.queryman.builder.PostgreSQL.selectDistinct;
import static org.queryman.builder.PostgreSQL.selectDistinctOn;

class SelectImplTest {
    @Test
    void selectTest() {
        SelectFromStep select = select("id", 2, .4, "name");

        assertEquals("SELECT id, 2, 0.4, name", select.sql());

        SelectFromStep select2 = select(asQuotedName("id2"), asQuotedName("name"), asFunc("min", asName("price")).as("min"));
        assertEquals("SELECT \"id2\", \"name\", min(price) AS min", select2.sql());

        SelectFromStep select3 = select(asQuotedName("id2"), asSubQuery(select("max(sum)")).as("sum"));
        assertEquals("SELECT \"id2\", (SELECT max(sum)) AS sum", select3.sql());

        assertEquals("SELECT MAX(total) FROM order", select(max("total")).from("order").sql());
    }

    @Test
    void selectAllTest() {
        SelectFromStep select = selectAll("id", "name");
        assertEquals("SELECT ALL id, name", select.sql());

        SelectFromStep select2 = selectAll(asQuotedName("id2"), asQuotedName("name"), asConstant("min(price) as min"));
        assertEquals("SELECT ALL \"id2\", \"name\", min(price) as min", select2.sql());
    }

    @Test
    void selectDistinctTest() {
        SelectFromStep select = selectDistinct("id", "name");
        assertEquals("SELECT DISTINCT id, name", select.sql());

        select = selectDistinct(asQuotedName("id2"), asQuotedName("name"), asConstant("min(price) as min"));
        assertEquals("SELECT DISTINCT \"id2\", \"name\", min(price) as min", select.sql());
    }

    @Test
    void selectDistinctOnTest() {
        SelectFromStep select = selectDistinctOn(new String[]{ "price", "id" }, "id", "name");
        assertEquals("SELECT DISTINCT ON (price, id) id, name", select.sql());

        select = selectDistinctOn(new Expression[]{ asName("price"), asName("id") }, asQuotedName("id2"), asQuotedName("name"), asConstant("min(price) as min"));
        assertEquals("SELECT DISTINCT ON (price, id) \"id2\", \"name\", min(price) as min", select.sql());
    }

    @Test
    void selectFrom() {
        SelectFromStep select = select("id", "name");

        assertEquals("SELECT id, name FROM book", select.from("book").sql());

        assertEquals("SELECT id, name FROM book as b(1,2)", select.from("book as b(1,2)").sql());

        assertEquals("SELECT id, name FROM \"book\"", select.from(asQuotedName("book")).sql());

        assertEquals("SELECT id, name FROM public.book", select.from(asName("public.book")).sql());

        assertEquals("SELECT id, name FROM table1, table2", select.from("table1", "table2").sql());
    }

    @Test
    void selectFromOnly() {
        SelectFromStep select = select("id", "name");

        assertEquals("SELECT id, name FROM ONLY book", select.from(fromOnly("book")).sql());
        assertEquals("SELECT id, name FROM ONLY book, ONLY authors", select.from(fromOnly("book"), fromOnly("authors")).sql());
        assertEquals("SELECT id, name FROM ONLY book AS b", select.from(fromOnly("book").as("b")).sql());
        assertEquals(
           "SELECT id, name FROM ONLY book TABLESAMPLE BERNOULLI(30)",
           select.from(
              fromOnly("book")
                 .tablesample("BERNOULLI", "30")
           )
              .sql()
        );
        assertEquals(
           "SELECT id, name FROM ONLY book TABLESAMPLE BERNOULLI(30) REPEATABLE(15)",
           select.from(fromOnly("book")
              .tablesample("BERNOULLI", "30")
              .repeatable(15))
              .sql()
        );
    }

    @Test
    void selectFromTablesample() {

    }

    @Test
    void selectFromJoin() {
        SelectJoinStep select = select("id", "name").from("book");

        assertEquals("SELECT id, name FROM book JOIN author ON (true)", select.join("author").on(true).sql());

        select = select("id", "name").from("book");
        assertEquals("SELECT id, name FROM book JOIN author USING (id)", select.join("author").using("id").sql());

        select = select("id", "name").from("book");
        assertEquals("SELECT id, name FROM book JOIN author USING (id, name)", select.join("author").using("id", "name").sql());

        select = select("id", "name").from("book");
        assertEquals("SELECT id, name FROM book JOIN author ON id = author_id", select.join("author").on("id", "=", "author_id").sql());

        select = select("id", "name").from("book").join("author").on("id", "=", "author_id").andExists(select("1", "2"));
        assertEquals("SELECT id, name FROM book JOIN author ON id = author_id AND EXISTS (SELECT 1, 2)", select.sql());

        select = select("id", "name").from("book").join("author").on("id", "=", "author_id").and(asName("id"), IN, select("1", "2"));
        assertEquals("SELECT id, name FROM book JOIN author ON id = author_id AND id IN (SELECT 1, 2)", select.sql());

        select = select("id", "name").from("book").join("author").on("id", "=", "author_id").andNot(asName("id"), IN, select("1", "2"));
        assertEquals("SELECT id, name FROM book JOIN author ON id = author_id AND NOT id IN (SELECT 1, 2)", select.sql());

        select = select("id", "name").from("book").join("author").on("id", "=", "author_id").or(asName("id"), IN, select("1", "2"));
        assertEquals("SELECT id, name FROM book JOIN author ON id = author_id OR id IN (SELECT 1, 2)", select.sql());

        select = select("id", "name").from("book").join("author").on("id", "=", "author_id").orNot(asName("id"), IN, select("1", "2"));
        assertEquals("SELECT id, name FROM book JOIN author ON id = author_id OR NOT id IN (SELECT 1, 2)", select.sql());

        select = select("*").from("book");
        assertEquals("SELECT * FROM book JOIN author ON EXISTS (SELECT 1, 2)", select.join("author").onExists(select("1", "2")).sql());

        select = select("*").from("book").join("author").on(true).innerJoin("sales").onExists(select("1", "2"));
        assertEquals("SELECT * FROM book JOIN author ON (true) INNER JOIN sales ON EXISTS (SELECT 1, 2)", select.sql());
    }

    @Test
    void selectFromInnerJoin() {
        SelectJoinStep select = select("id", "name").from("book");

        assertEquals("SELECT id, name FROM book INNER JOIN author ON (true)", select.innerJoin("author").on(true).sql());

        select = select("*").from("book").innerJoin("author").on(true).join("sales").onExists(select("1", "2"));
        assertEquals("SELECT * FROM book INNER JOIN author ON (true) JOIN sales ON EXISTS (SELECT 1, 2)", select.sql());
    }

    @Test
    void selectFromLeftJoin() {
        SelectJoinStep select = select("id", "name").from("book");
        assertEquals("SELECT id, name FROM book LEFT JOIN author ON (true)", select.leftJoin("author").on(true).sql());

        select = select("*").from("book").leftJoin("author").on(true).innerJoin(asName("sales")).onExists(select("1", "2"));
        assertEquals("SELECT * FROM book LEFT JOIN author ON (true) INNER JOIN sales ON EXISTS (SELECT 1, 2)", select.sql());
    }

    @Test
    void selectFromRightJoin() {
        SelectJoinStep select = select("id", "name").from("book");
        assertEquals("SELECT id, name FROM book RIGHT JOIN author ON (true)", select.rightJoin("author").on(true).sql());

        select = select("*").from("book").rightJoin("author").on(true).leftJoin(asName("sales")).onExists(select("1", "2"));
        assertEquals("SELECT * FROM book RIGHT JOIN author ON (true) LEFT JOIN sales ON EXISTS (SELECT 1, 2)", select.sql());
    }

    @Test
    void selectFromFullJoin() {
        SelectJoinStep select = select("id", "name").from("book");
        assertEquals("SELECT id, name FROM book FULL JOIN author ON (true)", select.fullJoin("author").on(true).sql());

        select = select("*").from("book").fullJoin("author").on(true).rightJoin(asName("sales")).onExists(select("1", "2"));
        assertEquals("SELECT * FROM book FULL JOIN author ON (true) RIGHT JOIN sales ON EXISTS (SELECT 1, 2)", select.sql());
    }

    @Test
    void selectFromCrossJoin() {
        SelectJoinStep select = select("id", "name").from("book");
        assertEquals("SELECT id, name FROM book CROSS JOIN author", select.crossJoin("author").sql());

        SelectJoinStep select1 = select("*").from("book");
        select1.crossJoin("author").fullJoin(asName("sales")).onExists(select("1", "2"));
        assertEquals("SELECT * FROM book CROSS JOIN author FULL JOIN sales ON EXISTS (SELECT 1, 2)", select1.sql());
    }

    @Test
    void selectFromNaturalJoin() {
        SelectJoinStep select = select("id", "name").from("book");
        assertEquals("SELECT id, name FROM book NATURAL JOIN author", select.naturalJoin("author").sql());

        SelectJoinStep select1 = select("*").from("book");
        select1.crossJoin("author").naturalJoin(asName("sales")).crossJoin(asName("calls"));
        assertEquals("SELECT * FROM book CROSS JOIN author NATURAL JOIN sales CROSS JOIN calls", select1.sql());
    }

    //---
    // WHERE
    //---

    @Test
    void selectFromWhere() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .where("id", "=", "1")
           .and("id2", "=", "2")
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 AND id2 = 2", sql);

        sql = select.from("book")
           .where(asQuotedName("id"), EQUAL, asNumber(1))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE \"id\" = 1", sql);

        sql = select.from("book")
           .where(asName("id"), IN, select("1", "2"))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id IN (SELECT 1, 2)", sql);

        sql = select.from("book")
           .whereExists(select("1", "2"))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE EXISTS (SELECT 1, 2)", sql);

        sql = select.from("book")
           .where("id", "=", "1")
           .andNot("id2", "=", "2")
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 AND NOT id2 = 2", sql);

        sql = select.from("book")
           .where("id", "=", "1")
           .orNot("id3", "=", "3")
           .andNot("id2", "=", "2")
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 OR NOT id3 = 3 AND NOT id2 = 2", sql);

        sql = select.from("book")
           .where(asName("id1"), EQUAL, asString("1"))
           .or(asQuotedName("id2"), EQUAL, asNumber(2))
           .orNot(asName("table.id3"), EQUAL, asNumber(3))
           .and(asQuotedName("table.id4"), EQUAL, asNumber(4))
           .andNot(asQuotedName("id5"), EQUAL, asNumber(5))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id1 = '1' OR \"id2\" = 2 OR NOT table.id3 = 3 AND \"table\".\"id4\" = 4 AND NOT \"id5\" = 5", sql);
    }

    @Test
    void selectFromWhereBetween() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .where(conditionBetween("id", "1", "2"))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id BETWEEN 1 AND 2", sql);

        sql = select.from("book")
           .where(conditionBetween(asQuotedName("id"), asNumber(3), asNumber(4)))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE \"id\" BETWEEN 3 AND 4", sql);

        sql = select.from("book")
           .where(conditionBetween(asQuotedName("id"), asNumber(3), asNumber(4)))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE \"id\" BETWEEN 3 AND 4", sql);
    }

    @Test
    void selectFromWhereAnd() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .where("id", "=", "1")
           .and("id2", "=", "2")
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 AND id2 = 2", sql);

        sql = select.from("book")
           .where("id", "=", "1")
           .and(asQuotedName("id2"), EQUAL, asNumber(2))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 AND \"id2\" = 2", sql);

        sql = select.from("book")
           .where("id", "=", "1")
           .and(condition(asQuotedName("id2"), EQUAL, asNumber(4)))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 AND \"id2\" = 4", sql);

        sql = select.from("book")
           .where("id", "=", "1")
           .and(asQuotedName("id2"), NOT_IN, select("1", "2"))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 AND \"id2\" NOT IN (SELECT 1, 2)", sql);

        sql = select.from("book")
           .where("id", "=", "1")
           .andExists(select("1", "2"))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 AND EXISTS (SELECT 1, 2)", sql);
    }

    @Test
    void selectFromWhereAndNot() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .where("id", "=", "1")
           .andNot("id2", "!=", "2")
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 AND NOT id2 != 2", sql);

        sql = select.from("book")
           .where("id", "=", "1")
           .andNot(asQuotedName("id2"), EQUAL, asNumber(3))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 AND NOT \"id2\" = 3", sql);

        sql = select.from("book")
           .where("id", "=", "1")
           .andNot(condition(asQuotedName("id2"), EQUAL, asNumber(4)))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 AND NOT \"id2\" = 4", sql);

        sql = select.from("book")
           .where("id", "=", "1")
           .andNot(asQuotedName("id2"), NOT_IN, select("1", "2"))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 AND NOT \"id2\" NOT IN (SELECT 1, 2)", sql);

        sql = select.from("book")
           .where("id", "=", "1")
           .andNotExists(select("1", "2"))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 AND NOT EXISTS (SELECT 1, 2)", sql);
    }

    @Test
    void selectFromWhereOr() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .where("id", "=", "1")
           .or("id2", "=", "2")
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 OR id2 = 2", sql);

        sql = select.from("book")
           .where("id", "<>", "1")
           .or(asQuotedName("id2"), NE2, asNumber(3))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id <> 1 OR \"id2\" <> 3", sql);

        sql = select.from("book")
           .where("id", "=", "1")
           .or(condition(asQuotedName("id2"), LT, asNumber(4)))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 OR \"id2\" < 4", sql);

        sql = select.from("book")
           .where("id", "=", "1")
           .or(asQuotedName("id2"), NOT_IN, select("1", "2"))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 OR \"id2\" NOT IN (SELECT 1, 2)", sql);

        sql = select.from("book")
           .where("id", "=", "1")
           .orExists(select("1", "2"))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 OR EXISTS (SELECT 1, 2)", sql);
    }

    @Test
    void selectFromWhereOrNot() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .where("id", "=", "1")
           .orNot("id2", "=", "2")
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 OR NOT id2 = 2", sql);

        sql = select.from("book")
           .where("id", "=", "1")
           .orNot(asQuotedName("id2"), EQUAL, asNumber(3))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 OR NOT \"id2\" = 3", sql);

        sql = select.from("book")
           .where("id", "=", "1")
           .orNot(condition(asQuotedName("id2"), EQUAL, asNumber(4)))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 OR NOT \"id2\" = 4", sql);

        sql = select.from("book")
           .where("id", "=", "1")
           .orNot(asQuotedName("id2"), NOT_IN, select("1", "2"))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 OR NOT \"id2\" NOT IN (SELECT 1, 2)", sql);

        sql = select.from("book")
           .where("id", "=", "1")
           .orNotExists(select("1", "2"))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 OR NOT EXISTS (SELECT 1, 2)", sql);
    }

    @Test
    void selectFromWhereGroup() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("user")
           .where(condition("name", "=", "timur")
              .and("phone", "is", "null")
              .or("email", "=", asString("timur@shaidullin.net").getName())
              .and(condition("id", "!=", "3")
                 .and("name", "is not", asString("max").getName())
              )
           )
           .or("id", "=", "1")
           .sql();

        assertEquals("SELECT id, name FROM user WHERE (name = timur AND phone is null OR email = 'timur@shaidullin.net' AND (id != 3 AND name is not 'max')) OR id = 1", sql);

        sql = select.from("user")
           .where(condition("name", "=", "timur")
              .and("phone", "is", "null")
           )
           .or("id", "=", "1")
           .and(condition("id", "!=", "3")
              .and("name", "is not", "max")
           )
           .sql();

        assertEquals("SELECT id, name FROM user WHERE (name = timur AND phone is null) OR id = 1 AND (id != 3 AND name is not max)", sql);
    }

    @Test
    void selectFromWhereAndWhereGroup() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("user")
           .where("id", "=", "1")
           .and(condition("name", "=", "timur")
              .and("phone", "is", "null")
              .or("email", "=", "'timur@shaidullin.net'")
           )
           .and("id2", "=", "2")
           .sql();

        assertEquals("SELECT id, name FROM user WHERE id = 1 AND (name = timur AND phone is null OR email = 'timur@shaidullin.net') AND id2 = 2", sql);

        sql = select("*")
           .from("book")
           .where("year", ">", "2010")
           .orNot(
              conditionBetween("id", "1", "10")
                 .and(asName("name"), operator("="), asString("Advanced SQL"))
           )
           .sql();

        assertEquals("SELECT * FROM book WHERE year > 2010 OR NOT (id BETWEEN 1 AND 10 AND name = 'Advanced SQL')", sql);
    }

    //---
    // GROUP BY
    //---

    @Test
    void selectFromGroupBy() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .groupBy("id", "name")
           .sql();
        assertEquals("SELECT id, name FROM book GROUP BY id, name", sql);

        select = select("id", "name");
        sql = select.from("book")
           .groupBy(asFunc("ROLLUP", "id", "name"), asFunc("CUBE", "id", "name"))
           .sql();
        assertEquals("SELECT id, name FROM book GROUP BY ROLLUP(id, name), CUBE(id, name)", sql);
    }

    @Test
    void selectFromWhereGroupBy() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .where("id", "=", "1")
           .groupBy("id")
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 GROUP BY id", sql);
    }

    //---
    // HAVING
    //---

    @Test
    void selectFromHaving() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .having("id", "=", "1")
           .and("id2", "=", "2")
           .sql();
        assertEquals("SELECT id, name FROM book HAVING id = 1 AND id2 = 2", sql);

        sql = select.from("book")
           .having(asQuotedName("id"), EQUAL, asNumber(1))
           .sql();
        assertEquals("SELECT id, name FROM book HAVING \"id\" = 1", sql);

        sql = select.from("book")
           .having(asName("id"), IN, select("1", "2"))
           .sql();
        assertEquals("SELECT id, name FROM book HAVING id IN (SELECT 1, 2)", sql);

        sql = select.from("book")
           .havingExists(select("1", "2"))
           .sql();
        assertEquals("SELECT id, name FROM book HAVING EXISTS (SELECT 1, 2)", sql);

        sql = select.from("book")
           .having("id", "=", "1")
           .andNot("id2", "=", "2")
           .sql();
        assertEquals("SELECT id, name FROM book HAVING id = 1 AND NOT id2 = 2", sql);

        sql = select.from("book")
           .having("id", "=", "1")
           .orNot("id3", "=", "3")
           .andNot("id2", "=", "2")
           .sql();
        assertEquals("SELECT id, name FROM book HAVING id = 1 OR NOT id3 = 3 AND NOT id2 = 2", sql);

        sql = select.from("book")
           .having(asName("id1"), EQUAL, asString("1"))
           .or(asQuotedName("id2"), EQUAL, asNumber(2))
           .orNot(asName("table.id3"), EQUAL, asNumber(3))
           .and(asQuotedName("table.id4"), EQUAL, asNumber(4))
           .andNot(asQuotedName("id5"), EQUAL, asNumber(5))
           .sql();
        assertEquals("SELECT id, name FROM book HAVING id1 = '1' OR \"id2\" = 2 OR NOT table.id3 = 3 AND \"table\".\"id4\" = 4 AND NOT \"id5\" = 5", sql);
    }

    @Test
    void selectFromWhereHaving() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .where("id", "=", "1")
           .having(asName("name"), LT, asString("Anna"))
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 HAVING name < 'Anna'", sql);
    }

    @Test
    void selectFromHavingBetween() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .having(conditionBetween("id", "1", "2"))
           .sql();
        assertEquals("SELECT id, name FROM book HAVING id BETWEEN 1 AND 2", sql);

        sql = select.from("book")
           .having(conditionBetween(asQuotedName("id"), asNumber(3), asNumber(4)))
           .sql();
        assertEquals("SELECT id, name FROM book HAVING \"id\" BETWEEN 3 AND 4", sql);

        sql = select.from("book")
           .having(conditionBetween(asQuotedName("id"), asNumber(3), asNumber(4)))
           .sql();
        assertEquals("SELECT id, name FROM book HAVING \"id\" BETWEEN 3 AND 4", sql);
    }

    @Test
    void selectFromWhereGroupByHaving() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .where("id", "=", "1")
           .groupBy("id")
           .having("id", "=", "2")
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 GROUP BY id HAVING id = 2", sql);
    }

    @Test
    void selectFromWhereGroupByHavingOrderBy() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .where("id", "=", "1")
           .groupBy("id")
           .having("id", "=", "2")
           .or("id", "=", "2")
           .orderBy("id")
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 GROUP BY id HAVING id = 2 OR id = 2 ORDER BY id", sql);
    }

    @Test
    void selectFromWhereGroupByHavingLimit() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .where("id", "=", "1")
           .groupBy("id")
           .having("id", "=", "2")
           .or("id", "=", "2")
           .limit(1)
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 GROUP BY id HAVING id = 2 OR id = 2 LIMIT 1", sql);
    }

    @Test
    void selectFromWhereGroupByHavingOffset() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .where("id", "=", "1")
           .groupBy("id")
           .having("id", "=", "2")
           .offset(1)
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 GROUP BY id HAVING id = 2 OFFSET 1", sql);
    }

    //---
    // ORDER BY
    //---

    @Test
    void selectFromOrderBy() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .orderBy("id")
           .sql();

        assertEquals("SELECT id, name FROM book ORDER BY id", sql);

        sql = select.from("book")
           .orderBy("name", "desc")
           .sql();

        assertEquals("SELECT id, name FROM book ORDER BY name desc", sql);

        sql = select.from("book")
           .orderBy("name", "desc", "nulls last")
           .sql();

        assertEquals("SELECT id, name FROM book ORDER BY name desc nulls last", sql);
    }

    @Test
    void selectFromWhereGroupByOrderBy() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .where("id", "=", "1")
           .groupBy("id")
           .orderBy("name")
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 GROUP BY id ORDER BY name", sql);
    }

    @Test
    void selectFromWhereOrderBy() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .where("id", "=", "1")
           .orderBy("name")
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 ORDER BY name", sql);

        sql = select.from("book")
           .where("id", "=", "1")
           .and("id2", "=", "2")
           .orderBy("id")
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 AND id2 = 2 ORDER BY id", sql);
    }

    //---
    // LIMIT
    //---

    @Test
    void selectFromLimit() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .limit(1)
           .sql();

        assertEquals("SELECT id, name FROM book LIMIT 1", sql);
    }

    @Test
    void selectFromOrderByLimit() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .orderBy("id")
           .limit(2)
           .sql();

        assertEquals("SELECT id, name FROM book ORDER BY id LIMIT 2", sql);
    }

    @Test
    void selectFromGroupByLimit() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .groupBy("id")
           .limit(2)
           .sql();

        assertEquals("SELECT id, name FROM book GROUP BY id LIMIT 2", sql);
    }

    @Test
    void selectFromWhereLimit() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .where("id", "=", "1")
           .and("id2", "=", "2")
           .limit(3)
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 AND id2 = 2 LIMIT 3", sql);
    }

    //---
    // OFFSET
    //---

    @Test
    void selectFromOffset() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .limit(1)
           .sql();

        assertEquals("SELECT id, name FROM book LIMIT 1", sql);
    }

    @Test
    void selectFromGroupByOffset() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .groupBy("id")
           .limit(1)
           .sql();

        assertEquals("SELECT id, name FROM book GROUP BY id LIMIT 1", sql);
    }

    @Test
    void selectFromWhereOffset() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .where("id", "=", "1")
           .and("id2", "=", "2")
           .offset(3)
           .sql();
        assertEquals("SELECT id, name FROM book WHERE id = 1 AND id2 = 2 OFFSET 3", sql);
    }

    @Test
    void selectFromWhereLimitOffset() {
        SelectFromStep select = select("id", "name");
        String sql = select.from("book")
           .where("id", "=", "1")
           .and("id2", "=", "2")
           .limit(3)
           .offset(3)
           .sql();

        assertEquals("SELECT id, name FROM book WHERE id = 1 AND id2 = 2 LIMIT 3 OFFSET 3", sql);
    }

    //---
    // UNION
    //---

    @Test
    void selectUnion() {
        String sql = select("1", "2").union(select("2", "2")).sql();
        assertEquals("SELECT 1, 2 UNION SELECT 2, 2", sql);
    }

    @Test
    void selectFromUnion() {
        SelectFromStep select = select("1", "2");
        select
           .from("book")
           .union(select("2", "2"));
        String sql = select.sql();
        assertEquals("SELECT 1, 2 FROM book UNION SELECT 2, 2", sql);

        select = select("1", "2");
        select
           .from("book")
           .crossJoin("author")
           .union(select("2", "2"));
        sql = select.sql();
        assertEquals("SELECT 1, 2 FROM book CROSS JOIN author UNION SELECT 2, 2", sql);

        select = select("1", "2");
        select
           .from("book")
           .join("author").on(true)
           .union(select("2", "2"));
        sql = select.sql();
        assertEquals("SELECT 1, 2 FROM book JOIN author ON (true) UNION SELECT 2, 2", sql);
    }

    @Test
    void selectFromWhereUnion() {
        SelectFromStep select = select("1", "2");
        select
           .from("book")
           .where(conditionBetween(asString("id"), asNumber(1), asNumber(2)))
           .union(select("2", "2"));

        assertEquals("SELECT 1, 2 FROM book WHERE 'id' BETWEEN 1 AND 2 UNION SELECT 2, 2", select.sql());

        select = select("1", "2");
        select
           .from("book")
           .where(conditionBetween(asString("id"), asNumber(1), asNumber(2)))
           .and("name", "IS NOT", null)
           .union(select("2", "2"));

        assertEquals("SELECT 1, 2 FROM book WHERE 'id' BETWEEN 1 AND 2 AND name IS NOT null UNION SELECT 2, 2", select.sql());
    }

    @Test
    void selectFromWhereGroupByUnion() {
        SelectFromStep select = select("1", "2");
        select
           .from("book")
           .where(conditionBetween(asString("id"), asNumber(1), asNumber(2)))
           .groupBy(asQuotedName("book.id"))
           .union(select("2", "2"));

        String sql = select.sql();
        assertEquals("SELECT 1, 2 FROM book WHERE 'id' BETWEEN 1 AND 2 GROUP BY \"book\".\"id\" UNION SELECT 2, 2", sql);
    }

    @Test
    void selectFromWhereGroupByUnionOrderBy() {
        SelectFromStep select = select("1", "2");
        select
           .from("book")
           .where(conditionBetween(asString("id"), asNumber(1), asNumber(2)))
           .groupBy(asQuotedName("book.id"))
           .union(select("2", "2"))
           .orderBy("id", "DESC");

        String sql = select.sql();
        assertEquals("SELECT 1, 2 FROM book WHERE 'id' BETWEEN 1 AND 2 GROUP BY \"book\".\"id\" UNION SELECT 2, 2 ORDER BY id DESC", sql);
    }

    @Test
    void selectUnionAll() {
        String sql = select("1", "2").unionAll(select("2", "2")).sql();
        assertEquals("SELECT 1, 2 UNION ALL SELECT 2, 2", sql);
    }

    @Test
    void selectUnionDistinct() {
        String sql = select("1", "2").unionDistinct(select("2", "2")).sql();
        assertEquals("SELECT 1, 2 UNION DISTINCT SELECT 2, 2", sql);
    }

    //---
    // INTERSECT
    //---

    @Test
    void selectIntersect() {
        String sql = select("1", "2").intersect(select("2", "2")).sql();
        assertEquals("SELECT 1, 2 INTERSECT SELECT 2, 2", sql);
    }

    @Test
    void selectIntersectAll() {
        String sql = select("1", "2").intersectAll(select("2", "2")).sql();
        assertEquals("SELECT 1, 2 INTERSECT ALL SELECT 2, 2", sql);
    }

    @Test
    void selectIntersectDistinct() {
        String sql = select("1", "2").intersectDistinct(select("2", "2")).sql();
        assertEquals("SELECT 1, 2 INTERSECT DISTINCT SELECT 2, 2", sql);
    }

    //---
    // EXCEPT
    //---

    @Test
    void selectExcept() {
        String sql = select("1", "2").except(select("2", "2")).sql();
        assertEquals("SELECT 1, 2 EXCEPT SELECT 2, 2", sql);
    }

    @Test
    void selectExceptAll() {
        String sql = select("1", "2").exceptAll(select("2", "2")).sql();
        assertEquals("SELECT 1, 2 EXCEPT ALL SELECT 2, 2", sql);
    }

    @Test
    void selectExceptDistinct() {
        String sql = select("1", "2").exceptDistinct(select("2", "2")).sql();
        assertEquals("SELECT 1, 2 EXCEPT DISTINCT SELECT 2, 2", sql);
    }


}