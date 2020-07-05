package io.nanovc.nano_jgit_analyzer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.nanovc.CommitTags;
import io.nanovc.TimestampAPI;
import io.nanovc.areas.*;
import io.nanovc.content.ByteArrayContent;
import io.nanovc.content.StringContent;
import io.nanovc.junit.TestDirectory;
import io.nanovc.junit.TestDirectoryExtension;
import io.nanovc.memory.MemoryCommit;
import io.nanovc.memory.strings.StringMemoryRepo;
import io.nanovc.memory.strings.StringMemoryRepoHandler;
import io.nanovc.timestamps.InstantTimestamp;
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
                                                            new TypeReference<Map<String, ResultValue>>() {}
        );
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

        // Register Java 8 Types:
        // https://github.com/FasterXML/jackson-modules-java8
        // This is so that we handle Instant correctly.
        objectMapper.registerModule(new JavaTimeModule());


        SimpleModule module = new SimpleModule();
        objectMapper.registerModule(module);

        // Use mixins to provide annotations for types that we don't control directly:
        // https://github.com/FasterXML/jackson-docs/wiki/JacksonMixInAnnotations
        objectMapper.addMixIn(MemoryCommit.class, MemoryCommitMixin.class);

        objectMapper.addMixIn(TimestampAPI.class, TimestampAPIMixin.class);
        objectMapper.addMixIn(InstantTimestamp.class, TimestampAPIMixin.class);

        objectMapper.addMixIn(ByteArrayAreaAPI.class, ByteArrayAreaAPIMixin.class);
        objectMapper.addMixIn(ByteArrayHashMapArea.class, ByteArrayAreaAPIMixin.class);

        objectMapper.addMixIn(ByteArrayContent.class, ByteArrayContentMixin.class);

        objectMapper.addMixIn(StringAreaAPI.class, StringAreaAPIMixin.class);
        objectMapper.addMixIn(StringHashMapArea.class, StringAreaAPIMixin.class);

        objectMapper.addMixIn(StringLinkedHashMapArea.class, StringAreaAPIMixin.class);
        objectMapper.addMixIn(CommitTags.class, StringAreaAPIMixin.class);

        objectMapper.addMixIn(CommitTags.class, CommitTagsMixin.class);

        objectMapper.addMixIn(StringContent.class, StringContentMixin.class);

        //        module.addAbstractTypeMapping(TimestampAPI.class, TimestampAPISurrogate.class);


        // Get the JSON for the repo:
        String json = objectMapper.writeValueAsString(handler.getRepo());
        String expectedJSON =
            "{\n" +
            "  \"danglingCommits\" : [ ],\n" +
            "  \"branchTips\" : {\n" +
            "    \"master\" : {\n" +
            "      \"@class\" : \"io.nanovc.memory.MemoryCommit\",\n" +
            "      \"timestamp\" : {\n" +
            "        \"instant\" : 1234567890.000000000\n" +
            "      },\n" +
            "      \"snapshot\" : {\n" +
            "        \"/Hello\" : {\n" +
            "          \"bytes\" : \"V29ybGQ=\"\n" +
            "        }\n" +
            "      },\n" +
            "      \"firstParent\" : null,\n" +
            "      \"otherParents\" : null,\n" +
            "      \"message\" : \"First Commit\",\n" +
            "      \"commitTags\" : {\n" +
            "        \"/author\" : {\n" +
            "          \"value\" : \"Luke\"\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"tags\" : { }\n" +
            "}";
        assertEquals(expectedJSON, json.replace(System.lineSeparator(), "\n"));

        // Make sure that we can hydrate a repo from the JSON:
        StringMemoryRepo repoFromJSON = objectMapper.readValue(json, StringMemoryRepo.class);

        // Connect the repo to a handler:
        handler.setRepo(repoFromJSON);

        // Make sure that the repo is as expected:
        assertEquals("First Commit", repoFromJSON.getBranchTips().get("master").message);
    }

    @JsonSerialize(as = TimestampAPISurrogate.class)
    @JsonDeserialize(as = TimestampAPISurrogate.class)
    public static abstract class TimestampAPIMixin
    {
        // @JsonUnwrapped
        // public Instant instant;
    }

    public static class TimestampAPISurrogate extends InstantTimestamp
    {
        @JsonCreator // constructor can be public, private, whatever
        public TimestampAPISurrogate(@JsonProperty("instant") Instant instant)
        {
            super(instant);
        }
    }

    @JsonSerialize(as = ByteArrayAreaAPISurrogate.class)
    @JsonDeserialize(as = ByteArrayAreaAPISurrogate.class)
    public static abstract class ByteArrayAreaAPIMixin
    {
    }

    public static class ByteArrayAreaAPISurrogate extends ByteArrayHashMapArea
    {
        @JsonCreator // constructor can be public, private, whatever
        public ByteArrayAreaAPISurrogate()
        {
            super();
        }
    }

    @JsonSerialize(as = ByteArrayContentSurrogate.class)
    @JsonDeserialize(as = ByteArrayContentSurrogate.class)
    public static abstract class ByteArrayContentMixin
    {
        @JsonIgnore
        public abstract byte[] getEfficientByteArray();
    }

    public static class ByteArrayContentSurrogate extends ByteArrayContent
    {
        @JsonCreator
        public ByteArrayContentSurrogate(@JsonProperty("bytes") byte[] bytes)
        {
            super(bytes);
        }
    }

    @JsonSerialize(as = StringAreaAPISurrogate.class)
    @JsonDeserialize(as = StringAreaAPISurrogate.class)
    public static abstract class StringAreaAPIMixin
    {
    }

    public static class StringAreaAPISurrogate extends StringLinkedHashMapArea
    {
        @JsonCreator
        public StringAreaAPISurrogate()
        {
            super();
        }
    }


    @JsonSerialize(as = CommitTagsSurrogate.class)
    @JsonDeserialize(as = CommitTagsSurrogate.class)
    public static abstract class CommitTagsMixin
    {
    }

    public static class CommitTagsSurrogate extends CommitTags
    {
        @JsonCreator
        public CommitTagsSurrogate()
        {
            super();
        }
    }

    @JsonSerialize(as = StringContentSurrogate.class)
    @JsonDeserialize(as = StringContentSurrogate.class)
    public static abstract class StringContentMixin
    {
        @JsonIgnore
        public abstract byte[] getEfficientByteArray();

        @JsonIgnore
        public abstract String getCharset();
    }

    public static class StringContentSurrogate extends StringContent
    {
        @JsonCreator
        public StringContentSurrogate(@JsonProperty("value") String value)
        {
            super(value);
        }
    }


    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public static class MemoryCommitMixin
    {
    }
}
