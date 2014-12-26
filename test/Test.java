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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import database.DatabaseManager;
import filter.Filter;
import filter.denovo.*;
import filter.dnarna.DNARNAFilter;
import filter.dnarna.LikelihoodRatioFilter;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Administrator on 2014/11/20.
 */
public class Test {
    private static DecimalFormat df = new DecimalFormat("#.###");

    public static void main(String[] args) throws SQLException, ClassNotFoundException, IOException, ParseException, JoranException {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        //        StatusPrinter.print(lc);
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(lc);
        lc.reset();
        configurator.doConfigure("src/config/LogbackConfig.xml");
        /**
         * Help content.
         */
        //                REFRunner.main(new String[]{"--help"});
        /**
         * DNA-RNA mode, use '--' pattern.
         */
        REFRunner.main(new String[]{
                "-H", "127.0.0.1",
                "-p", "3306",
                "-u", "root",
                "-P", "root",
                "-m", "dnarna",
                "-i", "E:\\Master\\ChongQing\\Data\\BJ22N_DNA_RNA\\BJ22.snvs.hard.filtered.vcf, " +
                        "E:\\Master\\ChongQing\\Data\\BJ22N_DNA_RNA\\BJ22_sites.hard.filtered.vcf, " +
                        "D:\\Downloads\\Documents\\hg19.txt, " +
                        "D:\\Downloads\\Documents\\genes.gtf, " +
                        "D:\\Downloads\\Documents\\hg19.fa.out, " +
                        "D:\\Downloads\\Documents\\dbsnp_138.hg19.vcf",
                "-r", "C:\\R\\R-3.1.1\\bin\\x64\\Rscript.exe",
                "-e", "all",
                "-o", "E:\\Workspace\\REFilters\\Results",
                "-O", "31265478"
        });

        /**
         * denovo mode, use '--' pattern.
         */
        //        REFRunner.main(new String[]{
        //                "--host=127.0.0.1", "--port=3306", "--user=root", "--pwd=root", "--mode=denovo", "--input=" +
        //                "E:\\Master\\ChongQing\\Data\\BJ22N_DNA_RNA\\BJ22.snvs.hard.filtered.vcf, " +
        //                "D:\\Downloads\\Documents\\hg19.txt," +
        //                "D:\\Downloads\\Documents\\genes.gtf, " +
        //                "D:\\Downloads\\Documents\\hg19.fa.out," +
        //                "D:\\Downloads\\Documents\\dbsnp_138.hg19.vcf", "-r", "C:\\R\\R-3.1.1\\bin\\x64\\Rscript.exe", "-e",
        //                "chrom,pos,level,alu", "-o", "E:\\Workspace\\REFilters\\Results"
        //        });

        /**
         * DNA-RNA mode, use '--' pattern.
         */
        //        REFRunner.main(new String[]{
        //                "-H", "127.0.0.1",
        //                "-p", "3306",
        //                "-u", "root",
        //                "-P", "root",
        //                "-m", "dnarna",
        //                "-r", "C:\\R\\R-3.1.1\\bin\\x64\\Rscript.exe",
        //                "-e", "annotation",
        //                "-o", "E:\\Workspace\\REFilters\\Results"
        //        });


        //        cal(new double[]{45819, 19856, 11123, 17126, 17116, 10816, 6418}, 252086);
        //        cal(new double[]{45819, 19856, 11123, 17126, 17116, 10816, 164, 157, 143}, 252086);
        //        cal(new double[]{109260, 22800, 5428, 17062, 17056, 6782, 3947}, 578942);
        //        cal(new double[]{109260, 22800, 5428, 17062, 17056, 6782, 26, 18, 12}, 578942);
        //        cal(new double[]{86413, 20776, 10850, 17642, 17627, 9588, 1437}, 413690);
        //        cal(new double[]{86413, 20776, 10850, 17642, 17627, 9588, 70, 68, 55}, 413690);
        //        cal(new double[]{55006, 11203, 3742, 9723, 9697, 4532, 2580}, 309260);
        //        cal(new double[]{55006, 11203, 3742, 9723, 9697, 4532, 63, 59, 58}, 309260);
    }

    public void cal(double[] filters, double total) {
        for (int i = 0; i < filters.length - 1; i++) {
            if (i == 0) {
                System.out.print("-" + df.format((total - filters[0]) / total * 100d) + "%,");
            } else if (i == 2) {
                continue;
            } else if (i == 3) {
                System.out.print("-" + df.format((filters[i - 2] - filters[i]) / total * 100d) + "%,");
            } else {
                System.out.print("-" + df.format((filters[i - 1] - filters[i]) / total * 100d) + "%,");
            }
        }
        System.out.print("+" + df.format(filters[filters.length - 1] / total * 100d) + "%");
        System.out.println();
    }

    public void sortFiltersTest() {
        DatabaseManager manager = DatabaseManager.getInstance();
        List<Filter> filters = new LinkedList<Filter>();
        filters.add(new EditingTypeFilter(manager));
        filters.add(new QualityControlFilter(manager));
        filters.add(new DNARNAFilter(manager));
        filters.add(new SpliceJunctionFilter(manager));
        filters.add(new RepeatRegionsFilter(manager));
        filters.add(new KnownSNPFilter(manager));
        filters.add(new LikelihoodRatioFilter(manager));
        filters.add(new FisherExactTestFilter(manager));
        REFRunner.sortFilters(filters, new int[]{1, 2, 3, 4, 5, 6, 7, 8});
        System.out.println(filters);
        REFRunner.sortFilters(filters, new int[]{2, 5, 1, 4, 3, 6, 7, 8});
        System.out.println(filters);
        REFRunner.sortFilters(filters, new int[]{2, 1, 3, 0, 5, 4, 0, 8});
        System.out.println(filters);
    }
}
