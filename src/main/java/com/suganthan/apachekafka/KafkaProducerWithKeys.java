package com.suganthan.apachekafka;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.awt.image.Kernel;
import java.util.Properties;

@SpringBootApplication
public class KafkaProducerWithKeys {

	public static void main(String[] args) throws Exception{

        Logger logger = LoggerFactory.getLogger(KafkaProducerWithKeys.class);

        String bootStrapServers = "127.0.0.1:9092";
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());


        //create a producer
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
        for (int i = 0; i < 10; i++) {

            String topic = "first_topic";
            String value = "hello world"+Integer.toString(i);
            String id = "id_"+Integer.toString(i);

            logger.info("Key "+id);
            ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, id, value);

            producer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                    if (e !=null) {
                        logger.error("Error while producing ", e);
                    } else {
                        logger.info("Received new metadata. \n"+
                                "Topic: "+recordMetadata.topic() + "\n" +
                                "Partition: "+recordMetadata.partition() + "\n"+
                                "Offset: "+recordMetadata.offset()+ "\n"+
                                "Timestamp: "+ recordMetadata.timestamp());
                    }
                }
            }).get();
        }

        producer.flush();
    }
}
