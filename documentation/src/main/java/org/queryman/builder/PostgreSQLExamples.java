/*
 *  Queryman. Java tools for working with queries of PostgreSQL database.
 *
 *  License: MIT License
 *  To see license follow by http://queryman.org/license.txt
 */
package org.queryman.builder;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;

import static org.queryman.builder.PostgreSQL.asConstant;
import static org.queryman.builder.PostgreSQL.asDollarString;
import static org.queryman.builder.PostgreSQL.asFunc;
import static org.queryman.builder.PostgreSQL.asName;
import static org.queryman.builder.PostgreSQL.asList;
import static org.queryman.builder.PostgreSQL.asOperator;
import static org.queryman.builder.PostgreSQL.asSubQuery;
import static org.queryman.builder.PostgreSQL.asTime;
import static org.queryman.builder.PostgreSQL.insertInto;
import static org.queryman.builder.PostgreSQL.select;

/**
 * @author Timur Shaidullin
 */
public class PostgreSQLExamples {
    public void common() {
        //tag::alias[]
        // book.id AS b_id
        PostgreSQL.asName("book.id").as("b_id");
        //end::alias[]

        //tag::select-alias[]
        // SELECT b.id FROM book AS b
        PostgreSQL.select("b.id")
           .from(asName("book").as("b"));
        //end::select-alias[]

        //tag::cast[]
        // ARRAY[1, 2, 3]::integer[] AS arr
        PostgreSQL.asArray(1, 2, 3).cast("integer[]").as("arr");
        //end::cast[]


        long ms = System.currentTimeMillis();
        /*
        //tag::prepared[]
        // SELECT * FROM book WHERE time = ?::time
        PostgreSQL.select("*")
           .from("schedule")
           .where("time", "=", asTime(new Time(ms)).cast("time"))
        //end::prepared[]
        */
    }

    public void constants() {
        //tag::constant[]
        // 20
        PostgreSQL.asConstant(20);
        // 'a string'
        PostgreSQL.asConstant("a string");
        // $$a dollar string$$
        PostgreSQL.asDollarString("a dollar string");
        // $tag$a dollar string$tag$
        PostgreSQL.asDollarString("a dollar string", "tag");
        //end::constant[]
    }

    public void columnReference() {
        //tag::column-reference[]
        // book
        PostgreSQL.asName("book");
        // book.id
        PostgreSQL.asName("book.id");
        // "book"
        PostgreSQL.asQuotedName("book");
        // "book"."id"
        PostgreSQL.asQuotedName("book.id");
        //end::column-reference[]
    }

    public void prepares() {
        //tag::insert-prepared[]
        // SELECT INTO book (name) VALUES (?) RETURNING *
        insertInto("book")
           .columns(
              "name"
           )
           .values(
              asConstant("Queryman builder")
           )
           .returning("*");
        //end::insert-prepared[]
    }

    public void array() {
        //tag::array[]
        // ARRAY[1, 2, 3]
        PostgreSQL.asArray(1, 2, 3);
        // ARRAY[1, 2, 3]
        PostgreSQL.asArray(List.of(1, 2, 3));
        //end::array[]
    }

    public void list() {
        //tag::list[]
        // (1, 2, 3)
        PostgreSQL.asList(1, 2, 3, asConstant(4));
        // (1, 2, 3)
        PostgreSQL.asList(List.of(1, 2, 3));
        //end::list[]
    }

    public void func() {
        //tag::func[]
        // concat('price', 1, 2)
        PostgreSQL.asFunc("concat", PostgreSQL.asList("'price'", 2, ".", 1));

        // concat('price', 1, 2)
        PostgreSQL.asFunc("concat", "'price'", 2, ".", 1);

        // (VALUES(1, 2), (3, 4)) AS point(x, y)
        PostgreSQL.values(asList(1, 2), asList(3, 4)).as("point", "x", "y");
        //end::func[]
    }

    public void query() {
        //tag::query[]
        // (SELECT id, name FROM book)
        PostgreSQL.asSubQuery(PostgreSQL.select("id", "name").from("book"));
        //end::query[]

        //tag::select-query[]
        // SELECT (SELECT max(price) FROM book) AS max
        select(
           asSubQuery(select("max(price)").from("book")).as("max")
        );
        //end::select-query[]

        //tag::exists-query[]
        // SELECT EXISTS(SELECT * FROM book) AS exists
        select(
           asOperator("EXISTS", asSubQuery(select("*").from("book"))).as("exists")
        );
        //end::exists-query[]
    }
}
