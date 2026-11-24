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

import lombok.RequiredArgsConstructor;
import nl.knaw.dans.datavaultcli.Context;
import nl.knaw.dans.datavaultcli.api.RestageFailedRequestDto;
import nl.knaw.dans.datavaultcli.client.ApiException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "restage-failed",
         mixinStandardHelpOptions = true,
         description = "Restage failed imports for a job UUID or inbox batch path.")
@RequiredArgsConstructor
public class ImportRestageFailed implements Callable<Integer> {
    private final Context context;

    @Parameters(index = "0", paramLabel = "selector", description = "Import job UUID or inbox batch path.")
    private String selector;

    @Override
    public Integer call() {
        try {
            context.getApi().importsRestageFailedPost(new RestageFailedRequestDto().selector(selector));
            System.err.println("Failed imports restaged.");
            return 0;
        }
        catch (ApiException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}