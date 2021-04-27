package net.kalars.jsondoc;

// This file contains all fixed schema names


import java.util.Arrays;
import java.util.List;

/** Strings for attributes etc. */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
interface JsonDocNames {
    String XDOC_PREFIX = "x-";
    String XIF_PREFIX = "xif-";
    String XIFNOT_PREFIX = "xifnot-";
    String IGNORE_PREFIX = "ignore-";

    String FORMAT = "format";
    String MINIMUM = "minimum";
    String EXCLUSIVE_MINIMUM = "exclusiveMinimum";
    String MAXIMUM = "maximum";
    String EXCLUSIVE_MAXIMUM = "exclusiveMaximum";
    String MIN_LENGTH = "minLength";
    String MAX_LENGTH = "maxLength";
    String MIN_ITEMS = "minItems";
    String MAX_ITEMS = "maxItems";
    String UNIQUE_ITEMS = "uniqueItems";
    String PATTERN = "pattern";
    String ARRAY = "array";
    String FIELD = "field";
    String DESCRIPTION = "description";
    String REQUIRED = "required";
    String TYPE = "type";
    String TITLE = "title";
    String PROPERTIES = "properties";
    String ENUM = "enum";
    String CONST = "const";

    List<String> PROP_KEYWORDS = Arrays.asList(
            DESCRIPTION, TYPE,
            MINIMUM, EXCLUSIVE_MINIMUM,
            MAXIMUM, EXCLUSIVE_MAXIMUM,
            MIN_LENGTH, MAX_LENGTH,
            MIN_ITEMS, MAX_ITEMS, UNIQUE_ITEMS,
            FORMAT, PATTERN, ENUM, CONST);
}

//   Copyright 2021, Lars Reed -- lars-at-kalars.net
//
//           Licensed under the Apache License, Version 2.0 (the "License");
//           you may not use this file except in compliance with the License.
//           You may obtain a copy of the License at
//
//           http://www.apache.org/licenses/LICENSE-2.0
//
//           Unless required by applicable law or agreed to in writing, software
//           distributed under the License is distributed on an "AS IS" BASIS,
//           WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//           See the License for the specific language governing permissions and
//           limitations under the License.