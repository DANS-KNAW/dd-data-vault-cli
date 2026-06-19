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

@Command(name = "list-extension-files",
         mixinStandardHelpOptions = true,
         description = "List all extension files for an OCFL object.")
@RequiredArgsConstructor
public class OcflListExtensionFiles implements Callable<Integer> {
    private final Context context;

    @Parameters(index = "0", description = "The object ID.")
    private String id;

    @Override
    public Integer call() {
        try {
            var files = context.getOcflApi().ocflObjectsIdExtensionFilesGet(id);
            System.out.println(context.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(files));
            return 0;
        }
        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
