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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import nl.knaw.dans.datavaultcli.api.ConsistencyCheckRequestDto;
import nl.knaw.dans.datavaultcli.api.ConsistencyCheckRequestDto.TypeEnum;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

@Command(name = "new")
public class ConsistencyCheckNew implements Callable<Integer> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @ParentCommand
    private ConsistencyCheck consistencyCheck;

    @ArgGroup(multiplicity = "1")
    private ExclusiveGroup group;

    static class ExclusiveGroup {
        @Option(names = { "-l", "--layer" }, description = "Check that the listing records of the layer are consistent with the files and directories on storage")
        private Long layer;

        @Option(names = { "-a", "--check-layer-ids" }, description = "Check that the layer IDs are the same on storage and the database")
        private boolean checkLayerIds;
    }

    @Override
    public Integer call() throws Exception {
        try {
            var request = new ConsistencyCheckRequestDto();
            if (group.checkLayerIds) {
                request.setType(TypeEnum.LAYER_IDS);
            }
            else if (group.layer != null) {
                request.setLayerId(group.layer);
                request.setType(TypeEnum.LISTING_RECORDS);
            }
            var result = consistencyCheck.getApi().consistencyChecksPost(request);
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.writerWithDefaultPrettyPrinter().writeValue(System.out, result);
            return 0;
        }
        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
