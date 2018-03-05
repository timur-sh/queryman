/*
 *  Queryman. Java tools for working with queries of PostgreSQL database.
 *
 *  License: MIT License
 *  To see license follow by http://queryman.org/license.txt
 */
package org.queryman.builder.token.expression.prepared;

import org.queryman.builder.token.PreparedExpression;

/**
 * This class is representation of float constant.
 *
 * @author Timur Shaidullin
 */
public class FloatExpression extends PreparedExpression<Float> {
    public FloatExpression(Float constant) {
        super(constant);
    }

    @Override
    protected String prepareName() {
        return name;
    }

    @Override
    protected Float getValue() {
        return value;
    }
}