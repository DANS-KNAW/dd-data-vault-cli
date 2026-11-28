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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Converts property values as given on the command line or in an input file to JSON.
 */
final class PropertyValues {
    private PropertyValues() {
    }

    /**
     * Parses a value. If the value, after trimming, starts with '{' or '[', or if {@code forceJson} is true, it is parsed as strict JSON. Otherwise, it is
     * taken as a string.
     *
     * @param value     the value
     * @param forceJson whether to parse the value as JSON regardless of its first character
     * @param mapper    the object mapper to use
     * @return the value as a JSON node
     * @throws IllegalArgumentException if the value must be parsed as JSON, but is not valid JSON
     */
    static JsonNode parse(String value, boolean forceJson, ObjectMapper mapper) {
        var trimmed = value.trim();
        if (forceJson || trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                var node = mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(trimmed);
                if (node == null || node.isMissingNode()) {
                    throw new IllegalArgumentException("Value is empty; expected JSON");
                }
                return node;
            }
            catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Value is not valid JSON: " + e.getOriginalMessage(), e);
            }
        }
        return new TextNode(value);
    }

    /**
     * Parses a value read from a file. Trailing whitespace is removed from the file content before parsing.
     *
     * @param file      the file containing the value
     * @param forceJson whether to parse the value as JSON regardless of its first character
     * @param mapper    the object mapper to use
     * @return the value as a JSON node
     * @throws IOException              if the file cannot be read
     * @throws IllegalArgumentException if the value must be parsed as JSON, but is not valid JSON
     */
    static JsonNode parseFile(Path file, boolean forceJson, ObjectMapper mapper) throws IOException {
        return parse(Files.readString(file, StandardCharsets.UTF_8).stripTrailing(), forceJson, mapper);
    }
}
