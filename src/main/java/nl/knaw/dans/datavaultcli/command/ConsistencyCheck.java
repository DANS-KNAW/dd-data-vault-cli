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
import nl.knaw.dans.datavaultcli.client.DefaultApi;
import picocli.CommandLine.Command;

import java.util.Map;
import java.util.concurrent.Callable;

@Command(name = "consistency-check",
         mixinStandardHelpOptions = true,
         description = "Manage consistency checks.")
public class ConsistencyCheck extends AbstractStorageRootCommand implements Callable<Integer> {

    public ConsistencyCheck(@NonNull Map<String, DefaultApi> storageRoots) {
        super(storageRoots);
    }

    @Override
    public Integer call() throws Exception {
        return 0;
    }
}
