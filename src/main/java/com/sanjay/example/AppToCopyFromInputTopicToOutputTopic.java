package com.sanjay.example;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;

import java.util.Properties;


/**
 * Transforms Integer in InputTopic to Integer:Integer in Key:value fornat in OutputTopic using KStreams.
 *
 * HOW TO RUN THIS APP
 * 1) Start Zookeeper and Kafka.
 *    For the subsequent steps, we assume that you are on a directory from where you can issue the kafka runtime commands mentioned in Steps 2, 4, 5
 *
 * 2) Create the input and output topics used by this App.
 * $ bin/kafka-topics --create --topic InputTopic --zookeeper localhost:2181 --partitions 1 --replication-factor 1
 * $ bin/kafka-topics --create --topic OutputTopic --zookeeper localhost:2181 --partitions 1 --replication-factor 1
 *
 * 3) Start this example application either in your IDE or on the command line.
 *
 * 4) Start the console producer.
 *
 * $ bin/kafka-console-producer --broker-list localhost:9092 --topic InputTopic
 *
 * You can then enter input data by writing some line of text, followed by ENTER:
 * #   123
 * #   abc
 * # Every line you enter will become the value of a single Kafka message.
 *
 * 5) Inspect the resulting data in the output topics, e.g. via {@code kafka-console-consumer}.
 *
 * $ bin/kafka-console-consumer --topic OutputTopic --from-beginning --bootstrap-server localhost:9092 --property "print.key=true" --property "key.separator=:"
 *
 *
 * You should see output data similar to:
 * {"123": "123"}
 *
 * {"abx": " abc : Not an Integer"}
 *
 * 6) Once you're done, you can stop this example via {@code Ctrl-C}.
 *
 */

public class AppToCopyFromInputTopicToOutputTopic {

    private static final String KAFKA_BROKERS = "localhost:9092";
    private static final String APPLICATION_ID = "AppToCopyFromInputTopicToOutputTopic";
    private static final String CLIENT_ID = "AppToCopyFromInputTopicToOutputTopic-client";


    private static Properties configureStreams () {
        final Properties streamsConfiguration = new Properties();
        final String bootstrapServers = KAFKA_BROKERS;

        // Give the Streams application a unique name.  The name must be unique in the Kafka cluster
        // against which the application is run.
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, APPLICATION_ID);
        streamsConfiguration.put(StreamsConfig.CLIENT_ID_CONFIG, CLIENT_ID);
        // Where to find Kafka broker(s).
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Specify default (de)serializers for record keys and for record values.
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        return streamsConfiguration;
    }


    private static KafkaStreams buildStreams (Properties streamsConfiguration) {

        // In the subsequent lines we define the processing topology of the Streams application.
        final StreamsBuilder builder = new StreamsBuilder();

        // Read the input Kafka topic into an input KStream instance.
        final KStream<String, String> inputStream = builder.stream("InputTopic");

        // Create the output KStream
        final KStream<String, String> outputStream =
                inputStream.map((key, value) -> {
                            return new KeyValue<>(value, decodeValue(value)); // validate Integer
                        }
                );

        // Publish outputStream to OutputTopic
        outputStream.to("OutputTopic");


        final KafkaStreams streams = new KafkaStreams(builder.build(), streamsConfiguration);

        return streams;
    }

    // To validate input
    private static String decodeValue(String v) {
        try {
            return Integer.decode(v).toString();
        } catch (NumberFormatException nfe) {
            return v + " : Not an Integer";
        }
    }


    public static void main(final String[] args) {

        final Properties streamsConfiguration = configureStreams();

        final KafkaStreams streams = buildStreams(streamsConfiguration);

        streams.cleanUp();
        streams.start();

        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
