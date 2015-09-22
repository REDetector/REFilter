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
import com.refilter.utils.NegativeType;
import com.refilter.utils.Timer;

/**
 * Created by Administrator on 2014/9/29.
 */
public class EditingTypeFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(EditingTypeFilter.class);
    private DatabaseManager databaseManager;

    public EditingTypeFilter(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void performFilter(String previousTable, String currentTable, String[] args) {
        if (args == null || args.length == 0) {
            return;
        } else if (args.length != 1) {
            throw new IllegalArgumentException(
                "Args " + Arrays.asList(args) + " for Editing Type Filter are incomplete, please have a check");
        }
        TableCreator.createFilterTable(previousTable, currentTable);
        logger.info("Start executing Editing Type Filter..." + Timer.getCurrentTime());
        String refAlt = args[0];
        String refAlt2 = NegativeType.getNegativeStrandEditingType(refAlt);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("insert into ");
        stringBuilder.append(currentTable);
        stringBuilder.append(" select * from ");
        stringBuilder.append(previousTable);
        stringBuilder.append(" WHERE REF='");
        stringBuilder.append(refAlt.substring(0, 1));
        stringBuilder.append("' AND ALT='");
        stringBuilder.append(refAlt.substring(1));
        stringBuilder.append("' AND GT!='0/0'");
        try {
            databaseManager.insertClause(stringBuilder.toString());
        } catch (SQLException e) {
            logger.error("There is a syntax error for SQL clause: " + stringBuilder.toString(), e);
        }

        stringBuilder = new StringBuilder();
        stringBuilder.append("insert into ");
        stringBuilder.append(currentTable);
        stringBuilder.append(" select * from ");
        stringBuilder.append(previousTable);
        stringBuilder.append(" WHERE REF='");
        stringBuilder.append(refAlt2.substring(0, 1));
        stringBuilder.append("' AND ALT='");
        stringBuilder.append(refAlt2.substring(1));
        stringBuilder.append("' AND GT!='0/0'");
        try {
            databaseManager.insertClause(stringBuilder.toString());
        } catch (SQLException e) {
            logger.error("There is a syntax error for SQL clause: " + stringBuilder.toString(), e);
        }
        logger.info("End executing Editing Type Filter..." + Timer.getCurrentTime());
    }

    @Override
    public String getName() {
        return DatabaseManager.EDITING_TYPE_FILTER_RESULT_TABLE_NAME;
    }
}
