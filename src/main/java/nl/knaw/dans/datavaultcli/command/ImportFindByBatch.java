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

import nl.knaw.dans.datavaultcli.Context;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "find-by-batch", mixinStandardHelpOptions = true, description = "Find import jobs for a batch.")
public class ImportFindByBatch extends ImportList {
    @Parameters(index = "0", paramLabel = "batch", description = "The batch path.")
    private String batch;

    public ImportFindByBatch(Context context) {
        super(context);
    }

    @Override
    protected String batch() {
        if (batch == null || batch.isBlank()) {
            throw new IllegalArgumentException("batch must not be blank");
        }
        return batch;
    }
}