# About this code

A prototype of a simple utility to be able to embed more comprehensive documentation within a JSON Schema.

Written by Lars Reed, april 2021. I'm happy for attribution if you use this code for anything ;)

# Writing schemas
* JSON Schema

    Start off by creating your actual schema, as per https://json-schema.org/
  
* Additional information

    * Decide on the headings you want for additional information, e.g. "Sample values".
      Add these as separate properties to the schema, alongside the regular schema keywords,
      but prefix the names with **"x-"**, and replace spaces with underscores -- i.e. *x-sample_values*.
      Such values will appear in the documentation, but will be stripped from the recreated schema output (see below).
    * You may also append other properties that appear neither in the documentation nor in the schema
      by starting the property names with **"ignore-"**.

* Sample:
```json
        "eventId": {
          "type": "string",
          "description": "UUID v4 format",
          "pattern": "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
          "x-My_additional_info": "is recorded here",
          "ignore-this" : "is ignored"
        },
```

# Producing documentation

## **Extracting schema for validation:**

The schema, without the documentation and ignored properties, can be recreated
for use in e.g. validators, like this:

```java
        final var visitor1 = new JsonSchemaPrintVisitor();
        new JsonGenParser().parseFile(fileName).visit(visitor1);
        final String schema = visitor1.toString();
```
 
## **Creating documentation**
  
Currently, two types of documentation are supported

1. HTML
   
    To create an HTML document documenting the schema, run a visitor like this
(the "2" parameter signifies that we want to embed tables of up to 2 rows).
```java
         final var visitor3 = new JsonDocHtmlVisitor(2);
         new JsonSchemaParser().parseFile(fileName).visit(visitor3);
         final String htmlDoc = visitor3.toString();
```

2. Wiki

   Like the HTML version, but no embedding of tables.
```java
        final var visitor2 = new JsonDocWikiVisitor();
        new JsonSchemaParser().parseFile(fileName).visit(visitor2);
        final String wikiDoc = visitor2.toString();
```


# Maintenance

## **Code style**

Having coded mostly in Scala (and a little Kotlin) lately, some classical Java conventions seem cumbersome...
So the code here is not entirely idiomatic - but it's mine. 

* *What? Multiple classes in the same file, and hardly anything public?*

      This is a small utility, and all classes fit nicely within a package. What should `public` do for me?
      And I think it's quite nice to find all the parser code in one file, all data structure nodes in one, etc.

* *Single line methods*

      Why does a single, short Java statement have to occupy 3+ lines in a file?
      It shouldn't...

* *Direct variable access*

    As I said, the code base is small. I'll introduce accessors when I need to,
    but having to write getters and setters really bugs me. So I don't.

* *Etc*

    Live with it :-) 

## **Lack of tests**

Yes, that is actually bad.  The code was created as a kind of discovery process, didn't think I'd even keep it,
but now that I did, there should be some structured tests...