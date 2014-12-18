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
import datatypes.SiteBean;
import utils.Timer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * LLR used for detecting editing sites
 */


public class LikelihoodRatioFilter {
    private DatabaseManager databaseManager;

    public LikelihoodRatioFilter(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void executeLLRFilter(String llrResultTable, String dnaVcfTable, String refTable, double threshold) {
        try {
            System.out.println("Start executing LikelihoodRatioFilter..." + Timer.getCurrentTime());
            ResultSet rs = databaseManager.query("select " + refTable + ".chrom," + refTable + ".pos," + refTable + ".AD," +
                    "" + dnaVcfTable + ".qual from " + refTable + "," + dnaVcfTable + " WHERE " + refTable + ".chrom=" + dnaVcfTable + ".chrom AND " +
                    refTable + ".pos=" + dnaVcfTable + ".pos");
            //            ResultSet rs = databaseManager.query(refTable + "," + dnaVcfTable, new String[]{refTable + ".chrom", refTable + ".pos", refTable + ".AD",
            //                    dnaVcfTable + ".qual"}, refTable + ".chrom = ? AND " + refTable + ".pos = ? ", new String[]{dnaVcfTable + ".chrom", dnaVcfTable + ".pos"});
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
                        databaseManager.insertClause("insert into " + llrResultTable + " select * from " + refTable + " where chrom='" + siteBean.getChr() +
                                "' and pos=" + siteBean.getPos());
                        if (++count % DatabaseManager.COMMIT_COUNTS_PER_ONCE == 0) {
                            databaseManager.commit();
                        }
                    }
                }
            }
            databaseManager.commit();
            databaseManager.setAutoCommit(true);

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("End executing LikelihoodRatioFilter..." + Timer.getCurrentTime());

    }

}
