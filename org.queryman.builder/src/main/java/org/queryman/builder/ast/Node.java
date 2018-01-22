/*
 *  Queryman. Java tools for working with queries of PostgreSQL database.
 *
 *  License: MIT License.
 *  To see license follow by http://queryman.org/license.txt
 */
package org.queryman.builder.ast;

/**
 * Represents node of tree.
 *
 * @author Timur Shaidullin
 */
final class Node {
    private final String name;

    Node(String name) {
        this.name = name;
    }
}
