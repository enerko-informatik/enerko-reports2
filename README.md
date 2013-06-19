# ENERKOs Report Engine

## Abstract

ENERKOs Report Engine is a [Apache POI-HSSF][1] based reporting engine that runs inside the JVM of an Oracle Datebase and creates Excel reports through an PL/SQL interface.

It is designed to provide an unified reporting engine for

* Oracle Forms 6i based applications
* Java SE clients as well as
* Webapplications with different architectures.

It fits into 2-tier applications as well as into 3-tier applications as reports are stored at a central point (the database) as 

* PL/SQL functions
* Queries or
* views.

It consists of 2 parts:

* The implementation in Java
* An PL/SQL api to access the Java Stored Procedures

ENERKOs Report Engine can also run client side but this is not recommended as installation base will be duplicated.

Reports can be created from scratch or can be based on other Excel sheets named templates. Cell formating, formulas, macros and diagrams will be preserved. When opened in Excel, those diagrams and macros will be updated using the actual values.

## Installation

The following describes the installation inside a database with the fictive user 'hre'.

The report engine should be installed into every scheme where it will be used.

The user needs the following privileges

	GRANT CREATE ANY PROCEDURE TO "HRE";
	GRANT CREATE ANY TABLE TO "HRE";
	GRANT CREATE PROCEDURE TO "HRE";
	GRANT CREATE TABLE TO "HRE";

Quotas must be adapted or unlimited tablespace be granted:

	GRANT UNLIMITED TABLESPACE TO "HRE"

To actually load the java source files the following permission is needed as well:

	call dbms_java.grant_permission('HRE', 'SYS:oracle.aurora.security.JServerPermission', 'loadLibraryInClass.*', null);
		
