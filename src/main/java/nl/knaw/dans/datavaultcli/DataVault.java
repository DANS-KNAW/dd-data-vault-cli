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
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import lombok.AllArgsConstructor;
import nl.knaw.dans.datavaultcli.client.ApiClient;
import nl.knaw.dans.datavaultcli.client.DefaultApi;
import nl.knaw.dans.datavaultcli.config.DataVaultConfiguration;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import javax.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

@Command(name = "data-vault",
         mixinStandardHelpOptions = true,
         versionProvider = VersionProvider.class,
         description = "Manage a Data Vault.")
@AllArgsConstructor
public class DataVault implements Callable<Integer> {
    private final DataVaultConfiguration configuration;

    @Override
    public Integer call() throws Exception {
        // The base command does nothing, it only serves as a container for subcommands.
        return 0;
    }

    public static void main(String[] args) throws IOException, ConfigurationException {
        File configFile = new File(System.getProperty("dans.default.config"));
        DataVaultConfiguration config = loadConfiguration(configFile);
        DefaultApi api = createDefaultApi(config);

        var commandLine = new CommandLine(new DataVault(config));
        commandLine.addSubcommand(new StartJob(config, api));
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    private static DefaultApi createDefaultApi(DataVaultConfiguration configuration) {
        var client = new JerseyClientBuilder(new Environment(DataVault.class.getName()))
            .using(configuration.getDataVaultService().getHttpClient())
            .build(DataVault.class.getName() + " client");
        var apiClient = new ApiClient();
        // End-slashes trip up the API client, so we remove them from the base path.
        apiClient.setBasePath(configuration.getDataVaultService().getUrl().toString().replaceAll("/+$", ""));
        apiClient.setHttpClient(client);
        return new DefaultApi(apiClient);
    }

    private static DataVaultConfiguration loadConfiguration(File configFile) throws IOException, ConfigurationException {
        ObjectMapper objectMapper = Jackson.newObjectMapper(new YAMLFactory());
        Validator validator = Validators.newValidator();
        ConfigurationFactory<DataVaultConfiguration> factory = new YamlConfigurationFactory<>(DataVaultConfiguration.class, validator, objectMapper, "dw");
        return factory.build(configFile);
    }
}
