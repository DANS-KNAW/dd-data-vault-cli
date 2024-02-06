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
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import lombok.AllArgsConstructor;
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
        // Create a client
        DefaultApi api = new DefaultApi();
        return 0;
    }

    public static void main(String[] args) throws IOException, ConfigurationException {
        ObjectMapper objectMapper = Jackson.newObjectMapper(new YAMLFactory());
        Validator validator = Validators.newValidator();
        ConfigurationFactory<DataVaultConfiguration> factory = new YamlConfigurationFactory<>(DataVaultConfiguration.class, validator, objectMapper, "dw");
        File configFile = new File(System.getProperty("dans.default.config"));
        DataVaultConfiguration config = factory.build(configFile);
        var commandLine = new CommandLine(new DataVault(config));
        commandLine.addSubcommand(new StartJob(config));
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }
}
