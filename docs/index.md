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

