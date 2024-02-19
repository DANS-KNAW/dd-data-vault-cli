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
package nl.knaw.dans.datavaultcli;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import nl.knaw.dans.datavaultcli.config.ImportAreaConfig;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine.Command;
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
import java.util.Set;
import java.util.concurrent.Callable;

@RequiredArgsConstructor
@Command(name = "copy-batch",
         mixinStandardHelpOptions = true,
         description = "Copies a batch from source to target. Source must be an existing batch directory, target must be a location " +
             "inside one of the inboxes of the ingest area. If it is not, an error is returned.\n" +
             "If target is a directory with the same name as source, and it is empty, the contents of source will be copied into target. If target is not empty, an error is returned.\n" +
             "If target is a directory with a different name, the contents of source will be copied into a new directory with the name of source inside target.\n" +
             "After copying, the mode of the copied files and directories will be set to the value configured in the ingest " +
             "area.")
public class CopyBatch implements Callable<Integer> {
    @NonNull
    private final ImportAreaConfig importAreaConfig;

    @Parameters(index = "0", paramLabel = "source", description = "The path to the batch to copy.")
    private Path source;

    @Parameters(index = "1", paramLabel = "target", description = "The path to the destination inside import area of the vault.")
    private Path target;

    @Override
    public Integer call() throws Exception {
        if (!Files.isDirectory(source)) {
            System.err.println("Source must be an existing directory.");
            return 1;
        }

        if (!target.toAbsolutePath().startsWith(importAreaConfig.getPath().toAbsolutePath())) {
            System.err.println("Destination must be inside the import area.");
            return 1;
        }

        if (target.getFileName().equals(source.getFileName())) {
            if (!isDirectoryEmtpy(target)) {
                System.err.println("Target directory is not empty.");
                return 1;
            }
        }
        else {
            target = target.resolve(source.getFileName());
        }

        // Create the target directory if it does not exist
        if (!Files.exists(target)) {
            Files.createDirectories(target);
        }

        // Copy the contents of source into target
        FileUtils.copyDirectory(source.toFile(), target.toFile());

        setModeRecursively(target, importAreaConfig.getFileMode(), importAreaConfig.getDirectoryMode());

        return 0;
    }

    private boolean isDirectoryEmtpy(Path directory) {
        try (var files = Files.list(directory)) {
            return files.findAny().isEmpty();
        }
        catch (Exception e) {
            return false;
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
