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

package com.refilter.filter.dnarna;

import com.refilter.database.DatabaseManager;
import com.refilter.database.TableCreator;
import com.refilter.filter.Filter;
import com.refilter.utils.NegativeType;
import com.refilter.utils.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Arrays;

/**
 * Detect SNP in DNA level
 */

public class DNARNAFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(DNARNAFilter.class);
    private DatabaseManager databaseManager;

    public DNARNAFilter(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void performFilter(String previousTable, String currentTable, String[] args) {
        if (args == null || args.length == 0) {
            return;
        } else if (args.length != 2) {
            throw new IllegalArgumentException("Args " + Arrays.asList(args) + " for DNA-RNA Filter are incomplete, please have a check");
        }
        TableCreator.createFilterTable(previousTable, currentTable);
        logger.info("Start performing DNA-RNA Filter...\t" + Timer.getCurrentTime());
        String dnaVcfTable = args[0];
        String editingType = args[1];
        String negativeType = NegativeType.getNegativeStrandEditingType(editingType);
        try {
            databaseManager.executeSQL("insert into " + currentTable + " select * from " + previousTable + " where exists (select chrom from " + dnaVcfTable
                    + " where (" + dnaVcfTable + ".chrom=" + previousTable + ".chrom and " + dnaVcfTable + ".pos=" + previousTable + ".pos and (" +
                    dnaVcfTable + ".ref='" + editingType.charAt(0) + "' or  " + dnaVcfTable + ".ref='" + negativeType.charAt(0) + "')))");
        } catch (SQLException e) {
            logger.error("Error execute sql clause in " + DNARNAFilter.class.getName() + ":performFilter()", e);
        }
        logger.info("End performing DNA-RNA Filter...\t" + Timer.getCurrentTime());
    }

    @Override
    public String getName() {
        return DatabaseManager.DNA_RNA_FILTER_RESULT_TABLE_NAME;
    }
}
