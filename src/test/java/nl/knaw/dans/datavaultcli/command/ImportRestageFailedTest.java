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

import nl.knaw.dans.datavaultcli.Context;
import nl.knaw.dans.datavaultcli.DataVaultCli;
import nl.knaw.dans.datavaultcli.api.RestageFailedRequestDto;
import nl.knaw.dans.datavaultcli.client.ApiException;
import nl.knaw.dans.datavaultcli.client.DefaultApi;
import nl.knaw.dans.datavaultcli.config.DataVaultConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ImportRestageFailedTest {
    private final Context context = mock(Context.class);
    private final DefaultApi api = mock(DefaultApi.class);
    private final PrintStream out = System.out;
    private final PrintStream err = System.err;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errors = new ByteArrayOutputStream();

    @BeforeEach
    void set_up() {
        when(context.getApi()).thenReturn(api);
        System.setOut(new PrintStream(output));
        System.setErr(new PrintStream(errors));
    }

    @AfterEach
    void tear_down() {
        System.setOut(out);
        System.setErr(err);
    }

    @Test
    void forwards_selectors_unchanged() throws Exception {
        for (var selector : List.of("a725716f-7c06-4ce8-9424-440aa36d52a5", "./a725716f-7c06-4ce8-9424-440aa36d52a5", "batch/../batch", "/inbox/batch")) {
            assertThat(new CommandLine(new ImportRestageFailed(context)).execute(selector)).isZero();
            verify(api).importsRestageFailedPost(new RestageFailedRequestDto().selector(selector));
        }
        assertThat(output.toString()).isEmpty();
        assertThat(errors.toString()).contains("Failed imports restaged.");
    }

    @Test
    void api_failure_returns_nonzero_and_reports_to_stderr() throws Exception {
        doThrow(new ApiException("bad selector")).when(api).importsRestageFailedPost(new RestageFailedRequestDto().selector("bad"));
        assertThat(new CommandLine(new ImportRestageFailed(context)).execute("bad")).isEqualTo(1);
        assertThat(output.toString()).isEmpty();
        assertThat(errors.toString()).contains("bad selector");
    }

    @Test
    void missing_selector_is_rejected() {
        assertThat(new CommandLine(new ImportRestageFailed(context)).execute()).isNotZero();
        verifyNoInteractions(api);
    }

    @Test
    void command_is_registered_under_import() {
        var cli = new DataVaultCli();
        var config = new DataVaultConfiguration();
        config.setStorageRoots(Map.of());
        var commandLine = new CommandLine(cli);
        cli.configureCommandLine(commandLine, config);
        var parsed = commandLine.parseArgs("-r", "root", "import", "restage-failed", "./batch");
        assertThat(parsed.subcommand().subcommand().commandSpec().name()).isEqualTo("restage-failed");
    }
}