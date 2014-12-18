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

/**
 * we will filter out base which already be recognized
 */

import database.DatabaseManager;
import utils.Timer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;

public class KnownSNPFilter {
    private DatabaseManager databaseManager;
    private int count = 0;

    public KnownSNPFilter(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public boolean hasEstablishDbSNPTable(String dbSnpTable) {
        try {
            return databaseManager.calRowCount(dbSnpTable) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void loadDbSNPTable(String dbSNPTable, String dbSNPPath) {
        System.out.println("Start loading dbSNPTable" + " " + Timer.getCurrentTime());
        try {
            if (!hasEstablishDbSNPTable(dbSNPTable)) {
                FileInputStream inputStream = new FileInputStream(dbSNPPath);
                BufferedReader rin = new BufferedReader(new InputStreamReader(
                        inputStream));
                String line;
                while ((line = rin.readLine()) != null) {
                    if (line.startsWith("#")) {
                        count++;
                    } else {
                        break;
                    }
                }
                rin.close();
                databaseManager.executeSQL("load data local infile '" + dbSNPPath + "' into table " + dbSNPTable + "" +
                        " fields terminated by '\t' lines terminated by '\n' IGNORE " + count + " LINES");
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            System.err.println("Error load file from " + dbSNPPath + " to file stream");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Error execute sql clause in " + KnownSNPFilter.class.getName() + ":loadDbSNPTable()");
            e.printStackTrace();
        }
        System.out.println("End loading dbSNPTable" + " " + Timer.getCurrentTime());
    }

    public void executeDbSNPFilter(String dbSnpTable, String dbSnpResultTable, String refTable) {
        System.out.println("Start executing KnownSNPFilter..." + Timer.getCurrentTime());
        try {
            databaseManager.executeSQL("insert into " + dbSnpResultTable + " select * from " + refTable + " where " +
                    "not exists (select chrom from " + dbSnpTable + " where (" + dbSnpTable + ".chrom=" + refTable +
                    ".chrom and " + dbSnpTable + ".pos=" + refTable + ".pos))");
        } catch (SQLException e) {
            System.err.println("Error execute sql clause in" + KnownSNPFilter.class.getName() + ":executeDbSNPFilter()");
            e.printStackTrace();
        }
        System.out.println("End executing KnownSNPFilter..." + Timer.getCurrentTime());
    }

}
