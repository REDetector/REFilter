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

package com.refilter.filter.denovo;

import com.refilter.database.DatabaseManager;
import com.refilter.database.TableCreator;
import com.refilter.filter.Filter;
import com.refilter.utils.RandomStringGenerator;
import com.refilter.utils.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * we will filter out base in repeated area except for SINE/alu
 */


public class RepeatRegionsFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(RepeatRegionsFilter.class);
    private DatabaseManager databaseManager;

    public RepeatRegionsFilter(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void performFilter(String previousTable, String currentTable, String[] args) {
        TableCreator.createFilterTable(previousTable, currentTable);
        logger.info("Start performing Repeat Regions Filter...\t" + Timer.getCurrentTime());
        String repeatTable = DatabaseManager.REPEAT_MASKER_TABLE_NAME;
        try {
            databaseManager.executeSQL("insert into " + currentTable + " select * from " + previousTable + " where not exists (select * from " +
                    repeatTable + " where (" + repeatTable + ".chrom= " + previousTable + ".chrom and  " + repeatTable + ".begin<=" + previousTable + ".pos and " +
                    repeatTable + ".end>=" + previousTable + ".pos)) ");

            logger.info("Start finding sites in Alu Regions...\t" + Timer.getCurrentTime());
            String tempTable = RandomStringGenerator.getRandomString(10);
            databaseManager.executeSQL("create temporary table " + tempTable + " like " + currentTable);
            databaseManager.executeSQL("insert into " + tempTable + " SELECT * from " + previousTable + " where exists (select chrom from " + repeatTable
                    + " where " + repeatTable + ".chrom = " + previousTable + ".chrom and " + repeatTable + ".begin<=" + previousTable + ".pos and " + repeatTable
                    + ".end>=" + previousTable + ".pos and " + repeatTable + ".type='SINE/Alu')");
            databaseManager.executeSQL("update " + tempTable + " set alu = 'T'");
            databaseManager.executeSQL("insert into " + currentTable + " select * from " + tempTable);
            databaseManager.deleteTable(tempTable);
            logger.info("End finding sites in Alu Regions...\t" + Timer.getCurrentTime());

        } catch (SQLException e) {
            logger.error("Error execute sql clause in " + RepeatRegionsFilter.class.getName() + ":performFilter()", e);
        }
        logger.info("End performing Repeat Regions Filter...\t" + Timer.getCurrentTime());
    }

    @Override
    public String getName() {
        return DatabaseManager.REPEAT_FILTER_RESULT_TABLE_NAME;
    }
}
