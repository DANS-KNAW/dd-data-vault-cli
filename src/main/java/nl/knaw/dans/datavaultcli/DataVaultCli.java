/*
 * Copyright (C) 2022 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.datavaultcli;

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.datavaultcli.client.ApiClient;
import nl.knaw.dans.datavaultcli.client.DefaultApi;
import nl.knaw.dans.datavaultcli.command.ConsistencyCheck;
import nl.knaw.dans.datavaultcli.command.ConsistencyCheckNew;
import nl.knaw.dans.datavaultcli.command.CopyBatch;
import nl.knaw.dans.datavaultcli.command.Import;
import nl.knaw.dans.datavaultcli.command.ImportStart;
import nl.knaw.dans.datavaultcli.command.ImportStatus;
import nl.knaw.dans.datavaultcli.command.Layer;
import nl.knaw.dans.datavaultcli.command.LayerNew;
import nl.knaw.dans.datavaultcli.config.DataVaultConfiguration;
import nl.knaw.dans.datavaultcli.config.ImportAreaConfig;
import nl.knaw.dans.datavaultcli.config.StorageRootConfig;
import nl.knaw.dans.lib.util.AbstractCommandLineApp;
import nl.knaw.dans.lib.util.ClientProxyBuilder;
import nl.knaw.dans.lib.util.PicocliVersionProvider;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.Map;
import java.util.stream.Collectors;

@Command(name = "data-vault",
         mixinStandardHelpOptions = true,
         versionProvider = PicocliVersionProvider.class,
         description = "Manage one or more Data Vault instances")
@Slf4j
public class DataVaultCli extends AbstractCommandLineApp<DataVaultConfiguration> {
    public static void main(String[] args) throws Exception {
        new DataVaultCli().run(args);
    }

    public String getName() {
        return "Data Vault CLI";
    }

    @Override
    public void configureCommandLine(CommandLine commandLine, DataVaultConfiguration config) {
        log.debug("Configuring command line");
        var storageRootEndPoints = getStorageRootEndPoints(config.getStorageRoots());
        var importAreaConfigs = getImportAreaConfigs(config.getStorageRoots());
        commandLine
            .addSubcommand(new CommandLine(new Import(storageRootEndPoints))
                .addSubcommand(new ImportStart())
                .addSubcommand(new ImportStatus()))
            .addSubcommand(new CommandLine(new Layer(storageRootEndPoints))
                .addSubcommand(new LayerNew())
                .addSubcommand(new CopyBatch(importAreaConfigs)))
            .addSubcommand(new CommandLine(new ConsistencyCheck(storageRootEndPoints))
                .addSubcommand(new ConsistencyCheckNew()));
    }

    private Map<String, DefaultApi> getStorageRootEndPoints(Map<String, StorageRootConfig> storageRoots) {
        return storageRoots.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> new ClientProxyBuilder<ApiClient, DefaultApi>()
                .apiClient(new ApiClient())
                .basePath(e.getValue().getDataVaultService().getUrl())
                .httpClient(e.getValue().getDataVaultService().getHttpClient())
                .defaultApiCtor(DefaultApi::new)
                .build()));
    }

    private Map<String, ImportAreaConfig> getImportAreaConfigs(Map<String, StorageRootConfig> storageRoots) {
        return storageRoots
            .entrySet()
            .stream()
            .map(e -> Map.entry(e.getKey(), e.getValue().getImportArea()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
