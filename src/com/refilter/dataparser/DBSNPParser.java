/*
 * REFilters: RNA Editing Filters Copyright (C) <2014> <Xing Li>
 * 
 * RED is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * RED is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.refilter.dataparser;

/**
 * we will filter out base which already be recognized
 */

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.refilter.database.DatabaseManager;
import com.refilter.database.TableCreator;
import com.refilter.utils.Indexer;
import com.refilter.utils.Timer;

public class DBSNPParser {
    private static final Logger logger = LoggerFactory.getLogger(DBSNPParser.class);
    private DatabaseManager databaseManager;

    public DBSNPParser(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    private void createDBSNPTable(final String tableName) {
        try {
            if (!databaseManager.existTable(tableName)) {
                // chrom varchar(15),pos int,index(chrom,pos);
                TableCreator.createReferenceTable(tableName, new String[] { "chrom", "pos" }, new String[] {
                    "varchar(30)", "int" }, Indexer.CHROM_POSITION);
            }
        } catch (SQLException e) {
            System.err.println("Error create dbSNP table");
            e.printStackTrace();
        }
    }

    public void loadDbSNPTable(String dbSNPPath) {
        logger.info("Start loading dbSNP file into database...\t" + Timer.getCurrentTime());
        String dbSNPTable = DatabaseManager.DBSNP_DATABASE_TABLE_NAME;
        try {
            if (!databaseManager.hasEstablishTable(dbSNPTable)) {
                createDBSNPTable(dbSNPTable);
                int count = 0;
                FileInputStream inputStream = new FileInputStream(dbSNPPath);
                BufferedReader rin = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = rin.readLine()) != null) {
                    if (line.startsWith("#")) {
                        count++;
                    } else {
                        break;
                    }
                }
                rin.close();
                databaseManager.executeSQL("load data local infile '" + dbSNPPath + "' into table " + dbSNPTable + ""
                    + " fields terminated by '\t' lines terminated by '\n' IGNORE " + count + " LINES");
            }
        } catch (IOException e) {
            logger.error("Error load file from " + dbSNPPath + " to file stream", e);
        } catch (SQLException e) {
            logger.error("Error execute sql clause in " + DBSNPParser.class.getName() + ":loadDbSNPTable()", e);
            return;
        }
        logger.info("End loading dbSNP file into database...\t" + Timer.getCurrentTime());
    }
}
