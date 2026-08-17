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
package nl.knaw.dans.datavaultcli.command;

import io.dropwizard.client.JerseyClientConfiguration;
import nl.knaw.dans.datavaultcli.Context;
import nl.knaw.dans.datavaultcli.DataVaultCli;
import nl.knaw.dans.datavaultcli.client.ApiException;
import nl.knaw.dans.datavaultcli.client.DefaultApi;
import nl.knaw.dans.datavaultcli.config.DataVaultConfiguration;
import nl.knaw.dans.datavaultcli.config.DataVaultServiceConfig;
import nl.knaw.dans.datavaultcli.config.ImportAreaConfig;
import nl.knaw.dans.datavaultcli.config.StorageRootConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportCleanupTest {
    private final Context context = Mockito.mock(Context.class);
    private final DefaultApi defaultApi = Mockito.mock(DefaultApi.class);
    private final PrintStream standardOut = System.out;
    private final PrintStream standardErr = System.err;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        when(context.getApi()).thenReturn(defaultApi);
        System.setOut(new PrintStream(outputStreamCaptor));
        System.setErr(new PrintStream(errorStreamCaptor));
    }

    @AfterEach
    void tearDown() {
        System.setOut(standardOut);
        System.setErr(standardErr);
    }

    @Test
    void call_should_call_imports_cleanup_post_and_return_0() throws Exception {
        var command = new ImportCleanup(context);
        var result = command.call();

        assertThat(result).isEqualTo(0);
        verify(defaultApi).importsCleanupPost();
        assertThat(errorStreamCaptor.toString()).contains("Import cleanup completed.");
    }

    @Test
    void call_should_return_1_and_print_error_when_api_exception_occurs() throws Exception {
        doThrow(new ApiException("API error occurred")).when(defaultApi).importsCleanupPost();

        var command = new ImportCleanup(context);
        var result = command.call();

        assertThat(result).isEqualTo(1);
        verify(defaultApi).importsCleanupPost();
        assertThat(errorStreamCaptor.toString()).contains("Error: API error occurred");
    }

    @Test
    void cli_execution_should_parse_import_cleanup_subcommand() {
        var cli = new DataVaultCli();
        var cmd = new CommandLine(cli);
        var config = new DataVaultConfiguration();
        var storageRootConfig = new StorageRootConfig();
        var serviceConfig = new DataVaultServiceConfig();
        serviceConfig.setUrl(URI.create("http://localhost:20365/"));
        serviceConfig.setHttpClient(new JerseyClientConfiguration());
        storageRootConfig.setDataVaultService(serviceConfig);
        storageRootConfig.setImportArea(new ImportAreaConfig());
        config.setStorageRoots(Map.of("root1", storageRootConfig));

        cli.configureCommandLine(cmd, config);

        var parseResult = cmd.parseArgs("-r", "root1", "import", "cleanup");
        assertThat(parseResult.subcommand().subcommand().commandSpec().name()).isEqualTo("cleanup");
    }
}
