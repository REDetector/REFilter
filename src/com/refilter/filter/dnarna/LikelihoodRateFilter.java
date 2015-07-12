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

package com.refilter.filter.dnarna;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.refilter.database.DatabaseManager;
import com.refilter.database.TableCreator;
import com.refilter.datatypes.SiteBean;
import com.refilter.filter.Filter;
import com.refilter.utils.Timer;

/**
 * LLR used for detecting editing sites
 */

public class LikelihoodRateFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(LikelihoodRateFilter.class);
    private DatabaseManager databaseManager;

    public LikelihoodRateFilter(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void performFilter(String previousTable, String currentTable, String[] args) {
        if (args == null || args.length == 0) {
            return;
        } else if (args.length != 2) {
            throw new IllegalArgumentException("Args " + Arrays.asList(args)
                + " for Likelihood Rate Test Filter are incomplete, please have a check");
        }
        String dnaVcfTable = args[0];
        double threshold = Double.parseDouble(args[1]);
        TableCreator.createFilterTable(previousTable, currentTable);
        logger.info("Start performing Likelihood Rate Test Filter...\t" + Timer.getCurrentTime());
        try {
            ResultSet rs =
                databaseManager.query("select " + previousTable + ".chrom," + previousTable + ".pos," + previousTable
                    + ".AD," + "" + dnaVcfTable + ".qual from " + previousTable + "," + dnaVcfTable + " WHERE "
                    + previousTable + ".chrom=" + dnaVcfTable + ".chrom AND " + previousTable + ".pos=" + dnaVcfTable
                    + ".pos");
            List<SiteBean> siteBeans = new ArrayList<SiteBean>();
            while (rs.next()) {
                String chr = rs.getString(1);
                int pos = rs.getInt(2);
                String ad = rs.getString(3);
                float qual = rs.getFloat(4);
                SiteBean pb = new SiteBean(chr, pos);
                pb.setAd(ad);
                pb.setQual(qual);
                siteBeans.add(pb);
            }
            databaseManager.setAutoCommit(false);
            int count = 0;
            for (SiteBean siteBean : siteBeans) {
                String[] section = siteBean.getAd().split("/");
                int ref = Integer.parseInt(section[0]);
                int alt = Integer.parseInt(section[1]);
                if (ref + alt > 0) {
                    double f_ml = 1.0 * ref / (ref + alt);
                    double y = Math.pow(f_ml, ref) * Math.pow(1 - f_ml, alt);
                    y = Math.log(y) / Math.log(10.0);
                    double judge = y + siteBean.getQual() / 10.0;
                    if (judge >= threshold) {
                        databaseManager.insertClause("insert into " + currentTable + " select * from " + previousTable
                            + " where chrom='" + siteBean.getChr() + "' and pos=" + siteBean.getPos());
                        if (++count % DatabaseManager.COMMIT_COUNTS_PER_ONCE == 0) {
                            databaseManager.commit();
                        }
                    }
                }
            }
            databaseManager.commit();
            databaseManager.setAutoCommit(true);

        } catch (SQLException e) {
            logger.error("Error execute sql clause in " + LikelihoodRateFilter.class.getName() + ":performFilter()", e);
        }
        logger.info("End performing Likelihood Rate Test Filter...\t" + Timer.getCurrentTime());
    }

    @Override
    public String getName() {
        return DatabaseManager.LLR_FILTER_RESULT_TABLE_NAME;
    }
}
