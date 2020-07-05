package io.nanovc.nano_jgit_analyzer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.nanovc.CommitTags;
import io.nanovc.areas.StringHashMapArea;
import io.nanovc.junit.TestDirectory;
import io.nanovc.junit.TestDirectoryExtension;
import io.nanovc.memory.MemoryCommit;
import io.nanovc.memory.strings.StringMemoryRepoHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the excellent Jackson serialization library.
 * https://github.com/FasterXML/jackson-databind/
 */
@ExtendWith(TestDirectoryExtension.class)
public class JacksonTests
{
    /**
     * 1 minute tutorial: POJOs to JSON and back
     * https://github.com/FasterXML/jackson-databind/#1-minute-tutorial-pojos-to-json-and-back
     */
    @Test
    public void test_1_Minute_Tutorial(@TestDirectory Path testPath) throws IOException
    {
        // The most common usage is to take piece of JSON,
        // and construct a Plain Old Java Object ("POJO") out of it.
        // So let's start there.
        //
        // With simple 2-property POJO like this:
        // // Note: can use getters/setters as well; here we just use public fields directly:
        /* public class MyValue {
           public String name;
           public int age;
           // NOTE: if using getters/setters, can keep fields `protected` or `private`
         }
         */

        // we will need a com.fasterxml.jackson.databind.ObjectMapper instance,
        // used for all data-binding, so let's construct one:
        ObjectMapper mapper = new ObjectMapper(); // create once, reuse

        // The default instance is fine for our use
        // -- we will learn later on how to configure mapper instance if necessary.
        // Usage is simple:
        //MyValue value = mapper.readValue(new File("data.json"), MyValue.class);
        // or:
        //value = mapper.readValue(new URL("http://some.com/api/entry.json"), MyValue.class);
        // or:
        MyValue value = mapper.readValue("{\"name\":\"Bob\", \"age\":13}", MyValue.class);

        // And if we want to write JSON, we do the reverse:
        MyValue myResultObject = value;
        mapper.writeValue(testPath.resolve("result.json").toFile(), myResultObject);
        // or:
        byte[] jsonBytes = mapper.writeValueAsBytes(myResultObject);
        assertEquals("{\"name\":\"Bob\",\"age\":13}", new String(jsonBytes, StandardCharsets.UTF_8));
        // or:
        String jsonString = mapper.writeValueAsString(myResultObject);
        assertEquals("{\"name\":\"Bob\",\"age\":13}", jsonString);
    }

    // Note: can use getters/setters as well; here we just use public fields directly:
    public static class MyValue
    {
        public String name;
        public int age;
        // NOTE: if using getters/setters, can keep fields `protected` or `private`
    }

    /**
     * 3 minute tutorial: Generic collections, Tree Model
     * https://github.com/FasterXML/jackson-databind/#3-minute-tutorial-generic-collections-tree-model
     */
    @Test
    public void test_3_Minute_Tutorial(@TestDirectory Path testPath) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper(); // create once, reuse

        // Beyond dealing with simple Bean-style POJOs, you can also handle JDK Lists, Maps:
        Map<String, Integer> scoreByName = mapper.readValue("{\"a\":1,\"b\":2}", Map.class);
        List<String> names = mapper.readValue("[\"cat\", \"dog\"]", List.class);

        // and can obviously write out as well
        mapper.writeValue(testPath.resolve("names.json").toFile(), names);

        // as long as JSON structure matches, and types are simple.
        // If you have POJO values, you need to indicate actual type
        // (note: this is NOT needed for POJO properties with List etc types):

        Map<String, ResultValue> results = mapper.readValue("{\"person1\": {\"name\":\"Bob\",\"age\":13} }",
                                                            new TypeReference<Map<String, ResultValue>>() { } );
        // why extra work? Java Type Erasure will prevent type detection otherwise

        // (note: no extra effort needed for serialization, regardless of generic types)

        // But wait! There is more!

        // While dealing with Maps, Lists and other "simple" Object types
        // (Strings, Numbers, Booleans) can be simple, Object traversal can be cumbersome.
        // This is where Jackson's Tree model (https://github.com/FasterXML/jackson-databind/wiki/JacksonTreeModel)
        // can come in handy:
        // can be read as generic JsonNode, if it can be Object or Array; or,
        // if known to be Object, as ObjectNode, if array, ArrayNode etc:
        ObjectNode root = (ObjectNode) mapper.readTree("{\"name\":\"Bob\",\"age\":13}");
        String name = root.get("name").asText();
        assertEquals("Bob", name);
        int age = root.get("age").asInt();
        assertEquals(13, age);

        // can modify as well: this adds child Object as property 'other', set property 'type'
        root.with("other").put("type", "student");
        String json = mapper.writeValueAsString(root);

        // with above, we end up with something like as 'json' String:
        // {
        //   "name" : "Bob", "age" : 13,
        //   "other" : {
        //      "type" : "student"
        //   }
        // }
        assertEquals("{\"name\":\"Bob\",\"age\":13,\"other\":{\"type\":\"student\"}}", json);

        // Tree Model can be more convenient than data-binding,
        // especially in cases where structure is highly dynamic,
        // or does not map nicely to Java classes.
    }

    public static class ResultValue
    {
        public String name;
        public int age;
        // NOTE: if using getters/setters, can keep fields `protected` or `private`
    }

    @Test
    public void RepoSerializationTests() throws JsonProcessingException
    {
        // Create the nano repo:
        StringMemoryRepoHandler handler = new StringMemoryRepoHandler();
        SimulatedInstantClock clock = new SimulatedInstantClock();
        handler.clock = clock;
        clock.nowOverride = Instant.ofEpochSecond(1234567890);

        StringHashMapArea area = handler.createArea();
        area.putString("Hello", "World");
        MemoryCommit commit = handler.commitToBranch(area, "master", "First Commit", CommitTags.withAuthor("Luke"));

        // Create the object mapper:
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Get the JSON for the repo:
        String json = objectMapper.writeValueAsString(handler.getRepo());
        assertEquals("{\n" +
                     "  \"danglingCommits\" : [ ],\n" +
                     "  \"branchTips\" : {\n" +
                     "    \"master\" : {\n" +
                     "      \"timestamp\" : {\n" +
                     "        \"instant\" : {\n" +
                     "          \"nano\" : 0,\n" +
                     "          \"epochSecond\" : 1234567890\n" +
                     "        }\n" +
                     "      },\n" +
                     "      \"snapshot\" : {\n" +
                     "        \"/Hello\" : {\n" +
                     "          \"bytes\" : \"V29ybGQ=\",\n" +
                     "          \"efficientByteArray\" : \"V29ybGQ=\"\n" +
                     "        }\n" +
                     "      },\n" +
                     "      \"firstParent\" : null,\n" +
                     "      \"otherParents\" : null,\n" +
                     "      \"message\" : \"First Commit\",\n" +
                     "      \"commitTags\" : {\n" +
                     "        \"/author\" : {\n" +
                     "          \"value\" : \"Luke\",\n" +
                     "          \"charset\" : \"UTF-8\",\n" +
                     "          \"efficientByteArray\" : \"THVrZQ==\"\n" +
                     "        }\n" +
                     "      }\n" +
                     "    }\n" +
                     "  },\n" +
                     "  \"tags\" : { }\n" +
                     "}", json.replace(System.lineSeparator(), "\n"));
    }
}
