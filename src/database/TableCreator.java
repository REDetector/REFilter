/*
 * REFilter: RNA Editing Filter
 *     Copyright (C) <2014>  <Xing Li>
 *
 *     REFilter is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     REFilter is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package database;


import utils.DatabasePreferences;
import utils.Indexer;

import java.sql.SQLException;

/**
 * Created by Administrator on 2014/11/13.
 */
public class TableCreator {
    private static DatabaseManager databaseManager = DatabaseManager.getInstance();

    public static void createFilterTable(String tableName) {
        String sqlClause = null;
        try {
            databaseManager.deleteTable(tableName);
            String tableBuilder = DatabasePreferences.getInstance().getDatabaseTableBuilder();
            sqlClause = "create table " + tableName + "(" + tableBuilder + "," + Indexer.CHROM_POSITION + ")";
            databaseManager.executeSQL(sqlClause);
        } catch (SQLException e) {
            System.err.println("There is a syntax error for SQL clause: " + sqlClause);
            e.printStackTrace();
        }
    }

    public static void createDARNEDTable(final String tableName) {
        try {
            if (!databaseManager.existTable(tableName)) {
                //"(chrom varchar(15),coordinate int,strand varchar(5),inchr varchar(5), inrna varchar(5) ,index(chrom,coordinate))");
                createReferenceTable(tableName, new String[]{"chrom", "coordinate", "strand", "inchr", "inrna"}, new String[]{
                        "varchar(30)",
                        "int", "varchar(5)", "varchar(5)", "varchar(5)"
                }, Indexer.CHROM_COORDINATE);
            }
        } catch (SQLException e) {
            System.err.println("Error create DARNED table");
            e.printStackTrace();
        }
    }

    public static void createDBSNPTable(final String tableName) {
        try {
            if (!databaseManager.existTable(tableName)) {
                //chrom varchar(15),pos int,index(chrom,pos);
                createReferenceTable(tableName, new String[]{"chrom", "pos"}, new String[]{"varchar(30)", "int"}, Indexer.CHROM_POSITION);
            }
        } catch (SQLException e) {
            System.err.println("Error create DBSNP table");
            e.printStackTrace();
        }
    }

    public static void createRepeatRegionsTable(final String tableName) {
        try {
            if (!databaseManager.existTable(tableName)) {
                //chrom varchar(30),begin int,end int,type varchar(40),index(chrom,begin,end);
                createReferenceTable(tableName, new String[]{"chrom", "begin", "end", "type"}, new String[]{"varchar(30)", "int", "int", "varchar(40)"},
                        Indexer.CHROM_BEGIN_END);
            }
        } catch (SQLException e) {
            System.err.println("Error create REPEAT REGIONS table");
            e.printStackTrace();
        }
    }

    public static void createSpliceJunctionTable(final String tableName) {
        try {
            if (!databaseManager.existTable(tableName)) {
                //   "(chrom varchar(15),ref varchar(30),type varchar(9),begin int,end int,unuse1 float(8,6),unuse2 varchar(5),unuse3 varchar(5),
                // info varchar(100),index(chrom,type))");
                createReferenceTable(tableName, new String[]{"chrom", "ref", "type", "begin", "end", "score", "strand", "frame", "info"},
                        new String[]{"varchar(30)", "varchar(30)", "varchar(10)", "int", "int", "float(8,6)", "varchar(1)", "varchar(1)", "varchar(100)"},
                        Indexer.CHROM_TYPE);
            }
        } catch (SQLException e) {
            System.err.println("Error create REPEAT REGIONS table");
            e.printStackTrace();
        }
    }

    public static void createFisherExactTestTable(String darnedResultTable) {
        databaseManager.deleteTable(darnedResultTable);
        String tableBuilder = DatabasePreferences.getInstance().getDatabaseTableBuilder();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("create table ").append(darnedResultTable).append("(").append(tableBuilder);
        stringBuilder.append(",");
        stringBuilder.append("level float, pvalue float, fdr float");
        stringBuilder.append(",");
        stringBuilder.append(Indexer.CHROM_POSITION);
        stringBuilder.append(")");
        try {
            databaseManager.executeSQL(stringBuilder.toString());
        } catch (SQLException e) {
            System.err.println("There is a syntax error for SQL clause: " + stringBuilder.toString());
            e.printStackTrace();
        }
    }

    private static void createReferenceTable(String tableName, String[] columnNames, String[] columnParams, String index) {
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
            System.err.println("There is a syntax error for SQL clause: " + stringBuilder.toString());
            e.printStackTrace();
        }
    }
}
