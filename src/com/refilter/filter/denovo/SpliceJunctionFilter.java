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

import java.sql.SQLException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.refilter.database.DatabaseManager;
import com.refilter.database.TableCreator;
import com.refilter.filter.Filter;
import com.refilter.utils.Timer;

/**
 * Comphrehensive phase we focus on base in exon we discard base in the rear or front of the sequence
 */

public class SpliceJunctionFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(SpliceJunctionFilter.class);
    private DatabaseManager databaseManager;

    public SpliceJunctionFilter(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void performFilter(String previousTable, String currentTable, String[] args) {
        if (args == null || args.length == 0) {
            return;
        } else if (args.length != 1) {
            throw new IllegalArgumentException("Args " + Arrays.asList(args)
                + " for Splice Junction Filter are incomplete, please have a check");
        }
        TableCreator.createFilterTable(previousTable, currentTable);
        logger.info("Start performing Splice Junction Filter...\t" + Timer.getCurrentTime());
        String spliceJunctionTable = DatabaseManager.SPLICE_JUNCTION_TABLE_NAME;
        int edge = Integer.parseInt(args[0]);
        try {
            databaseManager.executeSQL("insert into " + currentTable + " select * from " + previousTable
                + " where not exists (select chrom from " + spliceJunctionTable + " where (" + spliceJunctionTable
                + ".type='CDS' and " + spliceJunctionTable + ".chrom=" + previousTable + ".chrom" + " and (("
                + spliceJunctionTable + ".begin<" + previousTable + ".pos+" + edge + " and " + spliceJunctionTable
                + ".begin>" + previousTable + "" + ".pos-" + edge + ") or (" + spliceJunctionTable + ".end<"
                + previousTable + ".pos+" + edge + " and " + spliceJunctionTable + ".end>" + previousTable + ".pos-"
                + edge + "))))");
        } catch (SQLException e) {
            logger.error("Error execute sql clause in" + SpliceJunctionFilter.class.getName() + ":performFilter()", e);
        }
        logger.info("End performing Splice Junction Filter...\t" + Timer.getCurrentTime());
    }

    @Override
    public String getName() {
        return DatabaseManager.SPLICE_JUNCTION_FILTER_RESULT_TABLE_NAME;
    }
}
