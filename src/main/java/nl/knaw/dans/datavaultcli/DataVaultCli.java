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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.datavaultcli.client.ApiClient;
import nl.knaw.dans.datavaultcli.client.DefaultApi;
import nl.knaw.dans.datavaultcli.command.ConsistencyCheck;
import nl.knaw.dans.datavaultcli.command.ConsistencyCheckGet;
import nl.knaw.dans.datavaultcli.command.ConsistencyCheckNew;
import nl.knaw.dans.datavaultcli.command.CopyBatch;
import nl.knaw.dans.datavaultcli.command.Import;
import nl.knaw.dans.datavaultcli.command.ImportStart;
import nl.knaw.dans.datavaultcli.command.ImportStatus;
import nl.knaw.dans.datavaultcli.command.Layer;
import nl.knaw.dans.datavaultcli.command.LayerArchive;
import nl.knaw.dans.datavaultcli.command.LayerGetIds;
import nl.knaw.dans.datavaultcli.command.LayerGetStatus;
import nl.knaw.dans.datavaultcli.command.LayerNew;
import nl.knaw.dans.datavaultcli.config.DataVaultConfiguration;
import nl.knaw.dans.datavaultcli.config.ImportAreaConfig;
import nl.knaw.dans.datavaultcli.config.StorageRootConfig;
import nl.knaw.dans.lib.util.AbstractCommandLineApp;
import nl.knaw.dans.lib.util.ClientProxyBuilder;
import nl.knaw.dans.lib.util.PicocliVersionProvider;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Map;
import java.util.stream.Collectors;

@Command(name = "data-vault",
         mixinStandardHelpOptions = true,
         versionProvider = PicocliVersionProvider.class,
         description = "Manage one or more Data Vault instances")
@Slf4j
public class DataVaultCli extends AbstractCommandLineApp<DataVaultConfiguration> implements Context {
    public static void main(String[] args) throws Exception {
        new DataVaultCli().run(args);
    }

    @Getter
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getName() {
        return "Data Vault CLI";
    }

    private Map<String, DefaultApi> storageRootsEndPoints;

    private Map<String, ImportAreaConfig> importAreaConfigs;

    @Option(names = { "-r", "--storage-root" },
            description = "The storage root to execute the command on.",
            required = true)
    private String storageRoot;

    @Override
    public DefaultApi getApi() {
        if (storageRootsEndPoints == null) {
            throw new IllegalStateException("getApi() called before initialization.");
        }

        var api = storageRootsEndPoints.get(this.storageRoot);
        if (api == null) {
            System.err.println("No storage root found for " + this.storageRoot);
            throw new IllegalArgumentException("No storage root found for " + this.storageRoot);
        }
        return api;
    }

    @Override
    public ImportAreaConfig getImportAreaConfig() {
        if (importAreaConfigs == null) {
            throw new IllegalStateException("getImportAreaConfig() called before initialization.");
        }

        var config = importAreaConfigs.get(this.storageRoot);
        if (config == null) {
            System.err.println("No import area found for storage root " + this.storageRoot);
            throw new IllegalArgumentException("No import area found for storage root " + this.storageRoot);
        }
        return config;
    }

    @Override
    public void configureCommandLine(CommandLine commandLine, DataVaultConfiguration config) {
        log.debug("Configuring command line");
        fillStorageRootEndPoints(config.getStorageRoots());
        fillImportAreaConfigs(config.getStorageRoots());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        commandLine
            .addSubcommand(new CommandLine(new Import())
                .addSubcommand(new ImportStart(this))
                .addSubcommand(new ImportStatus(this)))
            .addSubcommand(new CommandLine(new Layer())
                .addSubcommand(new LayerNew(this))
                .addSubcommand(new LayerGetIds(this))
                .addSubcommand(new LayerGetStatus(this))
                .addSubcommand(new LayerArchive(this)))
            .addSubcommand(new CopyBatch(this))
            .addSubcommand(new CommandLine(new ConsistencyCheck())
                .addSubcommand(new ConsistencyCheckNew(this))
                .addSubcommand(new ConsistencyCheckGet(this)));
    }

    private void fillStorageRootEndPoints(Map<String, StorageRootConfig> storageRoots) {
        storageRootsEndPoints = storageRoots.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> new ClientProxyBuilder<ApiClient, DefaultApi>()
                .apiClient(new ApiClient())
                .basePath(e.getValue().getDataVaultService().getUrl())
                .httpClient(e.getValue().getDataVaultService().getHttpClient())
                .defaultApiCtor(DefaultApi::new)
                .build()));
    }

    private void fillImportAreaConfigs(Map<String, StorageRootConfig> storageRoots) {
        importAreaConfigs = storageRoots
            .entrySet()
            .stream()
            .map(e -> Map.entry(e.getKey(), e.getValue().getImportArea()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
