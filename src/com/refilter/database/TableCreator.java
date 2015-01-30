/*
 * REFilters: RNA Editing Filters
 *     Copyright (C) <2014>  <Xing Li>
 *
 *     RED is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     RED is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.refilter.database;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Created by Administrator on 2014/11/13.
 */
public class TableCreator {
    private static final Logger logger = LoggerFactory.getLogger(TableCreator.class);
    private static DatabaseManager databaseManager = DatabaseManager.getInstance();

    public static void createFilterTable(String refTable, String tableName) {
        String sqlClause = null;
        try {
            databaseManager.deleteTable(tableName);
            sqlClause = "create table " + tableName + " like " + refTable;
            databaseManager.executeSQL(sqlClause);
        } catch (SQLException e) {
            logger.error("There is a syntax error for SQL clause: " + sqlClause, e);
        }
    }

    public static void createFisherExactTestTable(String refTable, String darnedResultTable) {
        databaseManager.deleteTable(darnedResultTable);
        createFilterTable(refTable, darnedResultTable);
        try {
            databaseManager.executeSQL("alter table " + darnedResultTable + " add level float,add pvalue float,add fdr float;");
        } catch (SQLException e) {
            logger.error("Can not create Fisher Exact Test Table.", e);
        }
    }

    public static void createReferenceTable(String tableName, String[] columnNames, String[] columnParams, String index) {
        if (columnNames == null || columnParams == null || columnNames.length == 0 || columnNames.length != columnParams.length) {
            throw new IllegalArgumentException("Column names and column parameters can't not be null or zero-length.");
        }
        // Create table if not exists TableName(abc int, def varchar(2), hij text);
        StringBuilder stringBuilder = new StringBuilder("create table if not exists " + tableName + "(");
        stringBuilder.append(columnNames[0]).append(" ").append(columnParams[0]);
        for (int i = 1, len = columnNames.length; i < len; i++) {
            stringBuilder.append(", ").append(columnNames[i]).append(" ").append(columnParams[i]);
        }
        stringBuilder.append(",");
        stringBuilder.append(index);
        stringBuilder.append(")");
        try {
            databaseManager.executeSQL(stringBuilder.toString());
        } catch (SQLException e) {
            logger.error("There is a syntax error for SQL clause: " + stringBuilder.toString(), e);
        }
    }
}
