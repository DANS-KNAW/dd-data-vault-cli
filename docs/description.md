Description
===========

This package provides the `data-vault` command, to interact with the `dd-data-vault` service via its [REST API]{:target=_blank}.

[REST API]: https://dans-knaw.github.io/dd-data-vault/swagger-ui/

### Import jobs

`data-vault -r ROOT import list` returns a JSON array of import jobs. Combine `--pending`, `--running`, `--success`, and `--failed` to select statuses; without flags all statuses are included. Use a positive `--limit` and optionally a nonnegative `--offset` to select a page; offset requires limit and defaults to zero. Results are ordered by creation time and job ID.

`data-vault -r ROOT import find-by-batch BATCH` finds jobs for the exact batch path. The path may be relative to the server's configured `dataVault.ingest.inbox` or absolute within that inbox; paths outside it are rejected. Path normalization is lexical and does not resolve symlinks.

Both commands output all job attributes by default. Use `--short` for objects containing only `uuid`, `batch`, and `status`. Empty results are `[]`; errors go to standard error and return a nonzero exit status.

### OCFL objects

`data-vault -r ROOT ocfl list-objects` returns a JSON array of object IDs, fetching every page by default. By default it includes both blocked and unblocked, and both ingested and uningested objects. Use `--blocked` or `--unblocked` to filter by block status, and independently use `--ingested` or `--uningested` to filter by ingestion status. The options within each pair are mutually exclusive. Supplying `--limit` or `--offset` returns only one page; the defaults are 100 and 0 respectively.

`data-vault -r ROOT ocfl is-object-blocked ID` prints the blocked status as a JSON boolean on standard output. `data-vault -r ROOT ocfl clear-object-block ID` clears the block and writes `Object is unblocked: ID` to standard error, with no standard output. Either command accepts `-` or an omitted ID to read one ID from standard input. Errors are written to standard error and return a nonzero exit status.

This changes the clear command's former `false` standard output; scripts must use its exit status instead. The service now returns `204` without a body instead of `200` with JSON `false`, so API clients must be updated or regenerated and deployed with the compatible service.

These commands require a service implementing the `2.1.2-SNAPSHOT` API contract; the snapshot API artifact must be available to build the CLI.

### Restaging failed imports

Run `data-vault -r <root> import restage-failed <selector>` to restore failed object directories in the inbox without starting a new import job. The selector is an import job UUID or a batch path relative to or contained in the inbox. Prefix a UUID-shaped batch name with `./` to select it as a path. Missing or empty failed directories are no-ops; filesystem failures can leave a batch partially restaged. Messages and errors are written to stderr.
