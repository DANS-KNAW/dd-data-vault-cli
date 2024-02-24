data-vault
==========
Command line tool to interact with the `dd-data-vault` service.

SYNOPSIS
--------

```shell
data-vault import start path/to/batch
data-vault import status
data-vault layer new
```

DESCRIPTION
-----------
This package provides the `data-vault` command, to interact with the `dd-data-vault` service via its [REST API]{:target=_blank}.

[REST API]: https://dans-knaw.github.io/dd-data-vault/swagger-ui/

INSTALLATION AND CONFIGURATION
------------------------------
Currently, this project is built as an RPM package for RHEL8 compatible OSes and later. The RPM will install the binaries to
`/opt/dans.knaw.nl/dd-data-vault-cli` and the configuration files to `/etc/opt/dans.knaw.nl/dd-data-vault-cli`. The configuration options are documented by
comments in the default configuration file `config.yml`.

To install the module on systems that do not support RPM, you can copy and unarchive the tarball to the target host. You will have to take care of placing the
files in the correct locations for your system yourself. For instructions on building the tarball, see next section.

BUILDING FROM SOURCE
--------------------
Prerequisites:

* Java 17 or higher
* Maven 3.6.3 or higher
* RPM

Steps:

```shell 
git clone https://github.com/DANS-KNAW/dd-data-vault-cli.git
cd dd-data-vault-cli
mvn clean install
```

If the `rpm` executable is found at `/usr/local/bin/rpm`, the build profile that includes the RPM packaging will be activated. If `rpm` is available, but at a
different path, then activate it by using Maven's `-P` switch: `mvn -Pprm install`.

Alternatively, to build the tarball execute:

```bash
mvn clean install assembly:single
```

