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

package com.refilter.filter.denovo;

/**
 * we will filter out base which already be recognized
 */

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.refilter.database.DatabaseManager;
import com.refilter.database.TableCreator;
import com.refilter.filter.Filter;
import com.refilter.utils.Timer;

public class KnownSNPFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(KnownSNPFilter.class);
    private DatabaseManager databaseManager;

    public KnownSNPFilter(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void performFilter(String previousTable, String currentTable, String[] args) {
        logger.info("Start performing Known SNP Filter...\t" + Timer.getCurrentTime());
        TableCreator.createFilterTable(previousTable, currentTable);
        String dbSnpTable = DatabaseManager.DBSNP_DATABASE_TABLE_NAME;
        try {
            databaseManager.executeSQL("insert into " + currentTable + " select * from " + previousTable
                + " where not exists (select chrom from " + dbSnpTable + " where (" + dbSnpTable + ".chrom="
                + previousTable + ".chrom and " + dbSnpTable + ".pos=" + previousTable + ".pos))");
        } catch (SQLException e) {
            logger.error("Error execute sql clause in" + KnownSNPFilter.class.getName() + ":performFilter()", e);
        }
        logger.info("End performing Known SNP Filter...\t" + Timer.getCurrentTime());
    }

    @Override
    public String getName() {
        return DatabaseManager.DBSNP_FILTER_RESULT_TABLE_NAME;
    }
}
