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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.sun.net.httpserver.HttpServer;
import nl.knaw.dans.datavaultcli.Context;
import nl.knaw.dans.datavaultcli.client.ApiClient;
import nl.knaw.dans.datavaultcli.client.ApiException;
import nl.knaw.dans.datavaultcli.client.ObjectVersionPropertiesApi;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ObjectPropertyCommandsTest {
    private final Path testDir = Path.of("target/test").resolve(getClass().getSimpleName());
    private final ObjectMapper mapper = new ObjectMapper();
    private final Context context = mock(Context.class);
    private final ObjectVersionPropertiesApi api = mock(ObjectVersionPropertiesApi.class);
    private final PrintStream out = System.out;
    private final PrintStream err = System.err;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errors = new ByteArrayOutputStream();

    @BeforeEach
    void set_up() throws Exception {
        FileUtils.deleteDirectory(testDir.toFile());
        Files.createDirectories(testDir);
        when(context.getObjectVersionPropertiesApi()).thenReturn(api);
        when(context.getObjectMapper()).thenReturn(mapper);
        System.setOut(new PrintStream(output));
        System.setErr(new PrintStream(errors));
    }

    @AfterEach
    void tear_down() {
        System.setOut(out);
        System.setErr(err);
    }

    private int set(String... args) {
        // As in DataVaultCli, arguments starting with '@' are not expanded as argument files
        return new CommandLine(new ObjectPropertySet(context)).setExpandAtFiles(false).execute(args);
    }

    @Test
    void help_option_works_on_all_subcommands() {
        for (var command : List.<Object> of(new ObjectPropertyGet(context), new ObjectPropertySet(context), new ObjectPropertyDelete(context))) {
            output.reset();
            assertThat(new CommandLine(command).execute("--help")).isZero();
            assertThat(new CommandLine(command).execute("-h")).isZero();
            assertThat(output.toString()).startsWith("Usage: ").contains("--version=<version>");
        }
        assertThat(errors.toString()).isEmpty();
        verifyNoInteractions(api);
    }

    @Test
    void get_prints_single_property_as_json() throws Exception {
        when(api.objectVersionPropertiesIdNrNameGet("id", "1", "dataset-version")).thenReturn("1.0");

        assertThat(new CommandLine(new ObjectPropertyGet(context)).execute("-i", "id", "-v", "1", "dataset-version")).isZero();

        assertThat(output.toString().strip()).isEqualTo("\"1.0\"");
    }

    @Test
    void get_without_name_prints_all_properties_of_latest_version() throws Exception {
        when(api.objectVersionPropertiesIdNrGet("id", "latest")).thenReturn(Map.of("packaging-format", "x"));

        assertThat(new CommandLine(new ObjectPropertyGet(context)).execute("--identifier", "id")).isZero();

        assertThat(mapper.readTree(output.toString()).get("packaging-format").asText()).isEqualTo("x");
    }

    @Test
    void get_reports_server_reason_on_stderr() throws Exception {
        when(api.objectVersionPropertiesIdNrNameGet("id", "1", "p"))
            .thenThrow(new ApiException(404, "Not Found", Map.of(), "Property 'p' not found"));

        assertThat(new CommandLine(new ObjectPropertyGet(context)).execute("-i", "id", "-v", "1", "p")).isEqualTo(1);

        assertThat(output.toString()).isEmpty();
        assertThat(errors.toString()).contains("HTTP 404: Property 'p' not found");
    }

    @Test
    void set_sends_string_or_json_value() throws Exception {
        assertThat(set("-i", "id", "-v", "1", "dataset-version", "1.0")).isZero();
        assertThat(set("-i", "id", "-v", "1", "deaccessioned", "{\"reason\": \"Invalid dataset\"}")).isZero();

        verify(api).objectVersionPropertiesIdNrNamePut("id", "1", "dataset-version", new TextNode("1.0"));
        verify(api).objectVersionPropertiesIdNrNamePut("id", "1", "deaccessioned", mapper.readTree("{\"reason\": \"Invalid dataset\"}"));
        assertThat(output.toString()).isEmpty();
    }

    @Test
    void set_reads_value_from_file() throws Exception {
        var details = testDir.resolve("details.txt");
        Files.writeString(details, "{\"datetime\": \"2026-08-04T14:37:52Z\", \"reason\": \"Invalid dataset\" }\n");

        assertThat(set("--identifier", "urn:nbn:x", "--version", "1", "deaccessioned", "--from-file", details.toString())).isZero();

        verify(api).objectVersionPropertiesIdNrNamePut("urn:nbn:x", "1", "deaccessioned",
            mapper.readTree("{\"datetime\": \"2026-08-04T14:37:52Z\", \"reason\": \"Invalid dataset\"}"));
    }

    @Test
    void set_takes_value_starting_with_at_sign_literally_even_if_file_exists() throws Exception {
        var file = testDir.resolve("home");
        Files.writeString(file, "contents");

        assertThat(set("-i", "id", "-v", "1", "p", "@" + file)).isZero();

        verify(api).objectVersionPropertiesIdNrNamePut("id", "1", "p", new TextNode("@" + file));
    }

    @Test
    void set_rejects_invalid_json_without_calling_server() {
        assertThat(set("-i", "id", "-v", "1", "deaccessioned", "{\"reason\": ")).isEqualTo(1);

        assertThat(errors.toString()).contains("Value is not valid JSON");
        verifyNoInteractions(api);
    }

    @Test
    void set_requires_either_input_file_or_all_single_arguments() throws Exception {
        var csv = testDir.resolve("input.csv");
        Files.writeString(csv, "OBJECT_ID,OBJECT_VERSION_NR,PROPERTY,VALUE\n");

        assertThat(set()).isEqualTo(2);
        assertThat(set("-i", "id", "-v", "1", "p")).isEqualTo(2);
        assertThat(set("-f", csv.toString(), "-i", "id")).isEqualTo(2);
        assertThat(set("-f", csv.toString(), "--from-file", csv.toString())).isEqualTo(2);
        assertThat(set("-i", "id", "-v", "1", "p", "x", "--from-file", csv.toString())).isEqualTo(2);
        verifyNoInteractions(api);
    }

    @Test
    void set_from_csv_continues_after_failure_and_returns_nonzero() throws Exception {
        var csv = testDir.resolve("input.csv");
        Files.writeString(csv, """
            OBJECT_ID,OBJECT_VERSION_NR,PROPERTY,VALUE
            id1,1,deaccessioned,"{""reason"": ""Invalid dataset""}"
            id2,v2,dataset-version,1.0
            id3,1,deaccessioned,"{""reason"": "
            id4,1,no-such-property,x
            """);
        doThrow(new ApiException(400, "Bad Request", Map.of(), "Unknown property per property-registry: no-such-property"))
            .when(api).objectVersionPropertiesIdNrNamePut(eq("id4"), anyString(), anyString(), any());

        assertThat(set("--input-file", csv.toString())).isEqualTo(1);

        verify(api).objectVersionPropertiesIdNrNamePut("id1", "1", "deaccessioned", mapper.readTree("{\"reason\": \"Invalid dataset\"}"));
        verify(api).objectVersionPropertiesIdNrNamePut("id2", "v2", "dataset-version", new TextNode("1.0"));
        verify(api, never()).objectVersionPropertiesIdNrNamePut(eq("id3"), anyString(), anyString(), any());
        assertThat(errors.toString())
            .contains("Error in line 4: Value is not valid JSON")
            .contains("HTTP 400: Unknown property per property-registry: no-such-property")
            .contains("2 properties could not be set");
    }

    @Test
    void set_from_csv_rejects_missing_columns() throws Exception {
        var csv = testDir.resolve("input.csv");
        Files.writeString(csv, "OBJECT_ID,PROPERTY,VALUE\nid,p,x\n");

        assertThat(set("-f", csv.toString())).isEqualTo(1);

        assertThat(errors.toString()).contains("missing column(s)").contains("OBJECT_VERSION_NR");
        verifyNoInteractions(api);
    }

    @Test
    void delete_calls_server() throws Exception {
        assertThat(new CommandLine(new ObjectPropertyDelete(context)).execute("-i", "id", "-v", "1", "deaccessioned")).isZero();

        verify(api).objectVersionPropertiesIdNrNameDelete("id", "1", "deaccessioned");
        assertThat(errors.toString()).contains("Deleted property deaccessioned");
    }

    @Test
    void generated_client_sends_string_value_as_json_string() throws Exception {
        var requests = new ArrayList<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/object-version-properties/", exchange -> {
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getRawPath() + " "
                + exchange.getRequestHeaders().getFirst("Content-Type") + " "
                + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        try {
            var client = new ApiClient();
            client.setBasePath("http://127.0.0.1:" + server.getAddress().getPort());
            when(context.getObjectVersionPropertiesApi()).thenReturn(new ObjectVersionPropertiesApi(client));

            assertThat(set("-i", "urn:nbn:x", "-v", "1", "dataset-version", "1.0")).isZero();
            assertThat(set("-i", "urn:nbn:x", "-v", "1", "deaccessioned", "{\"reason\": \"r\"}")).isZero();
        }
        finally {
            server.stop(0);
        }
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0)).startsWith("PUT /object-version-properties/urn%3Anbn%3Ax/1/dataset-version application/json");
        assertThat(bodyOf(requests.get(0))).isEqualTo(new TextNode("1.0"));
        assertThat(bodyOf(requests.get(1))).isEqualTo(mapper.readTree("{\"reason\": \"r\"}"));
    }

    private JsonNode bodyOf(String request) throws Exception {
        return mapper.readTree(request.substring(request.indexOf(' ', request.indexOf("application/json")) + 1));
    }
}
