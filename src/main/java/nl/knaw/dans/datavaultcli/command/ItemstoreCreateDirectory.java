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
import nl.knaw.dans.datavaultcli.api.CreateDirectoryRequestDto;
import nl.knaw.dans.datavaultcli.client.ApiException;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "create-directory",
         mixinStandardHelpOptions = true,
         description = "Create a new directory in the item store.")
@RequiredArgsConstructor
public class ItemstoreCreateDirectory implements Callable<Integer> {
    private final Context context;

    @ParentCommand
    private Itemstore parent;

    @Parameters(index = "0", description = "The path to the directory to create.")
    private String path;

    @Override
    public Integer call() {
        try {
            var request = new CreateDirectoryRequestDto();
            request.setPath(path);
            context.getApi().itemstoreCreateDirectoryPost(request);
            System.err.println("Created directory " + path + " in respository");
            return 0;
        }
        catch (ApiException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
