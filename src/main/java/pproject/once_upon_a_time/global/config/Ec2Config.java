package pproject.once_upon_a_time.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

@Configuration
public class Ec2Config {

    @Bean
    public Ec2Client ec2Client(@Value("${aws.region}") String region) {
        return Ec2Client.builder()
            .region(Region.of(region))
            .build();
    }
}
