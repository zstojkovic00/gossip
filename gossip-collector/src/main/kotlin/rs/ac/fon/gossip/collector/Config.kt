package rs.ac.fon.gossip.collector

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory

@Configuration
open class Config {

    @Bean
    open fun neo4jDriver(
        @Value("\${spring.neo4j.uri}") neo4jUri: String,
        @Value("\${spring.neo4j.authentication.username}") username: String,
        @Value("\${spring.neo4j.authentication.password}") password: String
    ): Driver =
        GraphDatabase.driver(neo4jUri, AuthTokens.basic(username, password))

    @Bean
    open fun kafkaConsumerFactory(
        @Value("\${spring.kafka.bootstrap-servers}") bootstrapServers: String,
        @Value("\${spring.kafka.consumer.group-id}") groupId: String,
        @Value("\${spring.kafka.consumer.properties.schema.registry.url}") schemaRegistryUrl: String,
    ): ConsumerFactory<String, SpecificRecord> =
        DefaultKafkaConsumerFactory(
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG to groupId,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl,
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to "true",
            )
        )

    @Bean
    open fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, SpecificRecord>,
        @Value("\${spring.kafka.consumer.concurrency}") concurrency: Int,
    ): ConcurrentKafkaListenerContainerFactory<String, SpecificRecord> =
        ConcurrentKafkaListenerContainerFactory<String, SpecificRecord>().also {
            it.consumerFactory = consumerFactory
            it.setConcurrency(concurrency)
        }
}
