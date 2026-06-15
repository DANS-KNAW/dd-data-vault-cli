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
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "describe-version",
         mixinStandardHelpOptions = true,
         description = "Get details of a specific version of an OCFL object.")
@RequiredArgsConstructor
public class OcflDescribeVersion implements Callable<Integer> {
    private final Context context;

    @Parameters(index = "0", description = "The object ID.")
    private String id;

    @Parameters(index = "1", description = "The version number or 'latest'.", defaultValue = "latest")
    private String version;

    @Override
    public Integer call() {
        try {
            var details = context.getOcflApi().ocflObjectsIdVersionsNrDetailsGet(id, version);
            System.out.println(context.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(details));
            return 0;
        }
        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
