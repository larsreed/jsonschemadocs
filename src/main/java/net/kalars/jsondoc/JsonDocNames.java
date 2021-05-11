package net.kalars.jsondoc;

// This file contains all fixed schema names


import java.util.Arrays;
import java.util.List;

/** Strings for attributes etc. */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
interface JsonDocNames {
    // Prefixes specific to this tool
    String XDOC_PREFIX = "x-";
    String XIF_PREFIX = "xif-";
    String XIFNOT_PREFIX = "xifnot-";
    String IGNORE_PREFIX = "ignore-";
    // Other user input
    String USER_LINK_RE = "linkTo[(]([^),]+)((, *)([^)]+))?[)]"; // regexp to match linkTo(url) & linkTo(url, text)

    // JSON Schema names
    String ARRAY = "array";
    String CONTENT_ENCODING = "contentEncoding";
    String CONTENT_MEDIA_TYPE = "contentMediaType";
    String CONTENT_SCHEMA = "contentSchema";
    String CONST = "const";
    String DEFAULT = "default";
    String DEPRECATED = "deprecated";
    String DESCRIPTION = "description";
    String ENUM = "enum";
    String EXAMPLES = "examples";
    String EXCLUSIVE_MAXIMUM = "exclusiveMaximum";
    String EXCLUSIVE_MINIMUM = "exclusiveMinimum";
    String ID = "$id";
    String FIELD = "field";
    String FORMAT = "format";
    String MAXIMUM = "maximum";
    String MAX_ITEMS = "maxItems";
    String MAX_LENGTH = "maxLength";
    String MINIMUM = "minimum";
    String MIN_ITEMS = "minItems";
    String MIN_LENGTH = "minLength";
    String MULTIPLE_OF = "multipleOf";
    String PATTERN = "pattern";
    String PROPERTIES = "properties";
    String READ_ONLY = "readOnly";
    String REQUIRED = "required";
    String TITLE = "title";
    String TYPE = "type";
    String UNIQUE_ITEMS = "uniqueItems";
    String WRITE_ONLY = "writeOnly";

    List<String> PROP_KEYWORDS = Arrays.asList(
            DESCRIPTION, TYPE, EXAMPLES, ID,
            MINIMUM, EXCLUSIVE_MINIMUM,
            MAXIMUM, EXCLUSIVE_MAXIMUM,
            MIN_LENGTH, MAX_LENGTH,
            MIN_ITEMS, MAX_ITEMS, UNIQUE_ITEMS,
            FORMAT, PATTERN, ENUM, CONST, MULTIPLE_OF, DEFAULT, DEPRECATED, WRITE_ONLY, READ_ONLY,
            CONTENT_SCHEMA, CONTENT_ENCODING, CONTENT_MEDIA_TYPE);
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