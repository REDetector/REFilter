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

package com.refilter.dataparser;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.refilter.database.DatabaseManager;
import com.refilter.database.TableCreator;
import com.refilter.utils.Indexer;
import com.refilter.utils.Timer;

/**
 * Comphrehensive phase we focus on base in exon we discard base in the rear or front of the sequence
 */

public class GTFParser {
    private static final Logger logger = LoggerFactory.getLogger(GTFParser.class);
    private DatabaseManager databaseManager;

    public GTFParser(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    private void createSpliceJunctionTable(final String tableName) {
        try {
            if (!databaseManager.existTable(tableName)) {
                // "(chrom varchar(15),ref varchar(30),type varchar(9),begin int,end int,unuse1 float(8,6),unuse2
                // varchar(5),unuse3 varchar(5),
                // info varchar(100),index(chrom,type))");
                TableCreator.createReferenceTable(tableName,
                    new String[] { "chrom", "ref", "type", "begin", "end", "score", "strand", "frame", "info" },
                    new String[] { "varchar(30)", "varchar(30)", "varchar(10)", "int", "int", "float(8,6)",
                        "varchar(1)", "varchar(1)", "varchar(100)" },
                    Indexer.CHROM_TYPE);
            }
        } catch (SQLException e) {
            logger.error("Error create Splice Junction Table");
            e.printStackTrace();
        }
    }

    public void loadSpliceJunctionTable(String spliceJunctionPath) {
        logger.info("Start loading Gene Annotation File into database...\t" + Timer.getCurrentTime());
        String spliceJunctionTable = DatabaseManager.SPLICE_JUNCTION_TABLE_NAME;
        if (!databaseManager.hasEstablishTable(spliceJunctionTable)) {
            createSpliceJunctionTable(spliceJunctionTable);
            try {
                databaseManager.executeSQL("load data local infile '" + spliceJunctionPath + "' into table "
                    + spliceJunctionTable + " fields terminated" + " by '\t' lines terminated by '\n'");
            } catch (SQLException e) {
                logger.error("Error execute sql clause in " + GTFParser.class.getName() + ":loadSpliceJunctionTable().",
                    e);
            }
        }
        logger.info("End loading Gene Annotation File into database...\t" + Timer.getCurrentTime());
    }

}
