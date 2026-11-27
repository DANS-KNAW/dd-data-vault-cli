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
import nl.knaw.dans.datavaultcli.client.ApiException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "get",
         mixinStandardHelpOptions = true,
         description = "Print the value of an object version property as JSON, or all properties of the version if no property name is given.")
@RequiredArgsConstructor
public class ObjectPropertyGet implements Callable<Integer> {
    private final Context context;

    @Option(names = { "-i", "--identifier" }, required = true, description = "The object ID.")
    private String id;

    @Option(names = { "-v", "--version" }, defaultValue = "latest", description = "The version number (e.g. 1 or v1) or 'latest'. Default: ${DEFAULT-VALUE}.")
    private String version;

    @Parameters(index = "0", arity = "0..1", description = "The name of the property.")
    private String propertyName;

    @Override
    public Integer call() {
        try {
            var api = context.getObjectVersionPropertiesApi();
            var result = propertyName == null ?
                api.objectVersionPropertiesIdNrGet(id, version) :
                api.objectVersionPropertiesIdNrNameGet(id, version, propertyName);
            System.out.println(context.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result));
            return 0;
        }
        catch (ApiException e) {
            System.err.println("Error: " + ApiErrors.describe(e));
            return 1;
        }
        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
