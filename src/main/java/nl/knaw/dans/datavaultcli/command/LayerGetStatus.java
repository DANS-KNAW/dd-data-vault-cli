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
import nl.knaw.dans.datavaultcli.client.ApiException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

@Command(name = "status",
         mixinStandardHelpOptions = true,
         description = "Show the status of a layer by id, or use 'top' to show the status of the top layer.")
public class LayerGetStatus implements Callable<Integer> {

    @ParentCommand
    private Layer layerCommand;

    @Parameters(index = "0", paramLabel = "ID|top",
                description = "Layer id (long) or the word 'top' to get the status of the top layer")
    private String idOrTop;

    @Override
    public Integer call() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            var api = layerCommand.getApi();

            var response = "top".equalsIgnoreCase(idOrTop)
                ? api.layersTopGet()
                : api.layersIdGet(Long.parseLong(idOrTop));

            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
            return 0;
        }
        catch (NumberFormatException e) {
            System.err.println("Error: layer id must be a number or 'top': " + idOrTop);
            return 1;
        }
        catch (ApiException | JsonProcessingException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
