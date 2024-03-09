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
package nl.knaw.dans.datavaultcli.lib;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.core.Configuration;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.util.Generics;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import javax.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

public abstract class AbstractCommandLineApp<C extends Configuration> implements Callable<Integer> {
    public static String CONFIG_FILE_KEY = "dans.default.config";

    public void run(String[] args) throws IOException, ConfigurationException {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.OFF);
        File configFile = new File(System.getProperty(CONFIG_FILE_KEY));
        var config = loadConfiguration(configFile);
        var metricRegistry = new MetricRegistry();
        config.getLoggingFactory().configure(metricRegistry, getName());
        var commandLine = new CommandLine(this);
        configureCommandLine(commandLine, config);
        System.exit(commandLine.execute(args));
    }

    public abstract String getName();

    public abstract void configureCommandLine(CommandLine commandLine, C config);

    public Integer call() throws Exception {
        return 0;
    }

    private C loadConfiguration(File configFile) throws IOException, ConfigurationException {
        ObjectMapper objectMapper = Jackson.newObjectMapper(new YAMLFactory());
        Validator validator = Validators.newValidator();
        ConfigurationFactory<C> factory = new YamlConfigurationFactory<>(Generics.getTypeParameter(getClass(), Configuration.class), validator, objectMapper, "dw");
        return factory.build(configFile);
    }
}
