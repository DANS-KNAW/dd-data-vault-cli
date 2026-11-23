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
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

abstract class OcflObjectBlockCommand implements Callable<Integer> {
    final Context context;

    @Parameters(index = "0", arity = "0..1", description = "Object ID; omit or use - to read one ID from stdin.")
    private String id;

    OcflObjectBlockCommand(Context context) {
        this.context = context;
    }

    abstract Boolean execute(String objectId) throws Exception;

    @Override
    public Integer call() {
        try {
            var objectId = id;
            if (objectId == null || objectId.equals("-")) {
                objectId = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
            }
            if (objectId == null || objectId.isBlank()) {
                throw new IllegalArgumentException("Object ID is required");
            }
            System.out.println(execute(objectId));
            return 0;
        }
        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}