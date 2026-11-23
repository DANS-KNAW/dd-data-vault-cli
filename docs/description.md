Description
===========

This package provides the `data-vault` command, to interact with the `dd-data-vault` service via its [REST API]{:target=_blank}.

[REST API]: https://dans-knaw.github.io/dd-data-vault/swagger-ui/

### OCFL objects

`data-vault -r ROOT ocfl list-objects` returns a JSON array of all ingested object IDs, fetching every page by default. `--blocked` selects blocked objects (including uningested objects), `--unblocked` selects unblocked ingested objects, and `-all` or `--all` includes uningested objects without filtering by status. These options are mutually exclusive. Supplying `--limit` or `--offset` returns only one page; the defaults are 100 and 0 respectively.

`data-vault -r ROOT ocfl is-object-blocked ID` prints the blocked status as a JSON boolean. `data-vault -r ROOT ocfl clear-object-block ID` clears the block and prints the resulting status. Either command accepts `-` or an omitted ID to read one ID from standard input. Errors are written to standard error and return a nonzero exit status.

These commands require a service implementing the `2.1.2-SNAPSHOT` API contract; the snapshot API artifact must be available to build the CLI.
