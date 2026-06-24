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
import org.apache.commons.io.FileUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "get-extension-file",
         mixinStandardHelpOptions = true,
         description = "Get the content of a specific extension file.")
@RequiredArgsConstructor
public class OcflGetExtensionFile implements Callable<Integer> {
    private final Context context;

    @Parameters(index = "0", description = "The object ID.")
    private String id;

    @Parameters(index = "1", description = "The path to the specific extension file.")
    private String path;

    @Parameters(index = "2", description = "The destination path on the local file system. Use '-' for stdout.")
    private String destination;

    @Override
    public Integer call() {
        try {
            var file = context.getOcflApi().ocflObjectsIdExtensionFileGet(id, path);

            if ("-".equals(destination)) {
                FileUtils.copyFile(file, System.out);
            }
            else {
                FileUtils.copyFile(file, Path.of(destination).toFile());
                System.err.println("File saved to " + destination);
            }

            return 0;
        }
        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
