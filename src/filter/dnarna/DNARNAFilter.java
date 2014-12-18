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

package filter.dnarna;

import database.DatabaseManager;
import utils.Timer;

import java.sql.SQLException;

/**
 * Detect SNP in DNA level
 */

public class DNARNAFilter {
    private DatabaseManager databaseManager;

    public DNARNAFilter(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void executeDnaRnaFilter(String dnaRnaResultTable, String dnaVcfTable, String refTable) {
        System.out.println("Start executing DNARNAFilter..." + Timer.getCurrentTime());

        try {
            databaseManager.executeSQL("insert into " + dnaRnaResultTable + " select * from " + refTable + " where " +
                    "exists (select chrom from " + dnaVcfTable + " where (" + dnaVcfTable + ".chrom=" + refTable +
                    ".chrom and " + dnaVcfTable + ".pos=" + refTable + ".pos))");
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            System.err.println("Error execute sql clause in " + DNARNAFilter.class.getName() + ":executeDnaRnaFilter()");
            e.printStackTrace();
        }
        System.out.println("End executing DNARNAFilter..." + Timer.getCurrentTime());
    }

}
