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
    public static String DATABASE = DatabaseManager.DNA_RNA_MODE_DATABASE_NAME;
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
                case 'd':
                    DATABASE = value;
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
            } else if (key.equalsIgnoreCase("database")) {
                DATABASE = value;
            } else if (key.equalsIgnoreCase("input")) {
                INPUT = value;
            } else if (key.equalsIgnoreCase("output")) {
                OUTPUT = value;
            } else if (key.equalsIgnoreCase("export")) {
                EXPORT = value;
            } else if (key.equalsIgnoreCase("r")) {
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

        if (INPUT.length() != 0) {
            String[] sections = INPUT.split(",");
            if (MODE.equalsIgnoreCase("dnarna") && sections.length == 6) {
                RNAVCF = sections[0].trim();
                DNAVCF = sections[1].trim();
                DARNED = sections[2].trim();
                SPLICE = sections[3].trim();
                REPEAT = sections[4].trim();
                DBSNP = sections[5].trim();
            } else if (MODE.equalsIgnoreCase("denovo") && sections.length == 5) {
                RNAVCF = sections[0].trim();
                DARNED = sections[1].trim();
                SPLICE = sections[2].trim();
                REPEAT = sections[3].trim();
                DBSNP = sections[4].trim();
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

        File root = new File(OUTPUT);
        String rootPath = root.getAbsolutePath();
        File rootFile = new File(rootPath);
        try {
            if (!rootFile.exists()) {
                if (!rootFile.mkdirs()) {
                    throw new IOException("Root path '" + rootFile.getAbsolutePath() + "' can not be created. Make sure you have the file permission.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        boolean denovo;
        //Data import for five or six files, which is depended on denovo mode or DNA-RNA mode.
        if (DATABASE.length() != 0) {

            denovo = MODE.equalsIgnoreCase("denovo");
        } else if (MODE.equalsIgnoreCase("dnarna")) {
            DATABASE = DatabaseManager.DNA_RNA_MODE_DATABASE_NAME;
            denovo = false;
        } else if (MODE.equalsIgnoreCase("denovo")) {
            DATABASE = DatabaseManager.DENOVO_MODE_DATABASE_NAME;
            denovo = true;
        } else {
            throw new IllegalArgumentException("Unknown the mode '" + MODE + "', please have a check.");
        }
        DatabasePreferences.getInstance().setCurrentDatabase(DATABASE);
        manager.createDatabase(DATABASE);
        manager.useDatabase(DATABASE);

        File resultPath = new File(rootPath + File.separator + "RED_results");
        try {
            if (!resultPath.exists()) {
                if (!resultPath.mkdir()) {
                    throw new IOException("Result path '" + resultPath.getAbsolutePath() + "' can not be created. Make sure you have the file " +
                            "permission.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if (EXPORT.length() != 0 && (RNAVCF.length() == 0 || (!denovo && DNAVCF.length() == 0))) {
            try {
                exportData(resultPath, EXPORT.split(","), DATABASE);
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        String logPath = rootPath + File.separator + "RED_logs";
        File logDir = new File(logPath);
        try {
            if (!logDir.exists()) {
                if (!logDir.mkdir()) {
                    throw new IOException("Log path '" + logDir.getAbsolutePath() + "' can not be created. Make sure you have the file permission.");
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
            if (RNAVCF.length() != 0) {
                RNAVCFParser rnavcfParser = new RNAVCFParser();
                rnavcfParser.parseMultiVCFFile(RNAVCF);
                rnaVCFSampleNames = rnavcfParser.getSampleNames();
            } else {
                throw new NullPointerException("RNA VCF file is empty, please have a check.");
            }
            String[] dnaVCFSampleNames;
            if (!denovo) {
                if (DNAVCF.length() != 0) {
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

            if (REPEAT.length() != 0) {
                RepeatRegionsFilter rf = new RepeatRegionsFilter(manager);
                TableCreator.createRepeatRegionsTable(DatabaseManager.REPEAT_MASKER_TABLE_NAME);
                rf.loadRepeatTable(DatabaseManager.REPEAT_MASKER_TABLE_NAME, REPEAT);
            }

            if (SPLICE.length() != 0) {
                SpliceJunctionFilter cf = new SpliceJunctionFilter(manager);
                TableCreator.createSpliceJunctionTable(DatabaseManager.SPLICE_JUNCTION_TABLE_NAME);
                cf.loadSpliceJunctionTable(DatabaseManager.SPLICE_JUNCTION_TABLE_NAME, SPLICE);
            }
            if (DBSNP.length() != 0) {
                KnownSNPFilter sf = new KnownSNPFilter(manager);
                TableCreator.createDBSNPTable(DatabaseManager.DBSNP_DATABASE_TABLE_NAME);
                sf.loadDbSNPTable(DatabaseManager.DBSNP_DATABASE_TABLE_NAME, DBSNP);
            }
            if (DARNED.length() != 0) {
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
                    p.println("Mode:\tDNA-RNA Mode");
                    p.println("DNA VCF File :\t" + DNAVCF);
                } else {
                    p.println("Mode:\tde novo Mode");
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

                String drFilterName = sample + "_" + DatabaseManager.QC_FILTER_RESULT_TABLE_NAME + "_" + DatabaseManager.DNA_RNA_FILTER_RESULT_TABLE_NAME;
                String llrFilterName = sample + "_" + DatabaseManager.DNA_RNA_FILTER_RESULT_TABLE_NAME + "_" + DatabaseManager.LLR_FILTER_RESULT_TABLE_NAME;
                String ksfFilterName;

                if (!denovo) {
                    ksfFilterName = sample + "_" + DatabaseManager.LLR_FILTER_RESULT_TABLE_NAME + "_" + DatabaseManager.DBSNP_FILTER_RESULT_TABLE_NAME;
                } else {
                    ksfFilterName = sample + "_" + DatabaseManager.QC_FILTER_RESULT_TABLE_NAME + "_" + DatabaseManager.DBSNP_FILTER_RESULT_TABLE_NAME;
                }

                String sjFilterName = sample + "_" + DatabaseManager.DBSNP_FILTER_RESULT_TABLE_NAME + "_" + DatabaseManager.SPLICE_JUNCTION_FILTER_RESULT_TABLE_NAME;
                String aluFilterName = sample + "_" + DatabaseManager.SPLICE_JUNCTION_FILTER_RESULT_TABLE_NAME + "_" + DatabaseManager.ALU_FILTER_RESULT_TABLE_NAME;
                String rrFilterName = sample + "_" + DatabaseManager.SPLICE_JUNCTION_FILTER_RESULT_TABLE_NAME + "_" + DatabaseManager.REPEAT_FILTER_RESULT_TABLE_NAME;


                String fetFilterName = sample + "_" + DatabaseManager.REPEAT_FILTER_RESULT_TABLE_NAME + "_" + DatabaseManager.FET_FILTER_RESULT_TABLE_NAME;


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

                if (!denovo) {
                    DNARNAFilter drf = new DNARNAFilter(manager);
                    TableCreator.createFilterTable(drFilterName);
                    drf.executeDnaRnaFilter(drFilterName, dnavcfTableName, etfFilterName);
                    DatabaseManager.getInstance().distinctTable(drFilterName);

                    LikelihoodRatioFilter llrf = new LikelihoodRatioFilter(manager);
                    TableCreator.createFilterTable(llrFilterName);
                    llrf.executeLLRFilter(llrFilterName, dnavcfTableName, drFilterName, 4);
                    DatabaseManager.getInstance().distinctTable(llrFilterName);

                    KnownSNPFilter ksf = new KnownSNPFilter(manager);
                    TableCreator.createFilterTable(ksfFilterName);
                    ksf.executeDbSNPFilter(DatabaseManager.DBSNP_DATABASE_TABLE_NAME, ksfFilterName, llrFilterName);
                    DatabaseManager.getInstance().distinctTable(ksfFilterName);
                } else {
                    KnownSNPFilter ksf = new KnownSNPFilter(manager);
                    TableCreator.createFilterTable(ksfFilterName);
                    ksf.executeDbSNPFilter(DatabaseManager.DBSNP_DATABASE_TABLE_NAME, ksfFilterName, qcFilterName);
                    DatabaseManager.getInstance().distinctTable(ksfFilterName);
                }

                SpliceJunctionFilter sjf = new SpliceJunctionFilter(manager);
                TableCreator.createFilterTable(sjFilterName);
                sjf.executeSpliceJunctionFilter(DatabaseManager.SPLICE_JUNCTION_TABLE_NAME, sjFilterName, ksfFilterName, 2);
                DatabaseManager.getInstance().distinctTable(sjFilterName);

                RepeatRegionsFilter rrf = new RepeatRegionsFilter(manager);
                TableCreator.createFilterTable(rrFilterName);
                TableCreator.createFilterTable(aluFilterName);
                rrf.executeRepeatFilter(DatabaseManager.REPEAT_MASKER_TABLE_NAME, rrFilterName, aluFilterName, sjFilterName);
                DatabaseManager.getInstance().distinctTable(rrFilterName);

                FisherExactTestFilter fetf = new FisherExactTestFilter(manager);
                TableCreator.createFisherExactTestTable(fetFilterName);
                fetf.executeFDRFilter(DatabaseManager.DARNED_DATABASE_TABLE_NAME, fetFilterName, llrFilterName, RSCRIPT, 0.05, 0.05);
                DatabaseManager.getInstance().distinctTable(fetFilterName);

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
                    throw new IOException("Temp file '" + tempFile.getAbsolutePath() + "' can not be renamed. Make sure you have the file permission.");
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
        if (EXPORT.length() != 0) {
            try {
                exportData(resultPath, EXPORT.split(","), DATABASE);
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
        return "Usage: java -jar jarfile [-h|--help] [-v|--version] [-H|--host[=127.0.0.1]] [-p|--port[=3306]] [-u|--user[=root]] [-P|--pwd[=root]] [-m|--mode[=dnarna]] [-i|--input] [-o|--output[=./]] [-e|--export[=all]] [--rnavcf] [--dnavcf] [--darned] [--splice] [--repeat] [--dbsnp]\n" +
                "\n" +
                "The most commonly used REFilters commands are:\n" +
                "\t-h, --help   \t\t\tPrint short help message and exit;\n" +
                "\t-v, --version \t\t\tPrint version info and exit;\n" +
                "\t-H, --host=127.0.0.1    The host address of MySQL database;\n" +
                "\t-p, --port=3306    \t\tThe port used in MySQL;\n" +
                "\t-u, --user=root    \t\tMySQL user name;\n" +
                "\t-P, --pwd=root     \t\tMySQL password of user;\n" +
                "\t-m, --mode=dnarna  \t\tTell the program if it is denovo mode or DNARNA mode;\n" +
                "\t-i, --input  \t\t\tInput all required files in order (i.e., RNA VCF File, DNA VCF File, DARNED Database, Gene Annotation File, RepeatMasker Database File, dbSNP Database File) instead of single input, each file should be divided with ',' and there should not be blank with each file;\n" +
                "\t-o, --output=./    \t\tSet export path for the results in database, default path is current directory;\n" +
                "\t-e, --export=all  \t\tExport the needed columns in database, which must be the column name of a table in database, the column names should be divided by ',';\n" +
                "\t--rnavcf  \t\t\t\tFile path of RNA VCF file;\n" +
                "\t--dnavcf  \t\t\t\tFile path of DNA VCF file;\n" +
                "\t--darned  \t\t\t\tFile path of DARNED database;\n" +
                "\t--splice  \t\t\t\tFile path of annotation genes like \"gene.gft\";\n" +
                "\t--repeat  \t\t\t\tFile path of Repeat Masker database;\n" +
                "\t--dbsnp   \t\t\t\tFile path of dbSNP database;\n" +
                "\t-r, --rscript \t\t\tFile path of RScript.\n" +
                "\n" +
                "Example:\n" +
                "1) In Windows, use '--' patterns.\n" +
                "java -jar E:\\Workspace\\REFilters\\out\\artifacts\\REFilters\\REFilters_jdk1.6.0_43.jar ^\n" +
                "--host=127.0.0.1 ^\n" +
                "--port=3306 ^\n" +
                "--user=root ^\n" +
                "--pwd=123456 ^\n" +
                "--mode=denovo --input=D:\\Downloads\\Documents\\BJ22.snvs.hard.filtered.vcf,D:\\Downloads\\Documents\\hg19.txt,D:\\Downloads\\Documents\\genes.gtf,D:\\Downloads\\Documents\\hg19.fa.out,D:\\Downloads\\Documents\\dbsnp_138.hg19.vcf ^\n" +
                "--output=E:\\Workspace\\REFilters\\Results ^\n" +
                "--export=all ^\n" +
                "--rscript=C:\\R\\R-3.1.1\\bin\\Rscript.exe\n" +
                "\n" +
                "2) In Windows, use '-' patterns.\n" +
                "java -jar E:\\Workspace\\REFilters\\out\\artifacts\\REFilters\\REFilters_jdk1.6.0_43.jar ^\n" +
                "-H 127.0.0.1 ^\n" +
                "-p 3306 ^\n" +
                "-u root ^\n" +
                "-P 123456 ^\n" +
                "-m dnarna ^\n" +
                "-i D:\\Downloads\\Documents\\BJ22.snvs.hard.filtered.vcf,D:\\Downloads\\Documents\\BJ22_sites.hard.filtered.vcf,D:\\Downloads\\Documents\\hg19.txt,D:\\Downloads\\Documents\\genes.gtf,D:\\Downloads\\Documents\\hg19.fa.out,D:\\Downloads\\Documents\\dbsnp_138.hg19.vcf ^\n" +
                "-o E:\\Workspace\\REFilters\\Results ^\n" +
                "-e chrom,pos,level ^\n" +
                "-r C:\\R\\R-3.1.1\\bin\\Rscript.exe\n" +
                "\n" +
                "3) In CentOS, use '-' and '--' patterns.\n" +
                "java -jar /home/seq/softWare/RED/REFilter.jar \n" +
                "-h 127.0.0.1 \\\n" +
                "-p 3306 \\\n" +
                "-u seq \\\n" +
                "-P 123456 \\\n" +
                "-m denovo \\\n" +
                "--rnavcf=/data/rnaEditing/GM12878/GM12878.snvs.hard.filtered.vcf \\\n" +
                "--repeat=/home/seq/softWare/RED/hg19.fa.out \\\n" +
                "--splice=/home/seq/softWare/RED/genes.gtf \\\n" +
                "--dbsnp=/home/seq/softWare/RED/dbsnp_138.hg19.vcf \\\n" +
                "--darned=/home/seq/softWare/RED/hg19.txt \\\n" +
                "--rscript=/usr/bin/Rscript";
    }

    public static void exportData(File resultPath, String[] columns, String databaseName) throws IOException {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        List<String> currentTables;
        try {
            databaseManager.useDatabase(databaseName);
            currentTables = DatabaseManager.getInstance().getCurrentTables(databaseName);
        } catch (SQLException e) {
            throw new NullPointerException("Could not get the tables from database '" + databaseName + "'");
        }
        for (String currentTable : currentTables) {
            if (currentTable.contains(DatabaseManager.FET_FILTER_RESULT_TABLE_NAME)) {
                String sample = DatabaseManager.getInstance().getSampleName(currentTable);
                StringBuilder builder = new StringBuilder(sample);
                for (String column : columns) {
                    builder.append("_").append(column);
                }
                if (DatabaseManager.DENOVO_MODE_DATABASE_NAME.equals(databaseName)) {
                    builder.append("_denovo");
                } else {
                    builder.append("_dnarna");
                }
                System.out.println("Export data for : " + builder.toString());
                builder.append(".txt");
                File f = new File(resultPath + File.separator + builder.toString());
                PrintWriter pw = new PrintWriter(new FileWriter(f));
                //                    pw.println("pos");
                ResultSet rs;
                if (columns.length == 1 && columns[0].equalsIgnoreCase("all")) {
                    rs = databaseManager.query(currentTable, null, null, null);
                    List<String> columnNames;
                    try {
                        columnNames = databaseManager.getColumnNames(databaseName, currentTable);
                    } catch (SQLException e) {
                        throw new NullPointerException("Could not get the column names from table '" + currentTable + "'");
                    }

                    builder = new StringBuilder();
                    for (String column : columnNames) {
                        builder.append(column).append("\t");
                    }
                    pw.println(builder.toString().trim());

                    try {
                        while (rs.next()) {
                            builder = new StringBuilder();
                            for (String column : columnNames) {
                                builder.append(rs.getString(column)).append("\t");
                            }
                            pw.println(builder.toString().trim());
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else {
                    rs = databaseManager.query(currentTable, columns, null, null);
                    builder = new StringBuilder();
                    for (String column : columns) {
                        builder.append(column).append("\t");
                    }
                    pw.println(builder.toString().trim());

                    try {
                        while (rs.next()) {
                            builder = new StringBuilder();
                            for (String column : columns) {
                                builder.append(rs.getString(column)).append("\t");
                            }
                            pw.println(builder.toString().trim());
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                pw.flush();
                pw.close();
            }
        }

    }
}
