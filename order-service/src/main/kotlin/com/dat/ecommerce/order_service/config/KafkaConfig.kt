package com.dat.ecommerce.order_service.config

import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

@Configuration
class KafkaConfig {

    @Bean
    @Primary
    fun kafkaTemplate(kafkaProperties: KafkaProperties): KafkaTemplate<String, Any> {
        val producerProps = kafkaProperties.buildProducerProperties(null)
        val factory = DefaultKafkaProducerFactory<String, Any>(producerProps)
        return KafkaTemplate(factory)
    }

    @Bean
    fun dltKafkaTemplate(kafkaProperties: KafkaProperties): KafkaTemplate<ByteArray, ByteArray> {
        val producerProps = kafkaProperties.buildProducerProperties(null)
        producerProps[org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = ByteArraySerializer::class.java
        producerProps[org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = ByteArraySerializer::class.java
        val factory = DefaultKafkaProducerFactory<ByteArray, ByteArray>(producerProps)
        return KafkaTemplate(factory)
    }

    @Bean
    fun commonErrorHandler(dltKafkaTemplate: KafkaTemplate<ByteArray, ByteArray>): CommonErrorHandler {
        val recoverer = DeadLetterPublishingRecoverer(dltKafkaTemplate) { record, _ ->
            TopicPartition(record.topic() + "-dlt", -1)
        }
        val errorHandler = DefaultErrorHandler(recoverer, FixedBackOff(1000L, 2L))
        errorHandler.setCommitRecovered(true)
        return errorHandler
    }
}
