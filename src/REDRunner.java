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

import database.DatabaseManager;
import database.TableCreator;
import dataparser.DNAVCFParser;
import dataparser.RNAVCFParser;
import filter.denovo.*;
import filter.dnarna.DNARNAFilter;
import filter.dnarna.LikelihoodRatioFilter;
import utils.DatabasePreferences;
import utils.Timer;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Xing Li on 2014/11/20.
 * <p/>
 * A command line tool to run RNA Editing Filters.
 */
public class REDRunner {

    public static String HOST = "127.0.0.1";
    public static String PORT = "3306";
    public static String USER = "root";
    public static String PWD = "root";
    public static String MODE = "dnarna";
    public static String INPUT = "";
    public static String OUTPUT = "";
    public static String EXPORT = "all";
    public static String RNAVCF = "";
    public static String DNAVCF = "";
    public static String DARNED = "";
    public static String SPLICE = "";
    public static String REPEAT = "";
    public static String DBSNP = "";
    public static String RSCRIPT = "/usr/bin/RScript";

    /**
     * A tool to run RED using command line.
     *
     * @param args To run RED in a easy way, we define some parameters as following illustrated.
     *             <p/>
     *             -h, --help   Print short help message and exit
     *             <p/>
     *             --version Print version info and exit
     *             <p/>
     *             -H, --host=127.0.0.1    The host address of MySQL database;
     *             <p/>
     *             -p, --port=    The port used in MySQL;
     *             <p/>
     *             -u, --user    MySQL user name;
     *             <p/>
     *             -P, --pwd     MySQL password of user;
     *             <p/>
     *             -m, --mode  Tell the program if it is denovo mode of DNA-RNA mode
     *             <p/>
     *             -i, --input  Input all required files in order (i.e., RNA VCF File, DNA VCF File, DARNED Database, Gene Annotation File, RepeatMasker
     *             Database File, dbSNP Database File) instead of single input, each file should be divided with ','.
     *             <p/>
     *             --rnavcf  File path of RNA VCF file;
     *             <p/>
     *             --dnavcf  File path of DNA VCF file;
     *             <p/>
     *             --darned  File path of DARNED database;
     *             <p/>
     *             --splice  File path of annotation genes like "gene.gft";
     *             <p/>
     *             --repeat  File path of Repeat Masker database;
     *             <p/>
     *             --dbsnp   File path of dbSNP database;
     *             <p/>
     *             --rscript File path of RScript;
     *             <p/>
     *             --export  Export all final sites for annotation;
     *             <p/>
     *             --path    Export path.
     */
    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.equals("-h") || arg.equals("--help")) {
                System.out.println(getHelp());
                return;
            }
            if (arg.equals("-v") || arg.equals("--version")) {
                System.out.println(getVersion());
                return;
            }
        }
        Map<Character, String> singleMap = new HashMap<Character, String>();
        Map<String, String> doubleMap = new HashMap<String, String>();
        for (int i = 0, len = args.length; i < len; i++) {
            if (args[i].startsWith("--")) {
                String[] sections = args[i].substring(2).split("=");
                doubleMap.put(sections[0], sections[1]);
            } else if (args[i].startsWith("-") && !args[i].startsWith("--")) {
                char c = args[i].charAt(1);
                singleMap.put(c, args[i + 1]);
                i++;
            } else {
                throw new IllegalArgumentException("Wrong input parameters, please have a check.");
            }
        }

        for (Map.Entry entry : singleMap.entrySet()) {
            char key = (Character) entry.getKey();
            String value = entry.getValue().toString();
            switch (key) {
                case 'H':
                    HOST = value;
                    break;
                case 'p':
                    PORT = value;
                    break;
                case 'u':
                    USER = value;
                    break;
                case 'P':
                    PWD = value;
                    break;
                case 'm':
                    MODE = value;
                    break;
                case 'i':
                    INPUT = value;
                    break;
                case 'o':
                    OUTPUT = value;
                    break;
                case 'e':
                    EXPORT = value;
                    break;
                case 'r':
                    RSCRIPT = value;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown the argument '-" + key + "', please have a check.");
            }
        }

        for (Map.Entry entry : doubleMap.entrySet()) {
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();
            if (key.equalsIgnoreCase("host")) {
                HOST = value;
            } else if (key.equalsIgnoreCase("port")) {
                PORT = value;
            } else if (key.equalsIgnoreCase("user")) {
                USER = value;
            } else if (key.equalsIgnoreCase("pwd")) {
                PWD = value;
            } else if (key.equalsIgnoreCase("mode")) {
                MODE = value;
            } else if (key.equalsIgnoreCase("input")) {
                INPUT = value;
            } else if (key.equalsIgnoreCase("output")) {
                OUTPUT = value;
            } else if (key.equalsIgnoreCase("export")) {
                EXPORT = value;
            } else if (key.equalsIgnoreCase("rscript")) {
                RSCRIPT = value;
            } else if (key.equalsIgnoreCase("rnavcf")) {
                RNAVCF = value;
            } else if (key.equalsIgnoreCase("dnavcf")) {
                DNAVCF = value;
            } else if (key.equalsIgnoreCase("darned")) {
                DARNED = value;
            } else if (key.equalsIgnoreCase("splice")) {
                SPLICE = value;
            } else if (key.equalsIgnoreCase("repeat")) {
                REPEAT = value;
            } else if (key.equalsIgnoreCase("dbsnp")) {
                DBSNP = value;
            } else {
                throw new IllegalArgumentException("Unknown the argument '--" + key + "', please have a check.");
            }
        }

        if (INPUT != null) {
            String[] sections = INPUT.split(",");
            if (MODE.equalsIgnoreCase("dnarna") && sections.length == 6) {
                RNAVCF = sections[0];
                DNAVCF = sections[1];
                DARNED = sections[2];
                SPLICE = sections[3];
                REPEAT = sections[4];
                DBSNP = sections[5];
            } else if (MODE.equalsIgnoreCase("denovo") && sections.length == 5) {
                RNAVCF = sections[0];
                DARNED = sections[1];
                SPLICE = sections[2];
                REPEAT = sections[3];
                DBSNP = sections[4];
            } else {
                throw new IllegalArgumentException("Unknown the argument '--INPUT " + INPUT + "' or it is incomplete, please have a check.");
            }
        }

        DatabaseManager manager = DatabaseManager.getInstance();
        try {
            manager.connectDatabase(HOST, PORT, USER, PWD);
            manager.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }

        File rootPath = new File(OUTPUT);
        try {
            if (!rootPath.exists()) {
                if (!rootPath.mkdirs()) {
                    throw new IOException("File path '" + rootPath.getAbsolutePath() + "' can't not be created. Make sure you have the file permission.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        boolean denovo;
        //Data import for five or six files, which is depended on denovo mode or DNA-RNA mode.
        if (MODE.equalsIgnoreCase("dnarna")) {
            DatabasePreferences.getInstance().setCurrentDatabase(DatabaseManager.DNA_RNA_MODE_DATABASE_NAME);
            manager.createDatabase(DatabaseManager.DNA_RNA_MODE_DATABASE_NAME);
            manager.useDatabase(DatabaseManager.DNA_RNA_MODE_DATABASE_NAME);
            denovo = false;
        } else if (MODE.equalsIgnoreCase("denovo")) {
            DatabasePreferences.getInstance().setCurrentDatabase(DatabaseManager.DENOVO_MODE_DATABASE_NAME);
            manager.createDatabase(DatabaseManager.DENOVO_MODE_DATABASE_NAME);
            manager.useDatabase(DatabaseManager.DENOVO_MODE_DATABASE_NAME);
            denovo = true;
        } else {
            throw new IllegalArgumentException("Unknown the mode '" + MODE + "', please have a check.");
        }

        String logPath = rootPath + File.separator + "RED_logs";
        File logDir = new File(logPath);
        try {
            if (!logDir.exists()) {
                if (!logDir.mkdir()) {
                    throw new IOException("File path '" + logDir.getAbsolutePath() + "' can't not be created. Make sure you have the file permission.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            String tempPath = logDir.getAbsolutePath() + File.separator + "temp_" + Timer.getExportTime() + ".log";
            File tempFile = new File(tempPath);
            PrintStream p = new PrintStream(new FileOutputStream(tempFile));

            // Next, print the start time of the data import.
            String startTime = Timer.getCurrentTime();
            p.println("Start importing data at :\t" + startTime);
            String[] rnaVCFSampleNames;
            if (RNAVCF != null && RNAVCF.length() != 0) {
                RNAVCFParser rnavcfParser = new RNAVCFParser();
                rnavcfParser.parseMultiVCFFile(RNAVCF);
                rnaVCFSampleNames = rnavcfParser.getSampleNames();
            } else {
                throw new NullPointerException("RNA VCF file is empty, please have a check.");
            }
            String[] dnaVCFSampleNames;
            if (!denovo) {
                if (DNAVCF != null && DNAVCF.length() != 0) {
                    DNAVCFParser dnavcfParser = new DNAVCFParser();
                    dnavcfParser.parseMultiVCFFile(DNAVCF);
                    dnaVCFSampleNames = dnavcfParser.getSampleNames();
                } else {
                    throw new NullPointerException("DNA VCF file is empty, please have a check.");
                }

                boolean match = false;
                for (String rnaSample : rnaVCFSampleNames) {
                    match = false;
                    for (String dnaSample : dnaVCFSampleNames) {
                        if (rnaSample.equalsIgnoreCase(dnaSample)) {
                            match = true;
                        }
                    }
                }
                if (!match) {
                    throw new IllegalArgumentException("Samples in DNA VCF file does not match the RNA VCF, please have a check the sample name.");
                }
            }

            if (REPEAT != null && REPEAT.length() != 0) {
                RepeatRegionsFilter rf = new RepeatRegionsFilter(manager);
                TableCreator.createRepeatRegionsTable(DatabaseManager.REPEAT_MASKER_TABLE_NAME);
                rf.loadRepeatTable(DatabaseManager.REPEAT_MASKER_TABLE_NAME, REPEAT);
            }

            if (SPLICE != null && SPLICE.length() != 0) {
                SpliceJunctionFilter cf = new SpliceJunctionFilter(manager);
                TableCreator.createSpliceJunctionTable(DatabaseManager.SPLICE_JUNCTION_TABLE_NAME);
                cf.loadSpliceJunctionTable(DatabaseManager.SPLICE_JUNCTION_TABLE_NAME, SPLICE);
            }
            if (DBSNP != null && DBSNP.length() != 0) {
                KnownSNPFilter sf = new KnownSNPFilter(manager);
                TableCreator.createDBSNPTable(DatabaseManager.DBSNP_DATABASE_TABLE_NAME);
                sf.loadDbSNPTable(DatabaseManager.DBSNP_DATABASE_TABLE_NAME, DBSNP);
            }

            if (DARNED != null && DARNED.length() != 0) {
                FisherExactTestFilter pv = new FisherExactTestFilter(manager);
                TableCreator.createDARNEDTable(DatabaseManager.DARNED_DATABASE_TABLE_NAME);
                pv.loadDarnedTable(DatabaseManager.DARNED_DATABASE_TABLE_NAME, DARNED);
            }

            String endTime = Timer.getCurrentTime();
            p.println("End importing data at :\t" + endTime);
            p.println("Data import lasts for :\t" + Timer.calculateInterval(startTime, endTime));
            p.println();
            for (String sample : rnaVCFSampleNames) {
                // First, print base information of all data.

                p.println("------------------------ Sample name : " + sample + " ------------------------");
                if (!denovo) {
                    p.println("Mode:\t" + DatabaseManager.DNA_RNA_MODE_DATABASE_NAME);
                    p.println("DNA VCF File :\t" + DNAVCF);
                } else {
                    p.println("Mode:\t" + DatabaseManager.DENOVO_MODE_DATABASE_NAME);
                }
                p.println("RNA VCF File :\t" + RNAVCF);
                p.println("DARNED File :\t" + DARNED);
                p.println("Splice Junction File :\t" + SPLICE);
                p.println("Repeat Masker File :\t" + REPEAT);
                p.println("dbSNP File :\t" + DBSNP);
                p.println("RScript Path :\t" + RSCRIPT);

                String rawFilterName = sample + "_" + DatabaseManager.RNA_VCF_RESULT_TABLE_NAME;
                String dnavcfTableName = sample + "_" + DatabaseManager.DNA_VCF_RESULT_TABLE_NAME;
                String etfFilterName = sample + "_" + DatabaseManager.RNA_VCF_RESULT_TABLE_NAME + "_" + DatabaseManager.EDITING_TYPE_FILTER_RESULT_TABLE_NAME;
                String qcFilterName = sample + "_" + DatabaseManager.EDITING_TYPE_FILTER_RESULT_TABLE_NAME + "_" + DatabaseManager.QC_FILTER_RESULT_TABLE_NAME;
                String rrFilterName = sample + "_" + DatabaseManager.QC_FILTER_RESULT_TABLE_NAME + "_" + DatabaseManager.REPEAT_FILTER_RESULT_TABLE_NAME;
                String aluFilterName = sample + "_" + DatabaseManager.QC_FILTER_RESULT_TABLE_NAME + "_" + DatabaseManager.ALU_FILTER_RESULT_TABLE_NAME;
                String sjFilterName = sample + "_" + DatabaseManager.REPEAT_FILTER_RESULT_TABLE_NAME + "_" + DatabaseManager.SPLICE_JUNCTION_FILTER_RESULT_TABLE_NAME;
                String ksfFilterName = sample + "_" + DatabaseManager.SPLICE_JUNCTION_FILTER_RESULT_TABLE_NAME + "_" + DatabaseManager.DBSNP_FILTER_RESULT_TABLE_NAME;
                String drFilterName = sample + "_" + DatabaseManager.DBSNP_FILTER_RESULT_TABLE_NAME + "_" + DatabaseManager.DNA_RNA_FILTER_RESULT_TABLE_NAME;
                String llrFilterName = sample + "_" + DatabaseManager.DNA_RNA_FILTER_RESULT_TABLE_NAME + "_" + DatabaseManager.LLR_FILTER_RESULT_TABLE_NAME;
                String fetFilterName;
                if (!denovo) {
                    fetFilterName = sample + "_" + DatabaseManager.LLR_FILTER_RESULT_TABLE_NAME + "_" + DatabaseManager.FET_FILTER_RESULT_TABLE_NAME;
                } else {
                    fetFilterName = sample + "_" + DatabaseManager.DBSNP_FILTER_RESULT_TABLE_NAME + "_" + DatabaseManager.FET_FILTER_RESULT_TABLE_NAME;
                }

                startTime = Timer.getCurrentTime();
                p.println("Start performing filters at :\t" + startTime);
                // Filter execution
                EditingTypeFilter etf = new EditingTypeFilter(manager);
                TableCreator.createFilterTable(etfFilterName);
                etf.executeEditingTypeFilter(etfFilterName, rawFilterName, "A", "G");
                DatabaseManager.getInstance().distinctTable(etfFilterName);

                QualityControlFilter qcf = new QualityControlFilter(manager);
                TableCreator.createFilterTable(qcFilterName);
                qcf.executeQCFilter(etfFilterName, qcFilterName, 20, 6);
                DatabaseManager.getInstance().distinctTable(qcFilterName);

                RepeatRegionsFilter rrf = new RepeatRegionsFilter(manager);
                TableCreator.createFilterTable(rrFilterName);
                TableCreator.createFilterTable(aluFilterName);
                rrf.executeRepeatFilter(DatabaseManager.REPEAT_MASKER_TABLE_NAME, rrFilterName, aluFilterName, qcFilterName);
                DatabaseManager.getInstance().distinctTable(rrFilterName);

                SpliceJunctionFilter sjf = new SpliceJunctionFilter(manager);
                TableCreator.createFilterTable(sjFilterName);
                sjf.executeSpliceJunctionFilter(DatabaseManager.SPLICE_JUNCTION_TABLE_NAME, sjFilterName, rrFilterName, 2);
                DatabaseManager.getInstance().distinctTable(sjFilterName);

                KnownSNPFilter ksf = new KnownSNPFilter(manager);
                TableCreator.createFilterTable(ksfFilterName);
                ksf.executeDbSNPFilter(DatabaseManager.DBSNP_DATABASE_TABLE_NAME, ksfFilterName, sjFilterName);
                DatabaseManager.getInstance().distinctTable(ksfFilterName);

                if (!denovo) {
                    DNARNAFilter drf = new DNARNAFilter(manager);
                    TableCreator.createFilterTable(drFilterName);
                    drf.executeDnaRnaFilter(drFilterName, dnavcfTableName, ksfFilterName);
                    DatabaseManager.getInstance().distinctTable(drFilterName);

                    LikelihoodRatioFilter llrf = new LikelihoodRatioFilter(manager);
                    TableCreator.createFilterTable(llrFilterName);
                    llrf.executeLLRFilter(llrFilterName, dnavcfTableName, drFilterName, 4);
                    DatabaseManager.getInstance().distinctTable(llrFilterName);

                    FisherExactTestFilter fetf = new FisherExactTestFilter(manager);
                    TableCreator.createFisherExactTestTable(fetFilterName);
                    fetf.executeFDRFilter(DatabaseManager.DARNED_DATABASE_TABLE_NAME, fetFilterName, llrFilterName, RSCRIPT, 0.05, 0.05);
                    DatabaseManager.getInstance().distinctTable(fetFilterName);
                } else {
                    FisherExactTestFilter fetf = new FisherExactTestFilter(manager);
                    TableCreator.createFisherExactTestTable(fetFilterName);
                    fetf.executeFDRFilter(DatabaseManager.DARNED_DATABASE_TABLE_NAME, fetFilterName, ksfFilterName, RSCRIPT, 0.05, 0.05);
                    DatabaseManager.getInstance().distinctTable(fetFilterName);

                }
                endTime = Timer.getCurrentTime();
                p.println("End performing filters at :\t" + endTime);
                p.println("Filter performance lasts for :\t" + Timer.calculateInterval(startTime, endTime));

                p.flush();
            }
            String logName;
            if (rnaVCFSampleNames.length == 1) {
                logName = rnaVCFSampleNames[0];
            } else {
                logName = "multi_samples";
            }
            String logFile = logPath + "/" + logName + "_" + (denovo ? "denovo" : "dnarna") + "_" + Timer.getExportTime() + ".log";
            try {
                if (!tempFile.renameTo(new File(logFile))) {
                    throw new IOException("Temp file '" + tempFile.getAbsolutePath() + "' can't not be renamed. Make sure you have the file permission.");
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            p.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        if (EXPORT != null && EXPORT.length() != 0) {
            try {
                File resultPath = new File(rootPath + File.separator + "RED_results");
                if (!resultPath.exists()) {

                    if (!resultPath.mkdir()) {
                        throw new IOException("File path '" + resultPath.getAbsolutePath() + "' can't not be created. Make sure you have the file permission.");
                    }

                }
                exportData(resultPath, EXPORT.split(","), denovo ? DatabaseManager.DENOVO_MODE_DATABASE_NAME : DatabaseManager.DNA_RNA_MODE_DATABASE_NAME);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getVersion() {
        return "REFilters version 0.0.1 (2014-12-17)\n" +
                "\n" +
                "Copyright (C) <2014>  <Xing Li>\n" +
                "\n" +
                "REFilter is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.\n" +
                "\n" +
                "REFilter is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.\n" +
                "\n" +
                "You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.";
    }

    public static String getHelp() {
        return "\t-h, --help   \t\t\tPrint short help message and exit;\n" +
                "\t--version \t\t\t\tPrint version info and exit;\n" +
                "\t-H, --host=127.0.0.1    The host address of MySQL database;\n" +
                "\t-p, --port=3306    \t\tThe port used in MySQL;\n" +
                "\t-u, --user=root    \t\tMySQL user name;\n" +
                "\t-P, --pwd=root     \t\tMySQL password of user;\n" +
                "\t-m, --mode=dnarna  \t\t\tTell the program if it is denovo mode or DNARNA mode;\n" +
                "\t-i, --input  \t\t\tInput all required files in order (i.e., RNA VCF File, DNA VCF File, DARNED Database, Gene Annotation File, RepeatMasker Database File, dbSNP Database File) instead of single input, each file should be divided with ',';\n" +
                "\t-o, --output=./    \t\tSet export path for the results in database;\n" +
                "\t-e, --export=all  \t\t\tExport the needed columns in database, which must be the column name of a table in database, the column names should be divided by ',';\n" +
                "\t--rnavcf  \t\t\t\tFile path of RNA VCF file;\n" +
                "\t--dnavcf  \t\t\t\tFile path of DNA VCF file;\n" +
                "\t--darned  \t\t\t\tFile path of DARNED database;\n" +
                "\t--splice  \t\t\t\tFile path of annotation genes like \"gene.gft\";\n" +
                "\t--repeat  \t\t\t\tFile path of Repeat Masker database;\n" +
                "\t--dbsnp   \t\t\t\tFile path of dbSNP database;\n" +
                "\t--rscript \t\t\t\tFile path of RScript.";
    }

    public static void exportData(File resultPath, String[] columns, String databaseName) throws IOException {
        try {
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            databaseManager.useDatabase(databaseName);
            List<String> denovoTables = DatabaseManager.getInstance().getCurrentTables(databaseName);
            for (String denovoTable : denovoTables) {
                if (denovoTable.contains(DatabaseManager.FET_FILTER_RESULT_TABLE_NAME)) {
                    String sample = DatabaseManager.getInstance().getSampleName(denovoTable);
                    StringBuilder builder = new StringBuilder(sample);
                    for (String column : columns) {
                        builder.append("_").append(column);
                    }
                    if (DatabaseManager.DENOVO_MODE_DATABASE_NAME.equals(databaseName)) {
                        builder.append("_denovo");
                    } else {
                        builder.append("_dnarna");
                    }
                    builder.append(".txt");
                    File f = new File(resultPath + File.separator + builder.toString());
                    PrintWriter pw = new PrintWriter(new FileWriter(f));
                    //                    pw.println("pos");
                    ResultSet rs;
                    if (columns.length == 1 && columns[0].toLowerCase().equals("all")) {
                        rs = databaseManager.query(denovoTable, null, null, null);
                    } else {
                        rs = databaseManager.query(denovoTable, columns, null, null);
                    }
                    while (rs.next()) {
                        builder = new StringBuilder();
                        for (String column : columns) {
                            builder.append(rs.getString(column)).append("\t");
                        }
                        pw.println(builder.toString().trim());
                    }
                    pw.flush();
                    pw.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
