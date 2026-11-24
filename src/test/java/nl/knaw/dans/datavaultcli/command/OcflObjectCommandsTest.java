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
import com.sun.net.httpserver.HttpServer;
import nl.knaw.dans.datavaultcli.Context;
import nl.knaw.dans.datavaultcli.client.ApiClient;
import nl.knaw.dans.datavaultcli.client.ApiException;
import nl.knaw.dans.datavaultcli.client.OcflApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OcflObjectCommandsTest {
    private final Context context = mock(Context.class);
    private final OcflApi api = mock(OcflApi.class);
    private final PrintStream out = System.out;
    private final PrintStream err = System.err;
    private final InputStream in = System.in;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errors = new ByteArrayOutputStream();

    @BeforeEach
    void set_up() {
        when(context.getOcflApi()).thenReturn(api);
        when(context.getObjectMapper()).thenReturn(new ObjectMapper());
        System.setOut(new PrintStream(output));
        System.setErr(new PrintStream(errors));
    }

    @AfterEach
    void tear_down() {
        System.setOut(out);
        System.setErr(err);
        System.setIn(in);
    }

    @Test
    void list_fetches_all_pages_by_default() throws Exception {
        when(api.ocflObjectsGet(100, 0, null, null)).thenReturn(Collections.nCopies(100, "first"));
        when(api.ocflObjectsGet(100, 100, null, null)).thenReturn(List.of("last"));
        assertThat(new CommandLine(new OcflListObjects(context)).execute()).isZero();
        assertThat(new ObjectMapper().readTree(output.toString()).size()).isEqualTo(101);
        verify(api).ocflObjectsGet(100, 100, null, null);
    }

    @Test
    void explicit_pagination_and_filters_are_forwarded() throws Exception {
        when(api.ocflObjectsGet(5, 3, true, false)).thenReturn(List.of("id"));
        assertThat(new CommandLine(new OcflListObjects(context)).execute("--blocked", "--uningested", "--limit", "5", "--offset", "3")).isZero();
        verify(api).ocflObjectsGet(5, 3, true, false);
        verifyNoMoreInteractions(api);
    }

    @Test
    void filter_combinations_are_forwarded() throws Exception {
        String[] blockedOptions = { null, "--blocked", "--unblocked" };
        Boolean[] blockedValues = { null, true, false };
        String[] ingestedOptions = { null, "--ingested", "--uningested" };
        Boolean[] ingestedValues = { null, true, false };
        for (int i = 0; i < blockedOptions.length; i++) {
            for (int j = 0; j < ingestedOptions.length; j++) {
                var args = new ArrayList<String>();
                if (blockedOptions[i] != null) args.add(blockedOptions[i]);
                if (ingestedOptions[j] != null) args.add(ingestedOptions[j]);
                assertThat(new CommandLine(new OcflListObjects(context)).execute(args.toArray(new String[0]))).isZero();
                verify(api).ocflObjectsGet(100, 0, blockedValues[i], ingestedValues[j]);
            }
        }
        verifyNoMoreInteractions(api);
    }

    @Test
    void conflicting_filters_removed_all_and_invalid_limit_fail() {
        assertThat(new CommandLine(new OcflListObjects(context)).execute("--blocked", "--unblocked")).isNotZero();
        assertThat(new CommandLine(new OcflListObjects(context)).execute("--ingested", "--uningested")).isNotZero();
        assertThat(new CommandLine(new OcflListObjects(context)).execute("--all")).isNotZero();
        assertThat(new CommandLine(new OcflListObjects(context)).execute("-all")).isNotZero();
        assertThat(new CommandLine(new OcflListObjects(context)).execute("--limit", "0")).isNotZero();
        verifyNoInteractions(api);
    }

    @Test
    void block_commands_accept_argument_and_stdin() throws Exception {
        when(api.ocflObjectsIdBlockedGet("id1")).thenReturn(true);
        when(api.ocflObjectsIdBlockedGet("id3")).thenReturn(false);
        assertThat(new CommandLine(new OcflIsObjectBlocked(context)).execute("id1")).isZero();
        assertThat(new CommandLine(new OcflIsObjectBlocked(context)).execute("id3")).isZero();
        System.setIn(new ByteArrayInputStream("id2\n".getBytes(StandardCharsets.UTF_8)));
        assertThat(new CommandLine(new OcflClearObjectBlock(context)).execute("-")).isZero();
        assertThat(new CommandLine(new OcflClearObjectBlock(context)).execute("id4")).isZero();
        assertThat(output.toString()).isEqualTo("true\nfalse\n");
        assertThat(errors.toString()).isEqualTo("Object is unblocked: id2\nObject is unblocked: id4\n");
        verify(api).ocflObjectsIdBlockedDelete("id2");
        verify(api).ocflObjectsIdBlockedDelete("id4");
    }

    @Test
    void generated_client_accepts_no_content_response() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ocfl/objects/id/blocked", exchange -> {
            if (exchange.getRequestMethod().equals("DELETE")) {
                exchange.sendResponseHeaders(204, -1);
            }
            else {
                exchange.sendResponseHeaders(405, -1);
            }
            exchange.close();
        });
        server.start();
        try {
            var client = new ApiClient();
            client.setBasePath("http://127.0.0.1:" + server.getAddress().getPort());
            var realApi = new OcflApi();
            realApi.setApiClient(client);
            when(context.getOcflApi()).thenReturn(realApi);
            assertThat(new CommandLine(new OcflClearObjectBlock(context)).execute("id")).isZero();
            assertThat(output.toString()).isEmpty();
            assertThat(errors.toString()).isEqualTo("Object is unblocked: id\n");
        }
        finally {
            server.stop(0);
        }
    }

    @Test
    void failures_go_to_stderr_and_return_nonzero() throws Exception {
        when(api.ocflObjectsIdBlockedGet("missing")).thenThrow(new ApiException("not found"));
        assertThat(new CommandLine(new OcflIsObjectBlocked(context)).execute("missing")).isEqualTo(1);
        assertThat(output.toString()).isEmpty();
        assertThat(errors.toString()).contains("not found");
        System.setIn(new ByteArrayInputStream(new byte[0]));
        assertThat(new CommandLine(new OcflClearObjectBlock(context)).execute()).isEqualTo(1);
        assertThat(errors.toString()).contains("Object ID is required");
        doThrow(new ApiException("clear failed")).when(api).ocflObjectsIdBlockedDelete("missing");
        assertThat(new CommandLine(new OcflClearObjectBlock(context)).execute("missing")).isEqualTo(1);
        assertThat(output.toString()).isEmpty();
        assertThat(errors.toString()).contains("clear failed").doesNotContain("Object is unblocked");
    }
}