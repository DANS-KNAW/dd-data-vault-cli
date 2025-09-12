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
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Parameters;

import java.util.UUID;
import java.util.concurrent.Callable;

@Command(name = "get")
public class ConsistencyCheckGet implements Callable<Integer> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @ParentCommand
    private ConsistencyCheck consistencyCheck;

    @Parameters(index = "0", paramLabel = "ID", description = "UUID of the consistency check to retrieve")
    private UUID id;

    @Override
    public Integer call() {
        try {
            var result = consistencyCheck.getApi().consistencyChecksIdGet(id);
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
