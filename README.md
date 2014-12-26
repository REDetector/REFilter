
##What is REFilter?
REFilter is a command line tool for detecting RNA editing sites with rule-based filters and statistical filters.

## Table of contents

 - [Prerequisites](#prerequisites)
 - [How to use REFilter?](#how-to-use-refilter)
 - [Example](#example)
 - [Bugs and feature requests](#bugs-and-feature-requests)
 - [Creators](#creators)
 - [Copyright and license](#copyright-and-license)

##Prerequisites
Before running REFilter, the following software or programs are **demanded**:

- Java Runtime Environment (jdk 1.6.0_43 or later)
- MySQL Database Management System (MySQL 5.6.19 or later)
- R Environment (R 3.1.1 or later)

##How to use REFilter？
```java 
Usage: java -jar jarfile [-h|--help] [-v|--version] [-H|--host[=127.0.0.1]] [-p|--port[=3306]]
[-u|--user[=root]] [-P|--pwd[=root]] [-d|--database[=DNA_RNA_MODE]] [-m|--mode[=dnarna]] [-i|--input]
[-o|--output[=./]] [-e|--export[=all]] [-O|--order[=12345678]][--rnavcf] [--dnavcf] [--darned]
[--splice] [--repeat] [--dbsnp]
```

The most commonly used REFilters commands are:

    -h, --help                  Print short help message and exit;
	-v, --version               Print version info and exit;
	-H, --host=127.0.0.1        The host address of MySQL database;
	-p, --port=3306             The port used in MySQL;
	-u, --user=root             MySQL user name;
    -P, --pwd=root              MySQL password of user;
    -d, --database=DNA_RNA_MODE Database name;
	-m, --mode=dnarna           Tell the program if it is denovo mode or DNARNA mode [DENOVO|DNARNA];
    -r, --r=/usr/bin/RScript    File path of RScript or RExecutable.
	-i, --input                 Input all required files in order (i.e., RNA VCF File, DNA VCF File,
	                            DARNED Database, Gene Annotation File, RepeatMasker Database File,
	                            dbSNP Database File) instead of single input, each file should be
	                            divided with ',' and there should not be blank with each file;
    -o, --output=.              Set export path for the results in database, default path is current
                                directory;
    -e, --export=all            Export the needed columns in database, which must be the column name
                                of a table in database, the column names should be divided by ',';
    -O, --order=12345678        The order of performing the filter.
    --rnavcf                    File path of RNA VCF file;
    --dnavcf                    File path of DNA VCF file;
    --darned                    File path of DARNED database;
    --splice                    File path of annotation genes like "gene.gft";
    --repeat                    File path of Repeat Masker database;
    --dbsnp                     File path of dbSNP database;

##Explanation for order option
The default order for DNA-RNA mode is (1)editing type filter -> (2)quality control filter -> (3)DNA-RNA filter -> (4)splice junction filter -> (5)repeat
regions filter -> (6)known SNP filter -> (7)likelihood ratio test filter -> (8)fisher's exact test filter.

The default order for denovo mode is (1)editing type filter -> (2)quality control filter -> (3)splice junction filter -> (4)repeat regions filter -> (5)known
 SNP filter -> (6)fisher's exact test filter.

You can change the order by this option. For DNA-RNA mode, eight filters are available so that you could not enter less than 8 in the order (e.g., '2143657' is
illegal, '51432678' is legal). The same for denovo mode, six filters are available (e.g., '5214376' is illegal, '523516' is legal).

It is strongly recommended the FET filter place in the last of the order since it will affect the results by calculating the p-value and false discovery rate.

Besides, if there is any filter that you do not want to perform in the filter list, just replace the index number from the filter name to zero. For example,
in DNA-RNA mode, I do not want to perform known SNP filter and likelihood test filter, then the order should be '12345008'. You can change the filter order,
too.

##Example:
* 1) In Windows, use '--' patterns.

```java
java -jar E:\Workspace\REFilters\out\artifacts\REFilters\REFilters_jdk1.6.0_43.jar ^
--host=127.0.0.1 ^
--port=3306 ^
--user=root ^
--pwd=123456 ^
--mode=denovo ^
--input=D:\Downloads\Documents\BJ22.snvs.hard.filtered.vcf, D:\Downloads\Documents\hg19.txt, D:\Downloads\Documents\genes.gtf, D:\Downloads\Documents\hg19.fa
.out, D:\Downloads\Documents\dbsnp_138.hg19.vcf ^
--output=E:\Workspace\REFilters\Results ^
--export=all ^
--r=C:\R\R-3.1.1\bin\Rscript.exe
```

* 2) In Windows, use '-' patterns.

```java
java -jar E:\Workspace\REFilters\out\artifacts\REFilters\REFilters_jdk1.6.0_43.jar ^
-H 127.0.0.1 ^
-p 3306 ^
-u root ^
-P 123456 ^
-d BJ22_DNA_RNA ^
-m dnarna ^
-i D:\Downloads\Documents\BJ22.snvs.hard.filtered.vcf, D:\Downloads\Documents\BJ22_sites.hard.filtered.vcf, D:\Downloads\Documents\hg19.txt,
D:\Downloads\Documents\genes.gtf, D:\Downloads\Documents\hg19.fa.out, D:\Downloads\Documents\dbsnp_138.hg19.vcf ^
-o E:\Workspace\REFilters\Results ^
-e chrom,pos,level ^
-r C:\R\R-3.1.1\bin\Rscript.exe
```

* 3) In CentOS, use '-' and '--' patterns.

```java
java -jar /home/seq/softWare/RED/REFilter.jar 
-h 127.0.0.1 \
-p 3306 \
-u seq \
-P 123456 \
-m denovo \
--rnavcf=/data/rnaEditing/GM12878/GM12878.snvs.hard.filtered.vcf \
--repeat=/home/seq/softWare/RED/hg19.fa.out \
--splice=/home/seq/softWare/RED/genes.gtf \
--dbsnp=/home/seq/softWare/RED/dbsnp_138.hg19.vcf \
--darned=/home/seq/softWare/RED/hg19.txt \
--r=/usr/bin/Rscript
```

## Bugs and feature requests

Have a bug or a feature request? Please first read the [issue guidelines](https://github.com/iluhcm/REFilter/issues) and search for existing and closed issues
. If your problem or idea is not addressed yet, please open a [new issue](https://github.com//iluhcm/REFilter/issues/new).

## Creators

**Xing Li**

- <https://github.com/iluhcm>
- email: <sam.lxing@gmail.com>

## Copyright and license

Code and documentation copyright 2013-2014 BUPT/CQMU. 

REFilter is a free software, and you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
Foundation; either version 3 of the License, or (at your option) any later version .