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
 * we will filter out base in repeated area except for SINE/alu
 */
public class RepeatMaskerParser {
    private static final Logger logger = LoggerFactory.getLogger(RepeatMaskerParser.class);
    private DatabaseManager databaseManager;

    public RepeatMaskerParser(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    private void createRepeatRegionsTable(final String tableName) {
        try {
            if (!databaseManager.existTable(tableName)) {
                //chrom varchar(30),begin int,end int,type varchar(40),index(chrom,begin,end);
                TableCreator.createReferenceTable(tableName, new String[]{"chrom", "begin", "end", "type"},
                        new String[]{"varchar(30)", "int", "int", "varchar(40)"}, Indexer.CHROM_BEGIN_END);
            }
        } catch (SQLException e) {
            logger.error("Error create repeat regions table", e);
            e.printStackTrace();
        }
    }

    public void loadRepeatTable(String repeatPath) {
        logger.info("Start loading RepeatMasker file into database...\t" + Timer.getCurrentTime());
        BufferedReader rin = null;
        String repeatTable = DatabaseManager.REPEAT_MASKER_TABLE_NAME;
        try {
            if (!databaseManager.hasEstablishTable(repeatTable)) {
                createRepeatRegionsTable(repeatTable);
                databaseManager.setAutoCommit(false);
                int count = 0;
                FileInputStream inputStream = new FileInputStream(repeatPath);
                rin = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                rin.readLine();
                rin.readLine();
                rin.readLine();
                while ((line = rin.readLine()) != null) {
                    String section[] = line.trim().split("\\s+");
                    databaseManager.executeSQL("insert into " + repeatTable + "(chrom,begin,end,type) values('" +
                            section[4] + "','" + section[5] + "','" + section[6] + "','" + section[10] + "')");
                    if (++count % DatabaseManager.COMMIT_COUNTS_PER_ONCE == 0)
                        databaseManager.commit();
                }
                databaseManager.commit();
                databaseManager.setAutoCommit(true);
            }

        } catch (IOException e) {
            logger.error("Error load file from " + repeatPath + " to file stream", e);
        } catch (SQLException e) {
            logger.error("Error execute sql clause in " + RepeatMaskerParser.class.getName() + ":loadRepeatTable()", e);
            return;
        } finally {
            if (rin != null) {
                try {
                    rin.close();
                } catch (IOException e) {
                    logger.error("", e);
                }
            }
        }
        logger.info("End loading RepeatMasker file into database...\t" + Timer.getCurrentTime());
    }
}
