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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import nl.knaw.dans.datavaultcli.Context;
import nl.knaw.dans.datavaultcli.api.LayerStatusDto.StatusEnum;
import nl.knaw.dans.datavaultcli.client.ApiClient;
import nl.knaw.dans.datavaultcli.client.DefaultApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LayerGetStatusTest {
    private final PrintStream out = System.out;
    private final PrintStream err = System.err;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errors = new ByteArrayOutputStream();
    private final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    private HttpServer server;
    private DefaultApi api;
    private Context context;

    @BeforeEach
    void set_up() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/layers", exchange -> {
            var path = exchange.getRequestURI().getPath();
            var body = switch (path) {
                case "/layers/1" -> "{\"layerId\":1,\"status\":\"OPEN\",\"sizeInBytes\":null}";
                case "/layers/3" -> "{\"layerId\":3,\"status\":\"CLOSED\",\"sizeInBytes\":0}";
                case "/layers/top" -> "{\"layerId\":2,\"status\":\"ARCHIVED\",\"sizeInBytes\":42,\"message\":\"quote: \\\" and newline: \\n\"}";
                default -> "not found";
            };
            var bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(path.equals("/layers/1") || path.equals("/layers/3") || path.equals("/layers/top") ? 200 : 404, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        var client = new ApiClient();
        client.setBasePath("http://127.0.0.1:" + server.getAddress().getPort());
        api = new DefaultApi();
        api.setApiClient(client);
        context = mock(Context.class);
        when(context.getApi()).thenReturn(api);
        System.setOut(new PrintStream(output));
        System.setErr(new PrintStream(errors));
    }

    @AfterEach
    void tear_down() {
        System.setOut(out);
        System.setErr(err);
        server.stop(0);
    }

    @Test
    void generated_client_deserializes_and_roundtrips_http_responses() throws Exception {
        var open = api.layersIdGet(1L);
        assertThat(open.getStatus()).isEqualTo(StatusEnum.OPEN);
        assertThat(open.getSizeInBytes()).isNull();
        assertThat(open.getMessage()).isNull();
        var json = mapper.readTree(mapper.writeValueAsString(open));
        assertThat(json.has("sizeInBytes")).isTrue();
        assertThat(json.get("sizeInBytes").isNull()).isTrue();
        assertThat(json.has("message")).isFalse();

        var closed = api.layersIdGet(3L);
        assertThat(closed.getStatus()).isEqualTo(StatusEnum.CLOSED);
        assertThat(closed.getSizeInBytes()).isZero();

        var archived = api.layersTopGet();
        assertThat(archived.getStatus()).isEqualTo(StatusEnum.ARCHIVED);
        assertThat(archived.getSizeInBytes()).isEqualTo(42L);
        assertThat(archived.getMessage()).isEqualTo("quote: \" and newline: \n");
        assertThat(mapper.readTree(mapper.writeValueAsString(archived)).get("message").asText()).isEqualTo(archived.getMessage());
    }

    @Test
    void command_outputs_id_and_top_responses_as_json() throws Exception {
        assertThat(new CommandLine(new LayerGetStatus(context)).execute("1")).isZero();
        var json = mapper.readTree(output.toString());
        assertThat(json.get("layerId").asLong()).isEqualTo(1);
        assertThat(json.get("sizeInBytes").isNull()).isTrue();
        assertThat(json.has("message")).isFalse();
        output.reset();
        assertThat(new CommandLine(new LayerGetStatus(context)).execute("top")).isZero();
        json = mapper.readTree(output.toString());
        assertThat(json.get("status").asText()).isEqualTo("ARCHIVED");
        assertThat(json.get("sizeInBytes").asLong()).isEqualTo(42);
        assertThat(json.get("message").asText()).isEqualTo("quote: \" and newline: \n");
        assertThat(errors.toString()).isEmpty();
    }

    @Test
    void command_reports_invalid_id_and_http_error() {
        assertThat(new CommandLine(new LayerGetStatus(context)).execute("invalid")).isEqualTo(1);
        assertThat(errors.toString()).contains("layer id must be a number");
        errors.reset();
        assertThat(new CommandLine(new LayerGetStatus(context)).execute("99")).isEqualTo(1);
        assertThat(errors.toString()).contains("Error:");
        assertThat(output.toString()).isEmpty();
    }
}