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
import nl.knaw.dans.datavaultcli.command.CopyBatch;
import nl.knaw.dans.datavaultcli.command.Import;
import nl.knaw.dans.datavaultcli.command.ImportStart;
import nl.knaw.dans.datavaultcli.command.ImportStatus;
import nl.knaw.dans.datavaultcli.command.Layer;
import nl.knaw.dans.datavaultcli.command.LayerNew;
import nl.knaw.dans.datavaultcli.config.DataVaultConfiguration;
import nl.knaw.dans.lib.util.AbstractCommandLineApp;
import nl.knaw.dans.lib.util.ClientProxyBuilder;
import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "data-vault",
         mixinStandardHelpOptions = true,
         versionProvider = VersionProvider.class,
         description = "Manage a Data Vault.")
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
        DefaultApi api = new ClientProxyBuilder<ApiClient, DefaultApi>()
            .apiClient(new ApiClient())
            .basePath(config.getDataVaultService().getUrl())
            .httpClient(config.getDataVaultService().getHttpClient())
            .defaultApiCtor(DefaultApi::new)
            .build();
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
}
