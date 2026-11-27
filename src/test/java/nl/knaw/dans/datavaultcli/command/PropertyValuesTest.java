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
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyValuesTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void value_starting_with_brace_or_bracket_is_parsed_as_json() {
        assertThat(PropertyValues.parse(" {\"reason\": \"x\"}", false, mapper).get("reason").asText()).isEqualTo("x");
        assertThat(PropertyValues.parse("[1, 2]", false, mapper).isArray()).isTrue();
    }

    @Test
    void other_values_are_strings_even_if_they_look_like_numbers() {
        assertThat(PropertyValues.parse("1.0", false, mapper)).isEqualTo(new TextNode("1.0"));
        assertThat(PropertyValues.parse("true", false, mapper)).isEqualTo(new TextNode("true"));
        assertThat(PropertyValues.parse("Invalid dataset", false, mapper)).isEqualTo(new TextNode("Invalid dataset"));
    }

    @Test
    void force_json_parses_numbers_booleans_and_quoted_strings() {
        assertThat(PropertyValues.parse("1.0", true, mapper).isNumber()).isTrue();
        assertThat(PropertyValues.parse("true", true, mapper).isBoolean()).isTrue();
        assertThat(PropertyValues.parse("\"1.0\"", true, mapper)).isEqualTo(new TextNode("1.0"));
    }

    @Test
    void invalid_json_is_rejected() {
        assertThatThrownBy(() -> PropertyValues.parse("{\"reason\": \"x\"", false, mapper))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("Value is not valid JSON");
        assertThatThrownBy(() -> PropertyValues.parse("{} trailing", false, mapper))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PropertyValues.parse("not json", true, mapper))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PropertyValues.parse("  ", true, mapper))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
