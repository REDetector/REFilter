
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
[-u|--user[=root]] [-P|--pwd[=root]] [-m|--mode[=dnarna]] [-i|--input] [-o|--output[=./]] 
[-e|--export[=all]] [--rnavcf] [--dnavcf] [--darned] [--splice] [--repeat] [--dbsnp]
```

The most commonly used REFilters commands are:

    -h, --help              Print short help message and exit;
	-v, --version           Print version info and exit;
	-H, --host=127.0.0.1    The host address of MySQL database;
	-p, --port=3306         The port used in MySQL;
	-u, --user=root         MySQL user name;
    -P, --pwd=root          MySQL password of user;
	-m, --mode=dnarna       Tell the program if it is denovo mode or DNARNA mode;
	-i, --input             Input all required files in order (i.e., RNA VCF File, DNA VCF File,
	                        DARNED Database, Gene Annotation File, RepeatMasker Database File,
	                        dbSNP Database File) instead of single input, each file should be
	                        divided with ',' and there should not be blank with each file;
    -o, --output=./         Set export path for the results in database, default path is current
                            directory;
    -e, --export=all        Export the needed columns in database, which must be the column name 
                            of a table in database, the column names should be divided by ',';
    --rnavcf                File path of RNA VCF file;
    --dnavcf                File path of DNA VCF file;
    --darned                File path of DARNED database;
    --splice                File path of annotation genes like "gene.gft";
    --repeat                File path of Repeat Masker database;
    --dbsnp                 File path of dbSNP database;
    -r, --rscript           File path of RScript.
    
##Example:
* 1) In Windows, use '--' patterns.

```java
java -jar E:\Workspace\REFilters\out\artifacts\REFilters\REFilters_jdk1.6.0_43.jar ^
--host=127.0.0.1 ^
--port=3306 ^
--user=root ^
--pwd=123456 ^
--mode=denovo --input=D:\Downloads\Documents\BJ22.snvs.hard.filtered.vcf,D:\Downloads\Documents\hg19.txt,D:\Downloads\Documents\genes.gtf,D:\Downloads\Documents\hg19.fa.out,D:\Downloads\Documents\dbsnp_138.hg19.vcf ^
--output=E:\Workspace\REFilters\Results ^
--export=all ^
--rscript=C:\R\R-3.1.1\bin\Rscript.exe
```

* 2) In Windows, use '-' patterns.

```java
java -jar E:\Workspace\REFilters\out\artifacts\REFilters\REFilters_jdk1.6.0_43.jar ^
-H 127.0.0.1 ^
-p 3306 ^
-u root ^
-P 123456 ^
-m dnarna ^
-i D:\Downloads\Documents\BJ22.snvs.hard.filtered.vcf,D:\Downloads\Documents\BJ22_sites.hard.filtered.vcf,D:\Downloads\Documents\hg19.txt,D:\Downloads\Documents\genes.gtf,D:\Downloads\Documents\hg19.fa.out,D:\Downloads\Documents\dbsnp_138.hg19.vcf ^
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
--rscript=/usr/bin/Rscript
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