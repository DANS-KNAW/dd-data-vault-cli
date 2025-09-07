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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import nl.knaw.dans.datavaultcli.client.DefaultApi;
import picocli.CommandLine.Option;

import java.util.Map;

@RequiredArgsConstructor
public class AbstractStorageRootCommand {
    @NonNull
    private final Map<String, DefaultApi> storageRoots;

    @Option(names = { "-r", "--storage-root" },
            description = "The storage root to execute the command on.",
            required = true)
    protected String storageRoot;

    protected DefaultApi getApi() {
        var api = storageRoots.get(this.storageRoot);
        if (api == null) {
            System.err.println("No storage root found for " + this.storageRoot);
            throw new IllegalArgumentException("No storage root found for " + this.storageRoot);
        }
        return api;
    }

}
