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

package com.refilter.database;

/**
 * Linked to target database
 */

import com.refilter.utils.DatabasePreferences;
import com.refilter.utils.RandomStringGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    public static final int COMMIT_COUNTS_PER_ONCE = 10000;
    public static final String FILTER = "com/refilter/filter";
    public static final String DNA_RNA_MODE_DATABASE_NAME = "DNA_RNA_MODE";
    public static final String DENOVO_MODE_DATABASE_NAME = "DENOVO_MODE";
    public static final String RNA_VCF_RESULT_TABLE_NAME = "rnavcf";
    public static final String DNA_VCF_RESULT_TABLE_NAME = "dnavcf";
    public static final String QC_FILTER_RESULT_TABLE_NAME = "qcfilter";
    public static final String EDITING_TYPE_FILTER_RESULT_TABLE_NAME = "etfilter";
    public static final String SPLICE_JUNCTION_FILTER_RESULT_TABLE_NAME = "sjfilter";
    public static final String DBSNP_FILTER_RESULT_TABLE_NAME = "dbfilter";
    public static final String FET_FILTER_RESULT_TABLE_NAME = "fetfilter";
    public static final String REPEAT_FILTER_RESULT_TABLE_NAME = "rrfilter";
    public static final String DNA_RNA_FILTER_RESULT_TABLE_NAME = "drfilter";
    public static final String LLR_FILTER_RESULT_TABLE_NAME = "llrfilter";
    public static final String SPLICE_JUNCTION_TABLE_NAME = "splice_junction";
    public static final String DARNED_DATABASE_TABLE_NAME = "darned_database";
    public static final String REPEAT_MASKER_TABLE_NAME = "repeat_masker";
    public static final String DBSNP_DATABASE_TABLE_NAME = "dbsnp_database";
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final DatabaseManager DATABASE_MANAGER = new DatabaseManager();
    private static Statement stmt;
    private Connection con = null;

    private DatabaseManager() {
    }

    public static DatabaseManager getInstance() {
        return DATABASE_MANAGER;
    }

    public boolean connectDatabase(String host, String port, String user, String password) {

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("The JDBC driver has not been found.", e);
        }

        String connectionURL = "jdbc:mysql://" + host + ":" + port;
        try {
            con = DriverManager.getConnection(connectionURL, user, password);
        } catch (SQLException e) {
            logger.error("Database connection error.", e);
        }
        return con != null;
    }

    public void setAutoCommit(boolean autoCommit) {
        try {
            con.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            logger.warn("Can not set commit automatically");
        }
    }

    public void commit() {
        try {
            con.commit();
        } catch (SQLException e) {
            logger.error("Error commit to database", e);
        }
    }

    public int calRowCount(String tableName) throws SQLException {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("select count(1) from " + tableName);
        if (rs != null && rs.next()) {
            return rs.getInt(1);
        } else {
            return 0;
        }
    }

    public boolean hasEstablishTable(String darnedTable) {
        try {
            return calRowCount(darnedTable) > 0;
        } catch (SQLException e) {
            logger.warn("", e);
            return false;
        }
    }

    public void createDatabase(String databaseName) {
        try {
            Statement stmt = con.createStatement();
            stmt.executeUpdate("create database if not exists " + databaseName);
            stmt.close();
        } catch (SQLException e) {
            logger.error("Error create database: " + databaseName, e);
        }
    }

    public List<String> getColumnNames(String database, String tableName) throws SQLException {
        List<String> columnNames = new ArrayList<String>();
        useDatabase(database);
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("select COLUMN_NAME from information_schema.columns where table_name='" + tableName + "'");
        while (rs.next()) {
            columnNames.add(rs.getString(1));
        }
        return columnNames;
    }

    public List<String> getCurrentTables(String database) throws SQLException {
        List<String> tableLists = new ArrayList<String>();
        useDatabase(database);
        DatabaseMetaData databaseMetaData = con.getMetaData();
        ResultSet rs = databaseMetaData.getTables(database, null, null, new String[]{"TABLE"});
        while (rs.next()) {
            // get table name
            tableLists.add(rs.getString(3));
        }
        return tableLists;
    }

    public void deleteTable(String tableName) {
        try {
            Statement stmt = con.createStatement();
            stmt.executeUpdate("drop table if exists " + tableName);
            stmt.close();
        } catch (SQLException e) {
            logger.error("Error delete table: " + tableName, e);
        }
    }

    /**
     * BJ22_qcfilter_etfilter
     *
     * @param sampleName
     */
    public void deleteTableAndFilters(String database, String sampleName) {
        try {
            List<String> tableLists = getCurrentTables(database);
            // Prevent from deleting BJ22N sample, but actually we want to delete BJ22 sample.
            Statement stmt = con.createStatement();
            for (String table : tableLists) {
                if (table.startsWith(sampleName + "_")) {
                    stmt.executeUpdate("drop table if exists " + table);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            logger.error("Error delete sample '" + sampleName + "' in database '" + database + "'", e);
        }
    }

    /**
     * We get the name of a table like "BJ22" from "BJ22_qcfilter_etfilter"
     *
     * @param tableName
     * @return
     */
    public String getSampleName(String tableName) {
        if (tableName == null) {
            return null;
        }
        String[] sections = tableName.split("_");
        if (sections.length == 1) {
            return tableName;
        } else if (sections.length == 2) {
            return tableName.substring(0, tableName.indexOf("_"));
        } else {
            StringBuilder builder = new StringBuilder();
            for (String section : sections) {
                if (section.contains(DatabaseManager.RNA_VCF_RESULT_TABLE_NAME) || section.contains(FILTER)) {
                    break;
                } else {
                    builder.append(section).append("_");
                }
            }
            return builder.substring(0, builder.length() - 1);
        }
    }

    public void useDatabase(String databaseName) {
        try {
            Statement stmt = con.createStatement();
            stmt.executeUpdate("use " + databaseName);
            stmt.close();
        } catch (SQLException e) {
            logger.warn("Can not use the database: " + databaseName);
        }
    }

    public void insertClause(String sql) throws SQLException {
        if (stmt == null || stmt.isClosed()) {
            stmt = con.createStatement();
        }
        stmt.executeUpdate(sql);
    }

    public void executeSQL(String sql) throws SQLException {
        Statement stmt = con.createStatement();
        stmt.executeUpdate(sql);
        stmt.close();
    }

    public ResultSet query(String queryClause) {
        ResultSet rs = null;
        try {
            Statement stmt = con.createStatement();
            rs = stmt.executeQuery(queryClause);
        } catch (SQLException e) {
            logger.error("Can not get query results...", e);
        }
        return rs;
    }

    public ResultSet query(String table, String[] columns, String selection, String[] selectionArgs) {
        ResultSet rs = null;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("select ");
        if (columns == null || columns.length == 0 || columns[0].equals("*")) {
            stringBuilder.append(" * ");
        } else {
            stringBuilder.append(columns[0]);
            for (int i = 1, len = columns.length; i < len; i++) {
                stringBuilder.append(",").append(columns[i]);
            }
        }
        stringBuilder.append(" from ").append(table);
        try {
            if (selection == null || selectionArgs == null || selectionArgs.length == 0) {
                Statement stmt = con.createStatement();
                rs = stmt.executeQuery(stringBuilder.toString());
            } else {
                stringBuilder.append(" WHERE ").append(selection);
                PreparedStatement statement = con.prepareStatement(stringBuilder.toString());
                for (int i = 1, len = selectionArgs.length; i <= len; i++) {
                    statement.setString(i, selectionArgs[i - 1]);
                }
                rs = statement.executeQuery();
            }
        } catch (SQLException e) {
            logger.error("There is a syntax error: " + stringBuilder.toString(), e);
        }
        return rs;
    }

    public void closeDatabase() {
        try {
            if (con != null && !con.isClosed()) {
                con.close();
            }
        } catch (SQLException e) {
            logger.error("Error close database!", e);
        }
    }

    public boolean existTable(String tableName) throws SQLException {
        List<String> tableLists = getCurrentTables(DatabasePreferences.getInstance().getCurrentDatabase());
        return tableLists.contains(tableName);
    }

    public void distinctTable(String resultTable) {
        try {
            String tempTable = RandomStringGenerator.getRandomString(10);
            executeSQL("create temporary table " + tempTable + " select distinct * from " + resultTable);
            executeSQL("truncate table " + resultTable);
            executeSQL("insert into " + resultTable + " select * from " + tempTable);
            deleteTable(tempTable);
        } catch (SQLException e) {
            logger.error("Error execute sql clause in " + DatabaseManager.class.getName() + ":distinctTable()", e);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        closeDatabase();
    }
}
