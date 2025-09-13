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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import nl.knaw.dans.datavaultcli.Context;
import nl.knaw.dans.datavaultcli.client.ApiException;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

@Command(name = "list-ids",
         mixinStandardHelpOptions = true,
         description = "List all layer IDs.")
@RequiredArgsConstructor
public class LayerGetIds implements Callable<Integer> {
    private final Context context;

    @Override
    public Integer call() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            var ids = context.getApi().layersIdsGet();
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ids));
            return 0;
        }
        catch (ApiException | JsonProcessingException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