I use [loadjava.bat][3] to load the java source files which is part of the Oracle Client package (Oracle InstantClient won't be enough). Alternatively [dbms_java.loadjava][4] can be used.

First, load the required dependencies:

	loadjava.bat -user hre/hre@database -resolve lib/commons-codec-1.5.jar
	loadjava.bat -user hre/hre@database -resolve lib/poi-3.9.jar
	
then load ENERKOs Report Engine:

	loadjava.bat -user hre/hre@database -resolve target/enerko-reports2-0.0.1-SNAPSHOT.jar

Then you need some SQL packages:


    # A type that mimics a varg list
	sqlplus hre/hre@database < src/main/sql/t_vargs.sql
	
	# The type that represents a cell definition
	sqlplus hre/hre@database < src/main/sql/t_er_cell_definition.sql 
	# and the list thereof
	sqlplus hre/hre@database < src/main/sql/table_of_er_cell_definitions.sql 
	
	# And certainly the PL/SQL api:
	sqlplus hre/hre@database < src/main/sql/pck_enerko_reports2.pks.sql
	sqlplus hre/hre@database < src/main/sql/pck_enerko_reports2.pkb.sql
	

## Usage

The BLOBs created by ENERKOs Report Engine can be used in many possible ways, the can be access by Java based webapplications through JDBC, by Forms etc.

The following examples assume a writable database directory called "enerko_reports" that is writable by the ENERKOs Report Engine user. To create this directory, grant the HRE user the following privileges: 

	GRANT CREATE ANY DIRECTORY TO "HRE"
	
and create the directory like this:

	CREATE DIRECTORY enerko_reports AS '/var/tmp/enerko_reports';
	
Read more about LOB handling [DBMS_LOB][5]. pck_enerko_reports2 contains  [UTL_FILE][6] based procedures / functions to store LOBs into files and read files into LOBs.

All reports presented are part of this project, have a look at src/test/sql/pck_enerko_reports2_test.sql.
	
### Create a report using a simple statement

#### Without templates

	DECLARE
		v_report BLOB;
	BEGIN
		-- Create the report
		v_report := pck_enerko_reports2.f_create_report_from_statement('Select ''s1'' as sheetname, 1 as cell_column, 1 as cell_row, ''c1'' as cell_name, ''string'' as cell_type, ''cv'' as cell_value from dual');
		
		-- Store it into a server side file
		pck_enerko_reports2.p_blob_to_file(v_report, 'enerko_reports', 'example1.xls');
	END;
	/
	
#### With templates

The template can be any blob containing a valid Excel sheet. The examples accesses a template inside a server side directory but any blob column will do.

	DECLARE
		v_template BLOB;
		v_report BLOB;
	BEGIN
		v_template := pck_enerko_reports2.f_file_to_blob('enerko_reports', 'template1.xls');

		-- Create the report
		v_report := pck_enerko_reports2.f_create_report_from_statement(
			'Select ''f_fb_report_source_test'' as sheetname, 0 as cell_column, 0 as cell_row, null as cell_name, ''string'' as cell_type, ''Hello, World'' as cell_value from dual',
			v_template
		);
	
		-- Store it into a server side file
		pck_enerko_reports2.p_blob_to_file(v_report, 'enerko_reports', 'example2.xls');
	END;
	/
	
### Create a report using pipelined functions

#### Without templates

Pipelined functions are a very nice and handy feature of the Oracle database. Basically those are functions that can act as table in the from clause:

> Table functions are functions that produce a collection of rows (either a nested table or a varray) that can be queried like a physical database table. You use a table function like the name of a database table, in the FROM clause of a query.

From [Using Pipelined and Parallel Table Functions][7]

At ENERKO pipelined functions are main report source. Data can be selected in a PL/SQL method using standard SQL statements and than be arranged in arbitrary ways, incrementally building a report. Thus any developer with some SQL knowledge can create complex reports without overly complex queries or Java knowledge at all.

The test resources contain some very simple pipelined function based reports.

Here is a an example to call this from PL/SQL. Notice the lack of vargs in PL/SQL and the record type of strings:

	DECLARE
		v_report BLOB;
	BEGIN
		-- Create the report
		v_report := pck_enerko_reports2.f_create_report(
			'pck_enerko_reports2_test.f_fb_report_source_test',
			t_vargs('10', '21.09.1979', 'Some label')
		);
	
		-- Store it into a server side file
		pck_enerko_reports2.p_blob_to_file(v_report, 'enerko_reports', 'example3.xls');
		
		-- Create a report without arguments
		v_report := pck_enerko_reports2.f_create_report('pck_enerko_reports2_test.f_noarg_report');
		pck_enerko_reports2.p_blob_to_file(v_report, 'enerko_reports', 'example4.xls');
	END;
	/
	
#### With templates	

Reports based on pipelined functions works also with templates:

	DECLARE
		v_template BLOB;
		v_report BLOB;
	BEGIN
		v_template := pck_enerko_reports2.f_file_to_blob('enerko_reports', 'template1.xls');

		-- Create the report
		v_report := pck_enerko_reports2.f_create_report(
			'pck_enerko_reports2_test.f_fb_report_source_test',
			v_template,
			t_vargs('10', '21.09.1979', 'Some label')
		);
	
		-- Store it into a server side file
		pck_enerko_reports2.p_blob_to_file(v_report, 'enerko_reports', 'example5.xls');
		
		-- Create a report without arguments
		v_report := pck_enerko_reports2.f_create_report('pck_enerko_reports2_test.f_noarg_report', v_template);
		pck_enerko_reports2.p_blob_to_file(v_report, 'enerko_reports', 'example6.xls');
	END;
	/
	
### API

The main structure for creating reports is the cell definition t_er_cell_definition:

	SQL> desc t_er_cell_definition
	  Name                                      Null?    Typ
	  ----------------------------------------- -------- ----------------------------
	  SHEETNAME                                          VARCHAR2(512)
	  CELL_COLUMN                                        NUMBER(38)
	  CELL_ROW                                           NUMBER(38)
	  CELL_NAME                                          VARCHAR2(64)
	  CELL_TYPE                                          VARCHAR2(512)
	  CELL_VALUE                                         VARCHAR2(32767)

| Attribute      | Meanung                                                         |
|----------------|-----------------------------------------------------------------|
| SHEETNAME      | Name of the worksheet                                           |
| CELL_COLUMN    | 0-based column index                                            |
| CELL_ROW       | 0-based row index                                               |
| CELL_NAME      | Cell reference, only used for output, can be null               |
| CELL_TYPE      | Valid datatype (see "Supported datatypes for cell definitions") |
| CELL_VALUE     | String representation of the concrete cell                      |
		
Reports can be created either using SQL statements returning the columns mentioned here or pipelined functions returning this type in the pipe. Thus, all cells can be filled in arbitrary order.		
		
### Options and formatting

The pck_enerko_reports2_test.f_all_features shows pretty much all features:

	DECLARE
		v_template BLOB;
		v_report BLOB;
	BEGIN
		v_template := pck_enerko_reports2.f_file_to_blob('enerko_reports', 'template2.xls');
		v_report := pck_enerko_reports2.f_create_report('pck_enerko_reports2_test.f_all_features', v_template);

		pck_enerko_reports2.p_blob_to_file(v_report, 'enerko_reports', 'example7.xls');
	END;
	/
	
The following features are available and the following formatting is possible:

#### Supported datatypes for function arguments

| name (as in PL/SQL type name) | format                                  |
|:-------------------------------|:---------------------------------------|
| varchar2                      | free format                             |
| number                        | with nls_numeric_characters set to '.,' |
| date                          | DD.MM.YYYY                              |
| timestamp                     | DD.MM.YYYY HH24:MI'                     |

The arguments are passed as varchar2s to the report engine so the format must be valid. Otherwise the report generation will fail with a ParseException.

#### Supported datatypes for cell definitions

| Name     | format                                  |
|:---------|:----------------------------------------|
| string   | free format                             |
| number   | with nls_numeric_characters set to '.,' |
| date     | DD.MM.YYYY                              |
| datetime | DD.MM.YYYY HH24:MI'                     |
| formula  | i.e. SUM(B23:B42)                       |

Numeric values can add a format specification according to ["Including decimal places and significant digits"][8] by separating it with a double '@@' from the value:

	42.23@@#0.000
	
Formulas are noted without the leading '=', cell reference is the known excel notation A1 … Zn. 

#### Format templates / reference cells

ENERKOs Report Engine supports templates not only for formatting but also as a reference. The user can create an (invisible) sheet with formatting templates for dates, numbers etc.

Those references can be addressed through the data type with the following form:

	datetime; "reference_sheet" B1
	date; "reference_sheet" B2
	number; "reference_sheet" A1
	
Or abstract:
	
	datatype; "Name of the worksheet" CELLREFERENCE
	
Any non empty, formatted cell at the reference will be used to format the current cell.

This can be used for user defined date formats for example.

[1]: http://poi.apache.org/spreadsheet/
[2]: http://docs.oracle.com/cd/E11882_01/java.112/e10588/toc.htm
[3]: http://docs.oracle.com/cd/E11882_01/java.112/e10588/cheleven.htm#JJDEV10060
[4]: http://docs.oracle.com/cd/E11882_01/java.112/e10588/appendixa.htm#JJDEV13000
[5]: http://docs.oracle.com/cd/E11882_01/appdev.112/e10577/d_lob.htm
[6]: http://docs.oracle.com/cd/E11882_01/appdev.112/e25788/u_file.htm#ARPLS70896
[7]: http://docs.oracle.com/cd/E11882_01/appdev.112/e10765/pipe_paral_tbl.htm
[8]: http://office.microsoft.com/en-us/excel-help/number-format-codes-HP005198679.aspx