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

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.dans.datavaultcli.Context;
import nl.knaw.dans.datavaultcli.DataVaultCli;
import nl.knaw.dans.datavaultcli.api.ImportJobStatusDto;
import nl.knaw.dans.datavaultcli.api.ImportJobStatusDto.StatusEnum;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ImportListTest {
    private final Context context = mock(Context.class);
    private final DefaultApi api = mock(DefaultApi.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final PrintStream out = System.out;
    private final PrintStream err = System.err;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errors = new ByteArrayOutputStream();

    @BeforeEach
    void set_up() {
        when(context.getApi()).thenReturn(api);
        when(context.getObjectMapper()).thenReturn(mapper);
        System.setOut(new PrintStream(output));
        System.setErr(new PrintStream(errors));
    }

    @AfterEach
    void tear_down() {
        System.setOut(out);
        System.setErr(err);
    }

    @Test
    void list_returns_all_attributes_and_forwards_filters() throws Exception {
        var id = UUID.randomUUID();
        var job = new ImportJobStatusDto().id(id).path("batch").status(StatusEnum.FAILED).message("reason").singleObject(true);
        when(api.importsGet(List.of("RUNNING", "FAILED"), 10, 2, null)).thenReturn(List.of(job));
        assertThat(new CommandLine(new ImportList(context)).execute("--failed", "--running", "--limit", "10", "--offset", "2")).isZero();
        var json = mapper.readTree(output.toString()).get(0);
        assertThat(json.get("id").asText()).isEqualTo(id.toString());
        assertThat(json.get("path").asText()).isEqualTo("batch");
        assertThat(json.get("message").asText()).isEqualTo("reason");
        assertThat(json.get("singleObject").asBoolean()).isTrue();
        assertThat(json.has("created")).isTrue();
        assertThat(errors.toString()).isEmpty();
    }

    @Test
    void batch_search_short_output_contains_only_projection() throws Exception {
        var id = UUID.randomUUID();
        when(api.importsGet(null, null, null, "some/batch")).thenReturn(List.of(new ImportJobStatusDto().id(id).path("some/batch").status(StatusEnum.SUCCESS).message("hidden")));
        assertThat(new CommandLine(new ImportFindByBatch(context)).execute("--short", "some/batch")).isZero();
        var json = mapper.readTree(output.toString()).get(0);
        assertThat(json.size()).isEqualTo(3);
        assertThat(json.get("uuid").asText()).isEqualTo(id.toString());
        assertThat(json.get("batch").asText()).isEqualTo("some/batch");
        assertThat(json.get("status").asText()).isEqualTo("SUCCESS");
    }

    @Test
    void list_short_output_combines_all_status_flags() throws Exception {
        when(api.importsGet(List.of("PENDING", "RUNNING", "SUCCESS", "FAILED"), null, null, null))
            .thenReturn(List.of(new ImportJobStatusDto().id(UUID.randomUUID()).path("batch").status(StatusEnum.PENDING)));
        assertThat(new CommandLine(new ImportList(context)).execute("--short", "--pending", "--running", "--success", "--failed")).isZero();
        var json = mapper.readTree(output.toString()).get(0);
        assertThat(json.size()).isEqualTo(3);
        assertThat(json.has("uuid") && json.has("batch") && json.has("status")).isTrue();
    }

    @Test
    void batch_search_default_output_contains_all_attributes() throws Exception {
        when(api.importsGet(null, null, null, "batch")).thenReturn(List.of(new ImportJobStatusDto().path("batch")));
        assertThat(new CommandLine(new ImportFindByBatch(context)).execute("batch")).isZero();
        var json = mapper.readTree(output.toString()).get(0);
        assertThat(json.size()).isEqualTo(8);
        assertThat(json.has("id") && json.has("path") && json.has("singleObject") && json.has("status")
            && json.has("created") && json.has("started") && json.has("finished") && json.has("message")).isTrue();
    }

    @Test
    void default_list_and_empty_result() throws Exception {
        when(api.importsGet(null, null, null, null)).thenReturn(List.of());
        assertThat(new CommandLine(new ImportList(context)).execute()).isZero();
        assertThat(mapper.readTree(output.toString()).isEmpty()).isTrue();
    }

    @Test
    void batch_search_forwards_inherited_filters() throws Exception {
        when(api.importsGet(List.of("PENDING"), 4, 0, "/inbox/batch")).thenReturn(List.of());
        assertThat(new CommandLine(new ImportFindByBatch(context)).execute("/inbox/batch", "--pending", "--limit", "4", "--offset", "0")).isZero();
        verify(api).importsGet(List.of("PENDING"), 4, 0, "/inbox/batch");
    }

    @Test
    void commands_are_registered_under_import() {
        var cli = new DataVaultCli();
        var config = new DataVaultConfiguration();
        config.setStorageRoots(Map.of());
        var commandLine = new CommandLine(cli);
        cli.configureCommandLine(commandLine, config);
        assertThat(commandLine.getSubcommands().get("import").getSubcommands())
            .containsKeys("list", "find-by-batch", "status");
    }

    @Test
    void invalid_arguments_are_rejected_before_api_call() {
        for (var args : List.of(new String[] { "--limit", "0" }, new String[] { "--limit", "-1" },
            new String[] { "--offset", "1" }, new String[] { "--limit", "1", "--offset", "-1" },
            new String[] { "--unknown" })) {
            assertThat(new CommandLine(new ImportList(context)).execute(args)).isNotZero();
        }
        assertThat(new CommandLine(new ImportFindByBatch(context)).execute()).isNotZero();
        assertThat(new CommandLine(new ImportFindByBatch(context)).execute(" ")).isNotZero();
        assertThat(output.toString()).isEmpty();
        assertThat(errors.toString()).isNotEmpty();
        verifyNoInteractions(api);
    }

    @Test
    void api_failure_is_reported_on_stderr() throws Exception {
        when(api.importsGet(null, null, null, "batch")).thenThrow(new ApiException("bad batch"));
        assertThat(new CommandLine(new ImportFindByBatch(context)).execute("batch")).isEqualTo(1);
        assertThat(output.toString()).isEmpty();
        assertThat(errors.toString()).contains("bad batch");
    }

    @Test
    void status_all_uses_unfiltered_request() throws Exception {
        when(api.importsGet(null, null, null, null)).thenReturn(List.of());
        assertThat(new CommandLine(new ImportStatus(context)).execute("--all")).isZero();
        verify(api).importsGet(null, null, null, null);
        assertThat(errors.toString()).contains("[");
    }
}