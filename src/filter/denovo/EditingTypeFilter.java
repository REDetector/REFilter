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
 * Created by Administrator on 2014/9/29.
 */
public class EditingTypeFilter {
    private DatabaseManager databaseManager;

    public EditingTypeFilter(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void executeEditingTypeFilter(String specificTable, String rnaVcfTable, String ref, String alt) {
        System.out.println("Start executing Editing Type Filter..." + Timer.getCurrentTime());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("insert into ");
        stringBuilder.append(specificTable);
        stringBuilder.append(" select * from ");
        stringBuilder.append(rnaVcfTable);
        stringBuilder.append(" WHERE REF='");
        stringBuilder.append(ref);
        stringBuilder.append("' AND ALT='");
        stringBuilder.append(alt);
        stringBuilder.append("' AND GT!='0/0'");
        try {
            databaseManager.insertClause(stringBuilder.toString());
        } catch (SQLException e) {
            System.err.println("There is a syntax error for SQL clause: " + stringBuilder.toString());
            e.printStackTrace();
        }
        System.out.println("End executing Editing Type Filter..." + Timer.getCurrentTime());
    }

}
