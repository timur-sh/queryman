/*
 *  Queryman. Java tools for working with queries of PostgreSQL database.
 *
 *  License: MIT License
 *  To see license follow by http://queryman.org/license.txt
 */
package org.queryman.builder.token;

/**
 * PostgreSQL identifiers are names of tables, views, columns, and other
 * database objects. The identifiers may be as quoted an unquoted.
 * This {@code class} represents unquoted identifier.
 *
 * @author Timur Shaidullin
 */
public class Identifier extends AbstractToken {
    public Identifier(String name) {
        super(name);
    }

    @Override
    public String getName() {
        return name;
    }
}
