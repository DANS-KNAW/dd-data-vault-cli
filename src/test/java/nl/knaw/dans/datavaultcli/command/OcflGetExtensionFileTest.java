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
import nl.knaw.dans.datavaultcli.client.OcflApi;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class OcflGetExtensionFileTest {
    private final Context context = Mockito.mock(Context.class);
    private final OcflApi ocflApi = Mockito.mock(OcflApi.class);
    private final PrintStream standardOut = System.out;
    private final PrintStream standardErr = System.err;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        when(context.getOcflApi()).thenReturn(ocflApi);
        System.setOut(new PrintStream(outputStreamCaptor));
        System.setErr(new PrintStream(errorStreamCaptor));
    }

    @AfterEach
    void tearDown() {
        System.setOut(standardOut);
        System.setErr(standardErr);
    }

    @Test
    void call_should_print_to_stdout_when_destination_is_dash() throws Exception {
        var tempFile = Files.createTempFile("test-ext", ".txt").toFile();
        FileUtils.writeStringToFile(tempFile, "extension content", "UTF-8");
        when(ocflApi.ocflObjectsIdExtensionFileGet(anyString(), anyString())).thenReturn(tempFile);

        var command = new OcflGetExtensionFile(context);
        setField(command, "id", "obj1");
        setField(command, "path", "ext1");
        setField(command, "destination", "-");

        var result = command.call();

        assertThat(result).isEqualTo(0);
        assertThat(outputStreamCaptor.toString()).contains("extension content");
        assertThat(outputStreamCaptor.toString()).doesNotContain("File saved to");
    }

    @Test
    void call_should_save_to_file_when_destination_is_path() throws Exception {
        var tempFile = Files.createTempFile("test-ext", ".txt").toFile();
        FileUtils.writeStringToFile(tempFile, "extension content", "UTF-8");
        when(ocflApi.ocflObjectsIdExtensionFileGet(anyString(), anyString())).thenReturn(tempFile);

        var destination = Files.createTempFile("dest", ".txt");
        Files.deleteIfExists(destination);

        var command = new OcflGetExtensionFile(context);
        setField(command, "id", "obj1");
        setField(command, "path", "ext1");
        setField(command, "destination", destination.toString());

        var result = command.call();

        assertThat(result).isEqualTo(0);
        assertThat(destination).exists();
        assertThat(Files.readString(destination)).isEqualTo("extension content");
        assertThat(errorStreamCaptor.toString()).contains("File saved to " + destination);
        assertThat(outputStreamCaptor.toString()).isEmpty();
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        var field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
