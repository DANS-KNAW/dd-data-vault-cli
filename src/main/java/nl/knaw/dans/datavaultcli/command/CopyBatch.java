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
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.datavaultcli.config.ImportAreaConfig;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

@RequiredArgsConstructor
@Slf4j
@Command(name = "copy-batch",
         mixinStandardHelpOptions = true,
         description = "Copies a batch from source to target, setting the permissions as specified in the configuration.")
public class CopyBatch implements Callable<Integer> {
    @NonNull
    private final Map<String, ImportAreaConfig> importAreaConfigs;

    @Option(names = {"-r", "--storage-root"},
            description = "The storage root to execute the command on.",
            required = true)
    private String storageRoot;

    @Parameters(index = "0", paramLabel = "source", description = "The path to the batch to copy.")
    private Path source;

    @Parameters(index = "1", paramLabel = "target", description = "The path to the destination inside import area of the vault.")
    private Path target;

    @Override
    public Integer call() throws Exception {
        log.debug("Copying batch from {} to {}", source, target);
        if (!Files.isDirectory(source)) {
            System.err.println("Source must be an existing directory.");
            return 1;
        }

        var importAreaConfig = importAreaConfigs.get(storageRoot);
        if (importAreaConfig == null) {
            System.err.printf("No import area found for storage root %s%n", storageRoot);
            return 1;
        }

        if (!target.toAbsolutePath().startsWith(importAreaConfig.getPath().toAbsolutePath())) {
            System.err.println("Destination must be inside the import area.");
            return 1;
        }

        if (target.getFileName().equals(source.getFileName()) && Files.exists(target)) {
            log.debug("Source and target have the same name, and target exists.");
            if (isDirectoryEmpty(target)) {
                log.debug("Target directory is empty.");
                Files.createDirectories(target); // In case some ancestors of the target do not exist yet
                FileUtils.copyDirectory(source.toFile(), target.toFile());
                System.err.printf("Copied %s to %s%n", source, target);
            }
            else {
                System.err.println("Target directory not empty. When source and target have the same name, and target exists, it must be empty.");
                return 1;
            }
        }
        else if (Files.exists(target)) {
            log.debug("Target exists, but has a different name than the source.");
            target = target.resolve(source.getFileName());
            FileUtils.copyDirectory(source.toFile(), target.toFile());
            System.err.printf("Copied %s to %s%n", source, target);
        }
        else {
            log.debug("Target does not exist yet.");
            Files.createDirectories(target.getParent());
            FileUtils.copyDirectory(source.toFile(), target.toFile());
            System.err.printf("Copied %s to %s%n", source, target);
        }

        System.err.printf("Setting permissions on %s%n", target);
        setModeRecursively(target, importAreaConfig.getFileMode(), importAreaConfig.getDirectoryMode());
        return 0;
    }

    private boolean isDirectoryEmpty(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            return files.findAny().isEmpty();
        }
    }

    private void setModeRecursively(Path path, String fileMode, String directoryMode) throws IOException {
        Set<PosixFilePermission> filePermissions = getPermissions(fileMode);
        Set<PosixFilePermission> directoryPermissions = getPermissions(directoryMode);
        setModeRecursively(path, filePermissions, directoryPermissions);
    }

    private void setModeRecursively(Path path, Set<PosixFilePermission> filePermissions, Set<PosixFilePermission> directoryPermissions) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.setPosixFilePermissions(file, filePermissions);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.setPosixFilePermissions(dir, directoryPermissions);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private Set<PosixFilePermission> getPermissions(String mode) throws IOException {
        try {
            return PosixFilePermissions.fromString(mode);
        }
        catch (IllegalArgumentException e) {
            return permissionsFromOctal(Integer.parseInt(mode, 8));
        }
    }

    private Set<PosixFilePermission> permissionsFromOctal(int octal) {
        String binaryStr = Integer.toBinaryString(octal);
        while (binaryStr.length() < 9) {
            binaryStr = "0" + binaryStr;
        }

        Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);

        if (binaryStr.charAt(0) == '1')
            permissions.add(PosixFilePermission.OWNER_READ);
        if (binaryStr.charAt(1) == '1')
            permissions.add(PosixFilePermission.OWNER_WRITE);
        if (binaryStr.charAt(2) == '1')
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        if (binaryStr.charAt(3) == '1')
            permissions.add(PosixFilePermission.GROUP_READ);
        if (binaryStr.charAt(4) == '1')
            permissions.add(PosixFilePermission.GROUP_WRITE);
        if (binaryStr.charAt(5) == '1')
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
        if (binaryStr.charAt(6) == '1')
            permissions.add(PosixFilePermission.OTHERS_READ);
        if (binaryStr.charAt(7) == '1')
            permissions.add(PosixFilePermission.OTHERS_WRITE);
        if (binaryStr.charAt(8) == '1')
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);

        return permissions;
    }
}
