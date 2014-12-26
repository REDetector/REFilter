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
import database.TableCreator;
import datatypes.SiteBean;
import filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Timer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Basic process for QC filter means we set threshold on quality and depth
 */
public class QualityControlFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(QualityControlFilter.class);
    private DatabaseManager databaseManager;

    public QualityControlFilter(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void performFilter(String previousTable, String currentTable, String[] args) {
        if (args == null || args.length == 0) {
            return;
        } else if (args.length != 2) {
            throw new IllegalArgumentException("Args " + Arrays.asList(args) + " for Quality Control Filter are incomplete, please have a check");
        }
        double quality = Double.parseDouble(args[0]);
        int depth = Integer.parseInt(args[1]);
        TableCreator.createFilterTable(previousTable, currentTable);
        logger.info("Start performing Quality Control Filter...\t" + Timer.getCurrentTime());
        try {
            int count = 0;
            ResultSet rs = databaseManager.query(previousTable, new String[]{"chrom", "pos", "AD"}, null, null);
            List<SiteBean> siteBeans = new ArrayList<SiteBean>();
            while (rs.next()) {
                if (rs.getString(3) != null) {
                    SiteBean siteBean = new SiteBean(rs.getString(1), rs.getInt(2));
                    siteBean.setAd(rs.getString(3));
                    siteBeans.add(siteBean);
                }
            }
            databaseManager.setAutoCommit(false);
            for (SiteBean siteBean : siteBeans) {
                String[] sections = siteBean.getAd().split("/");
                int ref_n = Integer.parseInt(sections[0]);
                int alt_n = Integer.parseInt(sections[1]);
                if (ref_n + alt_n >= depth) {
                    databaseManager.executeSQL("insert into " + currentTable + " (select * from " + previousTable + " where filter='PASS' and pos=" + siteBean
                            .getPos() + " and qual >=" + quality + " and chrom='" + siteBean.getChr() + "')");
                    if (++count % DatabaseManager.COMMIT_COUNTS_PER_ONCE == 0)
                        databaseManager.commit();
                }
            }
            databaseManager.commit();
            databaseManager.setAutoCommit(true);
        } catch (SQLException e) {
            logger.error("Error execute sql clause in " + QualityControlFilter.class.getName() + ":performFilter()", e);
        }
        logger.info("End performing Quality Control Filter...\t" + Timer.getCurrentTime());
    }

    @Override
    public String getName() {
        return DatabaseManager.QC_FILTER_RESULT_TABLE_NAME;
    }
}
