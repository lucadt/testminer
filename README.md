## TestMiner

TestMiner extracts test input values, such as strings, from a corpus of existing tests and suggests these values to a test generation tool. The approach is described in our ASE 2017 paper:

```
Saying ‘Hi!’ Is Not Enough: Mining Inputs for Effective Test Generation
Luca Della Toffola (ETH Zurich, Switzerland)
Cristian-Alexandru Staicu, Michael Pradel (TU Darmstadt, Germany)
```

This short tutorial shows how to build and use TestMiner.

## Dependencies 
The Maven Central index retrieval and the static analysis framework are written in Scala/Java.
We have two (almost) equivalent implementation of TestMiner, one written in Python, and another written in Java.
The minimum software requirements to run and compile TestMiner are:
- Java Virtual Machine (version 7 or greater)
- Python (version 2.7 or greater)
- Gradle (version 3.3 or greater)
- Scala (version 2.10.x or greater)

## Tutorial

The first three steps indicated below are optional. The three applications are used to prepare the context-value pairs
that TestMiner uses as index. However in the repository we provide two ready-to-use data-sets in the directory
`indexer/src/main/resources`:
- *full.json*: this file contains all the context-value pairs that we extracted from test-cases in 3600 projects we 
downloaded from Maven Central.
- *evaluation.json*: this file contains all the context-value pairs that we used in the evaluation. In this data-set
we removed all the method signatures that are prefixed with one of the class-under-test. The file *packages_to_filter.txt*
contains the prefixes that are filtered.
 
The snapshot date for the data is *19-02-2017*. If you desire to get obtain an updated set of strings you can execute 
three steps otherwise TestMiner is ready to be used (see step 4.).

### 1. Download Maven Central index (optional)

This application downloads the Maven Central *index*, but not the entire repository containing the Jar archives of the
source-code. The index is required to extract the list of projects that will be parsed later on.

```bash
> gradle -p analysis index -Dexec.args=/path/to/index
```

### 2. Download and analysis of projects source-code (optional)
This application downloads and analyzes the projects Jar archives with the source-code.
Only the Jar archive that use one of the libraries in `parser/src/main/resources/top100.csv`
are downloaded. This is an heuristic to reduce the number of projects that are 
downloaded and analyzed.

```bash
> gradle -p analysis parse -Dexec.args=/path/to/index,/path/to/parsed
```
In addition to the source-code the application also downloads the JavaDoc for a project.

***THE COMPLETION OF THIS OPERATION CAN TAKE TIME***

### 3. Process parsed source-code (optional)

To transform the analyzed source-code in a form ready to be indexed by TestMiner we provide the script `preprocess.py`.
To display all the command-line options use the command:
```bash
python preprocess.py --help
```
The output of the command is supposed to be:
```bash
usage: preprocess.py [-h] [--input INPUT] [--output OUTPUT] [--type TYPE]
                     [--filter] [--use-generics]

TestMiner dataset pre-processor

optional arguments:
  -h, --help       show this help message and exit
  --input INPUT    specifies the resulting directory with the parsed of data
  --output OUTPUT  specifies the directory where to save the elaborated data
  --type TYPE      specifies the primitive type to export (e.g., string -> only type supported)
  --filter         specifies to filter tuples that are part of testing set
  --use-generics   specifies to include generics of the method signature
```
The example command below executes the script with the results of the analysis for the source-code:

```bash
python preprocess.py --input /path/to/parsed --output /path/to/dataset.json
```
To filter the context-value tuples used in the evaluation use the option `--filter`.

### 4. Run TestMiner

We currently provide two ways for running TestMiner. First, a standalone bundle (`bundles/test-miner.jar`) that can be used for generating arbitrary strings for a given context. The main method to be used is `testminer.TestMiner.query(String)` that accepts a context and returns a set of string constants for that context. The second way to run TestMiner is to use the Randoop+TestMiner bundled version (`bundles/randoop-with-test-miner.jar`) that can be run as described by [the randoop manual](https://randoop.github.io/randoop/manual/).
For example, you can generate tests from the `sample\_project` directory in the following way (you need to first compile the ValidatorClass into the `bin` folder):
```
java -ea -classpath ./bin:../bundles/randoop-with-test-miner.jar  randoop.main.Main gentests --testclass=de.tu.darmstadt.sola.ValidatorClass  --junit-output-dir=tests  --junit-package-name=de.tu.darmstadt.sola  --timelimit=60
```


