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
import lombok.RequiredArgsConstructor;
import nl.knaw.dans.datavaultcli.Context;
import nl.knaw.dans.datavaultcli.client.ApiException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "set",
         mixinStandardHelpOptions = true,
         description = {
             "Set an object version property, or a batch of properties from a CSV file. The new properties are validated against the property registry "
                 + "of the storage root before they are stored.",
             "",
             "A value that starts with '{' or '[' is parsed as JSON; any other value is taken as a string, unless --json is given. "
                 + "Use --from-file to read the value from a file.",
         },
         footer = {
             "",
             "The CSV file must have a header with the columns OBJECT_ID, OBJECT_VERSION_NR, PROPERTY and VALUE.",
             "",
             "A value starting with '@' is taken literally; '@<file>' does not read a file. Use --from-file for that.",
             "",
             "Example:",
             "  data-vault -r <root> object-property set -i urn:nbn:nl:ui:13-xxx -v 1 deaccessioned --from-file details.json"
         })
@RequiredArgsConstructor
public class ObjectPropertySet implements Callable<Integer> {
    static final String OBJECT_ID = "OBJECT_ID";
    static final String OBJECT_VERSION_NR = "OBJECT_VERSION_NR";
    static final String PROPERTY = "PROPERTY";
    static final String VALUE = "VALUE";

    private final Context context;

    @Spec
    private CommandSpec spec;

    @Option(names = { "-i", "--identifier" }, description = "The object ID.")
    private String id;

    @Option(names = { "-v", "--version" }, description = "The version number (e.g. 1 or v1).")
    private String version;

    @Option(names = { "-f", "--input-file" }, description = "CSV file with the properties to set. Cannot be combined with the other arguments.")
    private Path inputFile;

    @Option(names = { "--from-file" }, paramLabel = "<file>", description = "Read the value from <file> instead of from the command line.")
    private Path valueFile;

    @Option(names = { "--json" }, description = "Parse all values as JSON, also when they do not start with '{' or '['.")
    private boolean forceJson;

    @Parameters(index = "0", arity = "0..1", paramLabel = "<property-name>", description = "The name of the property.")
    private String propertyName;

    @Parameters(index = "1", arity = "0..1", paramLabel = "<value>", description = "The new value. Cannot be combined with --from-file.")
    private String value;

    @Override
    public Integer call() throws IOException {
        if (inputFile != null) {
            if (id != null || version != null || propertyName != null || value != null || valueFile != null) {
                throw new ParameterException(spec.commandLine(),
                    "--input-file cannot be combined with --identifier, --version, <property-name>, <value> or --from-file");
            }
            return setFromCsv();
        }
        if (id == null || version == null || propertyName == null || (value == null) == (valueFile == null)) {
            throw new ParameterException(spec.commandLine(),
                "Either --input-file, or --identifier, --version, <property-name> and exactly one of <value> and --from-file are required");
        }
        return setSingle();
    }

    private int setSingle() {
        JsonNode json;
        try {
            json = valueFile != null ?
                PropertyValues.parseFile(valueFile, forceJson, context.getObjectMapper()) :
                PropertyValues.parse(value, forceJson, context.getObjectMapper());
        }
        catch (IOException | IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
        return put(id, version, propertyName, json) ? 0 : 1;
    }

    private int setFromCsv() throws IOException {
        var format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get();
        var failures = 0;
        try (var parser = CSVParser.parse(Files.newBufferedReader(inputFile, StandardCharsets.UTF_8), format)) {
            var missing = List.of(OBJECT_ID, OBJECT_VERSION_NR, PROPERTY, VALUE).stream()
                .filter(column -> !parser.getHeaderMap().containsKey(column))
                .toList();
            if (!missing.isEmpty()) {
                System.err.println("Error: missing column(s) in " + inputFile + ": " + String.join(", ", missing));
                return 1;
            }
            for (var record : parser) {
                var line = record.getRecordNumber() + 1; // +1 for the header
                try {
                    var json = PropertyValues.parse(record.get(VALUE), forceJson, context.getObjectMapper());
                    if (!put(record.get(OBJECT_ID), record.get(OBJECT_VERSION_NR), record.get(PROPERTY), json)) {
                        failures++;
                    }
                }
                catch (IllegalArgumentException e) {
                    System.err.println("Error in line " + line + ": " + e.getMessage());
                    failures++;
                }
            }
        }
        if (failures > 0) {
            System.err.println(failures + " propert" + (failures == 1 ? "y" : "ies") + " could not be set");
            return 1;
        }
        return 0;
    }

    private boolean put(String objectId, String versionNr, String name, JsonNode json) {
        try {
            context.getObjectVersionPropertiesApi().objectVersionPropertiesIdNrNamePut(objectId, versionNr, name, json);
            System.err.println("Set property " + name + " on version " + versionNr + " of object " + objectId);
            return true;
        }
        catch (ApiException e) {
            System.err.println("Error setting property " + name + " on version " + versionNr + " of object " + objectId + ": " + ApiErrors.describe(e));
            return false;
        }
    }
}
