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

### Object version properties

Object version properties are modifiable attributes of an OCFL object version, as defined by the [object-version-properties]{:target=_blank} extension. The
`object-property` commands get and change them through the service, which validates every change against the property registry of the storage root before
it is stored. The main use case is deaccessioning a dataset version.

`data-vault -r ROOT object-property get -i ID [-v VERSION] [NAME]` prints the value of the property as JSON on standard output. Without `NAME` it prints all
properties of the version. `VERSION` may be given as `1` or `v1` and defaults to `latest`.

`data-vault -r ROOT object-property set -i ID -v VERSION NAME VALUE` adds the property or replaces its value. Use `--from-file FILE` instead of `VALUE` to
read the value from a file; trailing whitespace, such as a final newline, is removed. A value is interpreted as follows:

* A value that starts with `{` or `[` is parsed as JSON. If it is not valid JSON, the command fails before anything is sent to the service.
* Any other value is taken as a string, so `1.0` is stored as the string `"1.0"`.
* With `--json` every value is parsed as JSON. Use this for numbers and booleans, and quote strings: `'"foo"'`.

`data-vault -r ROOT object-property set -f CSV_FILE` sets a batch of properties. The CSV file must have a header with the columns `OBJECT_ID`,
`OBJECT_VERSION_NR`, `PROPERTY` and `VALUE`. Values are interpreted as above; `--from-file` cannot be used in this mode. Each row is set separately: a row
that fails is reported on standard error and the remaining rows are still processed. The command returns a nonzero exit status if any row failed.

`data-vault -r ROOT object-property delete -i ID -v VERSION NAME` removes the property from the version.

The service refuses a change if the resulting properties of the version do not conform to the property registry: an unknown property, a value of the
wrong type, a missing required sub-property, or the removal of a required property. The reason is printed on standard error. Note that the whole version is
checked, so a version that already lacks a required property cannot be changed until that property is set. Properties are set per version: setting a
property on version 1 does not change later versions.

Example: deaccession version 1 of a dataset, and reaccession it again.

```bash
echo '{"datetime": "2026-08-04T14:37:52Z", "reason": "Invalid dataset" }' > details.txt
data-vault -r ROOT object-property set --identifier urn:nbn:nl:ui:13-xxx --version 1 deaccessioned --from-file details.txt
data-vault -r ROOT object-property delete --identifier urn:nbn:nl:ui:13-xxx --version 1 deaccessioned
```

!!! note "Values starting with `@`"

    A value that starts with `@` is taken literally: `@details.txt` sets the value to the string `@details.txt`, it does not read the file. Use
    `--from-file` to read a value from a file. (The `data-vault` command does not support picocli argument files.)

[object-version-properties]: https://dans-knaw.github.io/dans-ocfl-extensions/object-version-properties/object-version-properties/
