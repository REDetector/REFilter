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

package com.refilter.dataparser;

import com.refilter.database.DatabaseManager;
import com.refilter.database.TableCreator;
import com.refilter.utils.Indexer;
import com.refilter.utils.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;

/**
 * P_value based on alt and ref
 */
public class DARNEDParser {
    private static final Logger logger = LoggerFactory.getLogger(DARNEDParser.class);
    private DatabaseManager databaseManager;

    public DARNEDParser(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    private void createDARNEDTable(final String tableName) {
        try {
            if (!databaseManager.existTable(tableName)) {
                //"(chrom varchar(15),coordinate int,strand varchar(5),inchr varchar(5), inrna varchar(5) ,index(chrom,coordinate))");
                TableCreator.createReferenceTable(tableName, new String[]{"chrom", "coordinate", "strand", "inchr", "inrna"}, new String[]{
                        "varchar(30)",
                        "int", "varchar(5)", "varchar(5)", "varchar(5)"
                }, Indexer.CHROM_COORDINATE);
            }
        } catch (SQLException e) {
            logger.error("Error create DARNED table: " + tableName, e);
        }
    }

    public void loadDarnedTable(String darnedPath) {
        logger.info("Start loading DARNED file into database...\t" + Timer.getCurrentTime());
        String darnedTable = DatabaseManager.DARNED_DATABASE_TABLE_NAME;
        if (!databaseManager.hasEstablishTable(darnedTable)) {
            createDARNEDTable(darnedTable);
            try {
                int count = 0;
                databaseManager.setAutoCommit(false);
                FileInputStream inputStream = new FileInputStream(darnedPath);
                BufferedReader rin = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                // Skip the first row.
                rin.readLine();
                while ((line = rin.readLine()) != null) {
                    String[] sections = line.trim().split("\\t");
                    StringBuilder stringBuilder = new StringBuilder("insert into ");
                    stringBuilder.append(darnedTable);
                    stringBuilder.append("(chrom,coordinate,strand,inchr,inrna) values(");
                    for (int i = 0; i < 5; i++) {
                        if (i == 0) {
                            stringBuilder.append("'chr").append(sections[i]).append("',");
                        } else if (i == 1) {
                            stringBuilder.append(sections[i]).append(",");
                        } else {
                            stringBuilder.append("'").append(sections[i]).append("',");
                        }
                    }
                    stringBuilder.deleteCharAt(stringBuilder.length() - 1).append(")");

                    databaseManager.executeSQL(stringBuilder.toString());
                    if (++count % DatabaseManager.COMMIT_COUNTS_PER_ONCE == 0)
                        databaseManager.commit();
                }
                databaseManager.commit();
                databaseManager.setAutoCommit(true);
            } catch (IOException e) {
                logger.error("Error load file from " + darnedPath + " to file stream", e);
            } catch (SQLException e) {
                logger.error("Error execute sql clause in " + DARNEDParser.class.getName() + ":loadDarnedTable()", e);
                return;
            }
        }
        logger.info("End loading DARNED file into database...\t" + Timer.getCurrentTime());
    }
}
