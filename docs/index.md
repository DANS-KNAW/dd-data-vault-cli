dd-data-vault-cli
=================

Command line tool to interact with the `dd-data-vault` service.

SYNOPSIS
--------

```bash
data-vault -r <storageRoot> import start [ -s | --single-object ] <path>
data-vault -r <storageRoot> import status { <id> | -a | --all }
data-vault -r <storageRoot> layer new
data-vault -r <storageRoot> layer list-ids
data-vault -r <storageRoot> layer status { <ID> | top }
data-vault -r <storageRoot> layer archive <ID>
data-vault -r <storageRoot> itemstore create-directory <path>
data-vault -r <storageRoot> itemstore delete-directory <path>
data-vault -r <storageRoot> itemstore delete-file <path>
data-vault -r <storageRoot> itemstore copy-directory-into <source> <destination>
data-vault -r <storageRoot> itemstore copy-file-to <source> <destination>
data-vault -r <storageRoot> copy-batch <source> <target>
data-vault -r <storageRoot> consistency-check new
data-vault -r <storageRoot> consistency-check get <id>
```

For more information on a subcommand use:

```bash
data-vault <subcommand> --help
```

