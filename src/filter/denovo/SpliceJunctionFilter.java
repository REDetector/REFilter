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

package filter.denovo;

import database.DatabaseManager;
import utils.Timer;

import java.sql.SQLException;

/**
 * Comphrehensive phase we focus on base in exon we discard base in the rear or front of the sequence
 */


public class SpliceJunctionFilter {
    private DatabaseManager databaseManager;

    public SpliceJunctionFilter(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public boolean hasEstablishedSpliceJunctionTable(String spliceJunctionTable) {
        try {
            return databaseManager.calRowCount(spliceJunctionTable) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void loadSpliceJunctionTable(String spliceJunctionTable, String spliceJunctionPath) {
        System.out.println("Start loading SpliceJunctionTable..." + Timer.getCurrentTime());
        if (!hasEstablishedSpliceJunctionTable(spliceJunctionTable)) {
            try {
                databaseManager.executeSQL("load data local infile '" + spliceJunctionPath + "' into table " + spliceJunctionTable + " fields terminated" +
                        " by '\t' lines terminated by '\n'");
            } catch (SQLException e) {
                System.err.println("Error execute sql clause in " + SpliceJunctionFilter.class.getName() + ":loadSpliceJunctionTable()..");
                e.printStackTrace();
            }
        }

        System.out.println("End loading SpliceJunctionTable..." + Timer.getCurrentTime());
    }

    public void executeSpliceJunctionFilter(String spliceJunctionTable, String spliceJunctionResultTable, String refTable, int edge) {
        System.out.println("Start executing SpliceJunctionFilter..." + Timer.getCurrentTime());
        try {
            databaseManager.executeSQL("insert into " + spliceJunctionResultTable + " select * from " + refTable + " where not exists (select chrom from "
                    + spliceJunctionTable + " where (" + spliceJunctionTable + ".type='CDS' and " + spliceJunctionTable + ".chrom=" + refTable + ".chrom" +
                    " and ((" + spliceJunctionTable + ".begin<" + refTable + ".pos+" + edge + " and " + spliceJunctionTable + ".begin>" + refTable + "" +
                    ".pos-" + edge + ") or (" + spliceJunctionTable + ".end<" + refTable + ".pos+" + edge + " and " + spliceJunctionTable + ".end>"
                    + refTable + ".pos-" + edge + "))))");
        } catch (SQLException e) {
            System.err.println("Error execute sql clause in" + SpliceJunctionFilter.class.getName() + ":executeSpliceJunctionFilter()");
            e.printStackTrace();
        }
        System.out.println("End executing SpliceJunctionFilter..." + Timer.getCurrentTime());
    }

}
