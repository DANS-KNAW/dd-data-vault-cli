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
package nl.knaw.dans.datavaultcli.config.validation;

import lombok.extern.slf4j.Slf4j;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.nio.file.attribute.PosixFilePermissions;

@Slf4j
public class ModeValidator implements ConstraintValidator<ValidMode, String> {

    @Override
    public void initialize(ValidMode constraintAnnotation) {
    }

    @Override
    public boolean isValid(String mode, ConstraintValidatorContext context) {
        try {
            log.debug("Validating mode: {}.", mode);
            Integer.parseInt(mode, 8);
            return true;
        }
        catch (NumberFormatException e) {
            try {
                PosixFilePermissions.fromString(mode);
                return true;
            }
            catch (IllegalArgumentException e1) {
                log.error("Invalid mode: {}.", mode);
                return false;
            }
        }
    }
}