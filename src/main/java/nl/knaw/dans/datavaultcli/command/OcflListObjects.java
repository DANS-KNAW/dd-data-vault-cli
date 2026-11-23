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
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.concurrent.Callable;

@Command(name = "list-objects",
         mixinStandardHelpOptions = true,
         description = "List all OCFL object IDs.")
@RequiredArgsConstructor
public class OcflListObjects implements Callable<Integer> {
    private final Context context;

    @Option(names = { "-l", "--limit" }, description = "Return one page with at most this many IDs (default: 100).")
    private Integer limit;

    @Option(names = { "-o", "--offset" }, description = "Return one page starting at this offset (default: 0).")
    private Integer offset;

    @ArgGroup(exclusive = true, multiplicity = "0..1")
    private Filter filter;

    static class Filter {
        @Option(names = "--blocked", description = "List blocked objects, including uningested objects.")
        boolean blocked;

        @Option(names = "--unblocked", description = "List unblocked ingested objects.")
        boolean unblocked;

        @Option(names = { "-all", "--all" }, description = "Include uningested objects.")
        boolean all;
    }

    @Override
    public Integer call() {
        try {
            var pageSize = limit == null ? 100 : limit;
            var pageOffset = offset == null ? 0 : offset;
            if (pageSize <= 0 || pageOffset < 0) {
                throw new IllegalArgumentException("limit must be positive and offset must be non-negative");
            }
            Boolean blocked = filter == null || filter.all ? null : filter.blocked;
            Boolean includeUningested = filter != null && filter.all ? true : null;
            var ids = new ArrayList<String>();
            while (true) {
                var page = context.getOcflApi().ocflObjectsGet(pageSize, pageOffset, blocked, includeUningested);
                ids.addAll(page);
                if (limit != null || offset != null || page.size() < pageSize) {
                    break;
                }
                pageOffset = Math.addExact(pageOffset, page.size());
            }
            System.out.println(context.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(ids));
            return 0;
        }
        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
