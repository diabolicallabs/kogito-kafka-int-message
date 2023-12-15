package org.acme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonFormat;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource.class)
public class IntermediateEventTest {

    @InjectKafkaCompanion
    KafkaCompanion companion;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(JsonFormat.getCloudEventJacksonModule());
    }

    @Test
    public void testStartFollowedByIntermediate() throws InterruptedException, JsonProcessingException {

        CloudEvent startEvent = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create(""))
                .withType("start")
                .withTime(OffsetDateTime.now())
                .withData("{\"name\": \"ganesha\"}".getBytes())
                .build();

        String sStartEvent = objectMapper.writeValueAsString(startEvent);

        CloudEvent intermediateEvent = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create(""))
                .withType("intermediate")
                .withTime(OffsetDateTime.now())
                .withData("{\"name\": \"ganapathi\"}".getBytes())
                .build();

        String sIntermmediateEvent = objectMapper.writeValueAsString(intermediateEvent);

        companion.produce(String.class).fromRecords(new ProducerRecord<>("start.msg", sStartEvent));
        Thread.sleep(1000 * 10);
        companion.produce(String.class).fromRecords(new ProducerRecord<>("intermediate.msg", sIntermmediateEvent));

        ConsumerTask<String, String> endEvents = companion.consumeStrings().fromTopics("end.msg", 1);
        endEvents.awaitCompletion();
        assertEquals(1, endEvents.count());
    }
}
