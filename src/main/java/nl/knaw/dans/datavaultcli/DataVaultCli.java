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

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.core.setup.Environment;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.datavaultcli.client.ApiClient;
import nl.knaw.dans.datavaultcli.client.DefaultApi;
import nl.knaw.dans.datavaultcli.config.DataVaultConfiguration;
import nl.knaw.dans.datavaultcli.subcommand.CopyBatch;
import nl.knaw.dans.datavaultcli.subcommand.Import;
import nl.knaw.dans.datavaultcli.subcommand.ImportStart;
import nl.knaw.dans.datavaultcli.subcommand.ImportStatus;
import nl.knaw.dans.datavaultcli.subcommand.Layer;
import nl.knaw.dans.datavaultcli.subcommand.LayerNew;
import nl.knaw.dans.lib.util.AbstractCommandLineApp;
import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "data-vault",
         mixinStandardHelpOptions = true,
         versionProvider = VersionProvider.class,
         description = "Manage a Data Vault.")
@AllArgsConstructor
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
        DefaultApi api = createDefaultApi(config);
        log.debug("Configuring command line");
        commandLine
            .addSubcommand(new CommandLine(new Import())
                .addSubcommand(new ImportStart(api))
                .addSubcommand(new ImportStatus(api)))
            .addSubcommand(new CommandLine(new Layer())
                .addSubcommand(new LayerNew(api)))
            .addSubcommand(new CopyBatch(config.getImportArea()))
            .addSubcommand(new GenerateCompletion());
    }

    private static DefaultApi createDefaultApi(DataVaultConfiguration configuration) {
        log.debug("Creating API client");
        var client = new JerseyClientBuilder(new Environment(DataVaultCli.class.getName()))
            .using(configuration.getDataVaultService().getHttpClient())
            .build(DataVaultCli.class.getName() + " client");
        var apiClient = new ApiClient();
        // End-slashes trip up the API client, so we remove them from the base path.
        apiClient.setBasePath(configuration.getDataVaultService().getUrl().toString().replaceAll("/+$", ""));
        apiClient.setHttpClient(client);
        return new DefaultApi(apiClient);
    }
}
