Installation
============

Currently, this project is built as an RPM package for RHEL8 compatible OSes and later. The RPM will install the binaries to
`/opt/dans.knaw.nl/dd-data-vault-cli` and the configuration files to `/etc/opt/dans.knaw.nl/dd-data-vault-cli`. The configuration options are documented by
comments in the default configuration file `config.yml`.

Building from source
--------------------

Prerequisites:

* Java 17 or higher
* Maven 3.6.3 or higher
* RPM

Steps:

```bash
git clone https://github.com/DANS-KNAW/dd-data-vault-cli.git
cd dd-data-vault-cli
mvn clean install
```
