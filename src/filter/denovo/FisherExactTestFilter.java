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
 *P_value based on alt and ref 
 */

import database.DatabaseManager;
import datatypes.SiteBean;
import net.sf.snver.pileup.util.math.FisherExact;
import rcaller.RCaller;
import rcaller.RCode;
import utils.Timer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class FisherExactTestFilter {
    private DatabaseManager databaseManager;

    public FisherExactTestFilter(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public boolean hasEstablishedDarnedTable(String darnedTable) {
        try {
            return databaseManager.calRowCount(darnedTable) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void loadDarnedTable(String darnedTable, String darnedPath) {
        System.out.println("Start loading DARNED database..." + Timer.getCurrentTime());
        if (!hasEstablishedDarnedTable(darnedTable)) {
            try {
                int count = 0;
                databaseManager.setAutoCommit(false);
                FileInputStream inputStream = new FileInputStream(darnedPath);
                BufferedReader rin = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                // Skip the first row.
                rin.readLine();
                while ((line = rin.readLine()) != null) {
                    String[] sections = line.trim().split("\\t");
                    StringBuilder stringBuilder = new StringBuilder("insert into ");
                    stringBuilder.append(darnedTable);
                    stringBuilder.append("(chrom,coordinate,strand,inchr,inrna) values(");
                    for (int i = 0; i < 5; i++) {
                        if (i == 0) {
                            stringBuilder.append("'chr").append(sections[i]).append("',");
                        } else if (i == 1) {
                            stringBuilder.append(sections[i]).append(",");
                        } else {
                            stringBuilder.append("'").append(sections[i]).append("',");
                        }
                    }
                    stringBuilder.deleteCharAt(stringBuilder.length() - 1).append(")");

                    databaseManager.executeSQL(stringBuilder.toString());
                    if (++count % DatabaseManager.COMMIT_COUNTS_PER_ONCE == 0)
                        databaseManager.commit();
                }
                databaseManager.commit();
                databaseManager.setAutoCommit(true);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                System.err.println("Error load file from " + darnedPath + " to file stream");
                e.printStackTrace();
            } catch (SQLException e) {
                System.err.println("Error execute sql clause in " + FisherExactTestFilter.class.getName() + ":loadDarnedTable()");
                e.printStackTrace();
            }
        }
        System.out.println("End loading DARNED database..." + Timer.getCurrentTime());
    }

    private List<PValueInfo> getExpectedInfo(String darnedTable, String refTable) {
        List<PValueInfo> valueInfos = new ArrayList<PValueInfo>();
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
            //select refTable.* from refTable INNER JOIN pvalueTable ON refTable.chrom=pvalueTable.chrom and refTable.pos=pvalueTable.coordinate
            rs = databaseManager.query("select " + refTable + ".* from " + refTable + " INNER JOIN " + darnedTable + " ON " + refTable + ".chrom=" + darnedTable + ".chrom" +
                    " and " + refTable + ".pos=" + darnedTable + ".coordinate and " + darnedTable + ".inchr='A' and (" + darnedTable + ".inrna='I' or " +
                    darnedTable + ".inrna='G')");
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
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    private List<PValueInfo> executeFETFilter(String darnedTable, String fetResultTable, String refTable) {
        System.out.println("Start executing Fisher Exact Test Filter..." + Timer.getCurrentTime());
        List<PValueInfo> valueInfos = getExpectedInfo(darnedTable, refTable);
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
            int alt = pValueInfo.altCount;
            int ref = pValueInfo.refCount;
            double pValue = fisherExact.getTwoTailedP(ref, alt, knownRef, knownAlt);
            double level = (double) alt / (alt + ref);
            pValueInfo.setPValue(pValue);
            pValueInfo.setLevel(level);
            try {
                databaseManager.executeSQL("insert into " + fetResultTable + "(chrom,pos,id,ref,alt,qual,filter,info,gt,ad,dp,gq,pl,alu,level,pvalue) " +
                        "values( " + pValueInfo.toString() + "," + dF.format(level) + "," + pValue + ")");
            } catch (SQLException e) {
                System.err.println("Error execute sql clause in " + FisherExactTestFilter.class.getName() + ":executeFETFilter()");
                e.printStackTrace();
                return new ArrayList<PValueInfo>();
            }
        }
        System.out.println("End executing Fisher Exact Test Filter..." + Timer.getCurrentTime());
        return valueInfos;
    }

    public void executeFDRFilter(String darnedTable, String darnedResultTable, String refTable, String rScript, double pvalueThreshold, double fdrThreshold) {
        System.out.println("Start executing FDRFilter..." + Timer.getCurrentTime());
        try {
            RCaller caller = new RCaller();
            if (rScript.trim().toLowerCase().contains("script")) {
                caller.setRscriptExecutable(rScript);
            } else {
                caller.setRExecutable(rScript);
            }
            RCode code = new RCode();
            List<PValueInfo> pValueList = executeFETFilter(darnedTable, darnedResultTable, refTable);
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
            for (int i = 0, len = results.length; i < len; i++) {
                databaseManager.executeSQL("update " + darnedResultTable + " set fdr=" + results[i] + " where chrom='" + pValueList.get(i).getChr() + "' " +
                        "and pos=" + pValueList.get(i).getPos());
            }
            databaseManager.commit();
            databaseManager.setAutoCommit(true);
            databaseManager.executeSQL("delete from " + darnedResultTable + " where (pvalue > " + pvalueThreshold + ") || (fdr > " + fdrThreshold + ")");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("End executing FDRFilter..." + Timer.getCurrentTime());
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
