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
import nl.knaw.dans.datavaultcli.api.DeleteFilesRequestDto;
import nl.knaw.dans.datavaultcli.client.ApiException;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "delete-file",
         mixinStandardHelpOptions = true,
         description = "Delete a file from the item store.")
@RequiredArgsConstructor
public class ItemstoreDeleteFile implements Callable<Integer> {
    private final Context context;

    @ParentCommand
    private Itemstore parent;

    @Parameters(index = "0", description = "The path to the file to delete.")
    private String path;

    @Override
    public Integer call() {
        try {
            var request = new DeleteFilesRequestDto();
            request.setPaths(List.of(path));
            context.getApi().itemstoreDeleteFilesPost(request);
            System.err.println("Deleted file " + path + " from respository");
            return 0;
        }
        catch (ApiException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
