package com.suganthan.apachekafka.kafkaproducertwitter;

import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Created by msuganthan on 8/11/20.
 */
public class TwitterProducer {

    Logger logger = LoggerFactory.getLogger(TwitterProducer.class.getName());

    final String consumerKey    = "kQZ9IzkGLez6L19AZgib0t4EP";
    final String consumerSecret = "TBQmPDBT6CxKKBIdfonCcFh6deeL5nayQ35LlTrbufM8zaxbgQ";
    final String token          = "1667579858-dWIZwCe2WJBULQo0yMoXGy4Oh9Zml0p4YrmXRFb";
    final String secret         = "LB1TT5XYxgb2DDkKZhGACV0fYxABXZxa8cm6AuQylfDr2";

    List<String> searchTerms = Arrays.asList("bitcoin", "usa", "politics", "sport", "soccer");

    public TwitterProducer() {}

    public static void main(String[] args) {
        new TwitterProducer().run();
    }

    public void run() {
        logger.info("Setup");

        BlockingQueue<String> queue = new LinkedBlockingDeque<>(1000);
        //Create the twitter client

        Client client = createTwitterClient(queue);
        client.connect();

        //create a kafka producer
        KafkaProducer<String, String> producer = createKafkaProducer();

        while (!client.isDone()) {
            String message = null;

            try {
                message = queue.poll(5, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                exception.printStackTrace();
                client.stop();
            }

            if (message != null) {
                logger.info(message);
                producer.send(new ProducerRecord<String, String>("twitter_tweets", null, message), new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata metadata, Exception exception) {
                        if(exception != null) {
                            logger.info("Something bad happened {} ", exception);
                        }
                    }
                });
            }
        }
        logger.info("End of the application");
    }

    private Client createTwitterClient(BlockingQueue<String> queue) {
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hoseBirdEndpoint = new StatusesFilterEndpoint();
        hoseBirdEndpoint.trackTerms(searchTerms);

        Authentication authentication = new OAuth1(consumerKey, consumerSecret, token, secret);
        return new ClientBuilder().name("Hosebird-Client-01")
                                  .hosts(hosebirdHosts)
                                  .authentication(authentication)
                                  .endpoint(hoseBirdEndpoint)
                                  .processor(new StringDelimitedProcessor(queue))
                                  .build();
    }

    private KafkaProducer<String, String> createKafkaProducer() {
        String bootStrapServer = "127.0.0.1:9092";

        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        //create safe producer
        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        properties.setProperty(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
        properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");

        //high through put setting
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32 * 1024));

        return new KafkaProducer<>(properties);
    }
}
