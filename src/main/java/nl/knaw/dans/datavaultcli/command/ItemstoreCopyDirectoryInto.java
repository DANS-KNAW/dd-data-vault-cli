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
import nl.knaw.dans.datavaultcli.api.CopyDirectoryIntoRequestDto;
import nl.knaw.dans.datavaultcli.client.ApiException;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "copy-directory-into",
         mixinStandardHelpOptions = true,
         description = "Copy a directory into the item store.")
@RequiredArgsConstructor
public class ItemstoreCopyDirectoryInto implements Callable<Integer> {
    private final Context context;

    @ParentCommand
    private Itemstore parent;

    @Parameters(index = "0", description = "The source directory path.")
    private String source;

    @Parameters(index = "1", description = "The destination directory path in the item store.")
    private String destination;

    @Override
    public Integer call() {
        try {
            var request = new CopyDirectoryIntoRequestDto();
            request.setSource(source);
            request.setDestination(destination);
            context.getApi().itemstoreCopyDirectoryIntoPost(request);
            System.err.println("Copied " + source + " to " + destination + " in respository");
            return 0;
        }
        catch (ApiException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
