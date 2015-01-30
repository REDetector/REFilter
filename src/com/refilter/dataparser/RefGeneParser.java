/*
 * REFilters: RNA Editing Filters
 *     Copyright (C) <2014>  <Xing Li>
 *
 *     RED is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     RED is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.refilter.dataparser;

import com.refilter.database.DatabaseManager;
import com.refilter.database.TableCreator;
import com.refilter.utils.Indexer;
import com.refilter.utils.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Comprehensive phase we focus on base in exon we discard base in the rear or front of the sequence
 */


public class RefGeneParser {
    private static final Logger logger = LoggerFactory.getLogger(RefGeneParser.class);
    private DatabaseManager databaseManager;

    public RefGeneParser(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    private void createRefSeqGeneTable(final String tableName) {
        try {
            if (!databaseManager.existTable(tableName)) {
                //   "(chrom varchar(15),ref varchar(30),type varchar(9),begin int,end int,unuse1 float(8,6),unuse2 varchar(5),unuse3 varchar(5),
                // info varchar(100),index(chrom,type))");
                TableCreator.createReferenceTable(tableName, new String[]{
                                "bin", "name", "chrom", "strand", "txStart", "txEnd", "cdsStart", "cdsEnd",
                                "exonCount", "exonStarts", "exonEnds", "score", "name2", "cdsStartStat", "cdsEndStat", "exonFrames"
                        },
                        new String[]{
                                "int", "varchar(255)", "varchar(255)", "varchar(1)", "int", "int", "int", "int", "int", "longblob", "longblob", "int",
                                "varchar(255)", "varchar(8)", "varchar(8)", "longblob"
                        },
                        Indexer.CHROM_START_END);
            }
        } catch (SQLException e) {
            logger.error("Error create Splice Junction Table");
            e.printStackTrace();
        }
    }

    public void loadRefSeqGeneTable(String refSeqGenePath) {
        logger.info("Start loading Ref Seq Gene File into database...\t" + Timer.getCurrentTime());
        String refseqGeneTableName = DatabaseManager.REFSEQ_GENE_TABLE_NAME;
        if (!databaseManager.hasEstablishTable(refseqGeneTableName)) {
            createRefSeqGeneTable(refseqGeneTableName);
            try {
                databaseManager.executeSQL("load data local infile '" + refSeqGenePath + "' into table " + refseqGeneTableName + " fields terminated" +
                        " by '\t' lines terminated by '\n'");
            } catch (SQLException e) {
                logger.error("Error execute sql clause in " + RefGeneParser.class.getName() + ":loadRefSeqGeneTable().", e);
            }
        }
        logger.info("End loading Ref Seq Gene File into database...\t" + Timer.getCurrentTime());
    }

}
