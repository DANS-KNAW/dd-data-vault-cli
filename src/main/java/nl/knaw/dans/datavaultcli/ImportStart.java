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
package nl.knaw.dans.datavaultcli;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import nl.knaw.dans.datavaultcli.api.ImportCommandDto;
import nl.knaw.dans.datavaultcli.client.ApiException;
import nl.knaw.dans.datavaultcli.client.DefaultApi;
import nl.knaw.dans.datavaultcli.config.DataVaultConfiguration;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

@Command(name = "start",
         mixinStandardHelpOptions = true,
         description = "Start a job.")

@RequiredArgsConstructor
public class ImportStart implements Callable<Integer> {
    @NonNull
    private final DataVaultConfiguration configuration;

    @NonNull
    private final DefaultApi api;

    @Parameters(index = "0",
                paramLabel = "path",
                description = "The path to the object or batch of objects to import.")
    private String path;

    @Option(names = { "-s", "--single-object" },
            usageHelp = true,
            description = "The path parameter points to a single object import directory (by default path points to a batch directory).")
    private boolean singleObject;

    @Override
    public Integer call() {
        try {
            Path batchDir = Paths.get(this.path);
            api.importsPost(new ImportCommandDto()
                .path(Path.of(batchDir.toString()).toAbsolutePath().toString())
                .singleObject(singleObject));
            return 0;
        }
        catch (ApiException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
