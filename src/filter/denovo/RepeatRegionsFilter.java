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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;

/**
 * we will filter out base in repeated area except for SINE/alu
 */


public class RepeatRegionsFilter {
    private DatabaseManager databaseManager;

    public RepeatRegionsFilter(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public boolean hasEstablishedRepeatTable(String repeatTable) {
        try {
            return databaseManager.calRowCount(repeatTable) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void loadRepeatTable(String repeatTable, String repeatPath) {
        System.out.println("Start loading RepeatTable..." + Timer.getCurrentTime());
        BufferedReader rin = null;
        try {
            if (!hasEstablishedRepeatTable(repeatTable)) {
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
            // TODO Auto-generated catch block
            System.err.println("Error load file from " + repeatPath + " to file stream");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Error execute sql clause in " + RepeatRegionsFilter.class.getName() + ":loadRepeatTable()");
            e.printStackTrace();
        } finally {
            if (rin != null) {
                try {
                    rin.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("End loading RepeatTable..." + Timer.getCurrentTime());
    }

    public void executeRepeatFilter(String repeatTable, String repeatResultTable, String aluResultTable, String refTable) {
        System.out.println("Start executing RepeatRegionsFilter..." + Timer.getCurrentTime());
        try {
            databaseManager.executeSQL("insert into " + repeatResultTable + " select * from " + refTable + " where not exists (select * from " +
                    repeatTable + " where (" + repeatTable + ".chrom= " + refTable + ".chrom and  " + repeatTable + ".begin<=" + refTable + ".pos and " +
                    repeatTable + ".end>=" + refTable + ".pos)) ");

            System.out.println("Start executing AluFilter..." + Timer.getCurrentTime());

            databaseManager.executeSQL("insert into " + aluResultTable + " SELECT * from " + refTable + " where exists (select chrom from " + repeatTable
                    + " where " + repeatTable + ".chrom = " + refTable + ".chrom and " + repeatTable + ".begin<=" + refTable + ".pos and " + repeatTable
                    + ".end>=" + refTable + ".pos and " + repeatTable + ".type='SINE/Alu')");

            databaseManager.executeSQL("update " + aluResultTable + " set alu = 'T'");
            databaseManager.executeSQL("insert into " + repeatResultTable + " select * from " + aluResultTable);

            System.out.println("End executing AluFilter..." + Timer.getCurrentTime());

        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("End executing RepeatRegionsFilter..." + Timer.getCurrentTime());
    }

}
