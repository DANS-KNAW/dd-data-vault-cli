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
import picocli.CommandLine.Option;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.concurrent.Callable;

@Command(name = "list", mixinStandardHelpOptions = true, description = "List import jobs.")
@RequiredArgsConstructor
public class ImportList implements Callable<Integer> {
    protected final Context context;

    @Option(names = "--pending", description = "Include pending jobs.")
    private boolean pending;

    @Option(names = "--running", description = "Include running jobs.")
    private boolean running;

    @Option(names = "--success", description = "Include successful jobs.")
    private boolean success;

    @Option(names = "--failed", description = "Include failed jobs.")
    private boolean failed;

    @Option(names = { "-l", "--limit" }, description = "Maximum number of jobs to return.")
    private Integer limit;

    @Option(names = { "-o", "--offset" }, description = "Number of jobs to skip; requires --limit.")
    private Integer offset;

    @Option(names = "--short", description = "Show only UUID, batch and status.")
    private boolean shortOutput;

    protected String batch() {
        return null;
    }

    @Override
    public Integer call() {
        try {
            if (limit != null && limit <= 0 || offset != null && (offset < 0 || limit == null)) {
                throw new IllegalArgumentException("limit must be positive; offset must be non-negative and requires limit");
            }
            var statuses = new ArrayList<String>();
            if (pending) statuses.add("PENDING");
            if (running) statuses.add("RUNNING");
            if (success) statuses.add("SUCCESS");
            if (failed) statuses.add("FAILED");
            var jobs = context.getApi().importsGet(statuses.isEmpty() ? null : statuses, limit, offset, batch());
            Object result = jobs;
            if (shortOutput) {
                result = jobs.stream().map(job -> {
                    var fields = new LinkedHashMap<String, Object>();
                    fields.put("uuid", job.getId());
                    fields.put("batch", job.getPath());
                    fields.put("status", job.getStatus());
                    return fields;
                }).toList();
            }
            System.out.println(context.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result));
            return 0;
        }
        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}