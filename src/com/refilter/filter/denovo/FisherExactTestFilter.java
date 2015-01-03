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

package com.refilter.filter.denovo;

/**
 *P_value based on alt and ref 
 */

import com.refilter.database.DatabaseManager;
import com.refilter.database.TableCreator;
import com.refilter.datatypes.SiteBean;
import com.refilter.filter.Filter;
import com.refilter.utils.NegativeType;
import com.refilter.utils.Timer;
import net.sf.snver.pileup.util.math.FisherExact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rcaller.RCaller;
import rcaller.RCode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FisherExactTestFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(FisherExactTestFilter.class);
    private DatabaseManager databaseManager;

    public FisherExactTestFilter(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    private List<PValueInfo> getExpectedInfo(String refTable, String editingType) {
        List<PValueInfo> valueInfos = new ArrayList<PValueInfo>();
        String darnedTable = DatabaseManager.DARNED_DATABASE_TABLE_NAME;
        try {
            ResultSet rs = databaseManager.query(refTable, null, null, null);
            while (rs.next()) {
                //1.CHROM varchar(15),2.POS int,3.ID varchar(30),4.REF varchar(3),5.ALT varchar(5),6.QUAL float(8,2),7.FILTER text,8.INFO text,9.GT text,
                // 10.AD text,11.DP text,12.GQ text,13.PL text,14.alu varchar(1)
                PValueInfo info = new PValueInfo(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getString(4).charAt(0), rs.getString(5).charAt(0),
                        rs.getFloat(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12),
                        rs.getString(13), rs.getString(14));
                String[] sections = info.getAd().split("/");
                info.refCount = Integer.parseInt(sections[0]);
                info.altCount = Integer.parseInt(sections[1]);
                valueInfos.add(info);
            }

            String negativeType = NegativeType.getNegativeStrandEditingType(editingType);
            char[] editingTypes = editingType.toCharArray();
            char[] negativeTypes = negativeType.toCharArray();
            StringBuilder stringBuilder = new StringBuilder("select ");
            stringBuilder.append(refTable);
            stringBuilder.append(".* from ");
            stringBuilder.append(refTable);
            stringBuilder.append(" INNER JOIN ");
            stringBuilder.append(darnedTable);
            stringBuilder.append(" ON ");
            stringBuilder.append(refTable).append(".chrom=").append(darnedTable).append(".chrom AND ");
            stringBuilder.append(refTable).append(".pos=").append(darnedTable).append(".coordinate AND ");
            stringBuilder.append("(").append(darnedTable).append(".inchr='").append(editingTypes[0]).append("' AND ").append(darnedTable).append(".inrna='")
                    .append(editingTypes[1]).append("' OR ");
            stringBuilder.append(darnedTable).append(".inchr='").append(negativeTypes[0]).append("' AND ").append(darnedTable).append(".inrna='").append
                    (negativeTypes[1]).append("')");
            logger.info(stringBuilder.toString());
            //select refTable.* from refTable INNER JOIN pvalueTable ON refTable.chrom=pvalueTable.chrom and refTable.pos=pvalueTable.coordinate
            rs = databaseManager.query(stringBuilder.toString());
            logger.info(stringBuilder.toString());
            while (rs.next()) {
                for (PValueInfo info : valueInfos) {
                    if (info.getChr().equals(rs.getString(1)) && info.getPos() == rs.getInt(2)) {
                        info.setInDarnedDB(true);
                        break;
                    }
                }
            }
            return valueInfos;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<PValueInfo> executeFETFilter(String previousTable, String fetResultTable, String refAlt) {
        logger.info("Start performing Fisher's Exact Test Filter...\t" + Timer.getCurrentTime());
        List<PValueInfo> valueInfos = getExpectedInfo(previousTable, refAlt);
        int knownAlt = 0;
        int knownRef = 0;
        for (PValueInfo info : valueInfos) {
            if (info.isInDarnedDB) {
                knownAlt += info.altCount;
                knownRef += info.refCount;
            } else {
                knownRef += info.altCount + info.refCount;
            }
        }
        knownAlt = Math.round(knownAlt / valueInfos.size());
        knownRef = Math.round(knownRef / valueInfos.size());
        FisherExact fisherExact = new FisherExact(1000);
        DecimalFormat dF = new DecimalFormat("#.###");
        for (PValueInfo pValueInfo : valueInfos) {
            int altCount = pValueInfo.altCount;
            int refCount = pValueInfo.refCount;
            double pValue = fisherExact.getTwoTailedP(refCount, altCount, knownRef, knownAlt);
            double level = (double) altCount / (altCount + refCount);
            pValueInfo.setPValue(pValue);
            pValueInfo.setLevel(level);
            try {
                databaseManager.executeSQL("insert into " + fetResultTable + "(chrom,pos,id,ref,alt,qual,filter,info,gt,ad,dp,gq,pl,alu,level,pvalue) " +
                        "values( " + pValueInfo.toString() + "," + dF.format(level) + "," + pValue + ")");
            } catch (SQLException e) {
                logger.error("Error execute sql clause in " + FisherExactTestFilter.class.getName() + ":executeFETFilter()", e);
                return new ArrayList<PValueInfo>();
            }
        }
        logger.info("End performing Fisher's Exact Test Filter...\t" + Timer.getCurrentTime());
        return valueInfos;
    }


    @Override
    public void performFilter(String previousTable, String currentTable, String[] args) {
        if (args == null || args.length == 0) {
            return;
        } else if (args.length != 4) {
            logger.error("Args " + Arrays.asList(args) + " for Fisher's Exact Test Filter are incomplete, please have a check");
            throw new IllegalArgumentException("Args " + Arrays.asList(args) + " for Fisher's Exact Test Filter are incomplete, please have a check");
        }
        String rScript = args[0];
        double pvalueThreshold = Double.parseDouble(args[1]);
        double fdrThreshold = Double.parseDouble(args[2]);
        String editingType = args[3];
        TableCreator.createFisherExactTestTable(previousTable, currentTable);
        logger.info("Start performing False Discovery Rate Filter...\t" + Timer.getCurrentTime());
        RCaller caller = new RCaller();
        if (rScript.trim().toLowerCase().contains("script")) {
            caller.setRscriptExecutable(rScript);
        } else {
            caller.setRExecutable(rScript);
        }
        RCode code = new RCode();
        List<PValueInfo> pValueList = executeFETFilter(previousTable, currentTable, editingType);
        double[] pValueArray = new double[pValueList.size()];
        for (int i = 0, len = pValueList.size(); i < len; i++) {
            pValueArray[i] = pValueList.get(i).getPvalue();
        }
        code.addDoubleArray("parray", pValueArray);
        code.addRCode("result<-p.adjust(parray,method='fdr',length(parray))");
        caller.setRCode(code);
        if (rScript.trim().toLowerCase().contains("script")) {
            caller.runAndReturnResult("result");
        } else {
            caller.runAndReturnResultOnline("result");
        }
        double[] results = caller.getParser().getAsDoubleArray("result");
        caller.deleteTempFiles();
        databaseManager.setAutoCommit(false);
        try {
            for (int i = 0, len = results.length; i < len; i++) {
                databaseManager.executeSQL("update " + currentTable + " set fdr=" + results[i] + " where chrom='" + pValueList.get(i).getChr() + "' " +
                        "and pos=" + pValueList.get(i).getPos());
            }
            databaseManager.commit();
            databaseManager.setAutoCommit(true);
            databaseManager.executeSQL("delete from " + currentTable + " where (pvalue > " + pvalueThreshold + ") || (fdr > " + fdrThreshold + ")");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        logger.info("Start performing False Discovery Rate Filter...\t" + Timer.getCurrentTime());
    }

    @Override
    public String getName() {
        return DatabaseManager.FET_FILTER_RESULT_TABLE_NAME;
    }

    private class PValueInfo extends SiteBean {
        public boolean isInDarnedDB = false;
        public int refCount = 0;
        public int altCount = 0;

        public PValueInfo(String chr, int pos, String id, char ref, char alt, float qual, String filter, String info, String gt, String ad, String dp, String gq,
                          String pl, String alu) {
            super(chr, pos, id, ref, alt, qual, filter, info, gt, ad, dp, gq, pl, alu);
        }

        public void setInDarnedDB(boolean isInDarnedDB) {
            this.isInDarnedDB = isInDarnedDB;
        }

        @Override
        public String toString() {

            return "'" + getChr() + "'," + getPos() + ",'" + getId() + "','" + getRef() + "','" + getAlt() + "'," + getQual() + ",'" + getFilter() + "'," +
                    "'" + getInfo() + "','" + getGt() + "','" + getAd() + "','" + getDp() + "','" + getGq() + "','" + getPl() + "','" + getIsAlu() + "'";
        }
    }
}
