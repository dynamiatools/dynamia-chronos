package tools.dynamia.chronos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.dynamia.modules.saas.api.AccountServiceAPI;
import tools.dynamia.modules.saas.jpa.NoOpAccountServiceAPI;
import tools.dynamia.viewers.ViewDescriptorFactory;
import tools.dynamia.zk.app.EnableDynamiaTools;

@SpringBootApplication
@EnableScheduling
@EnableDynamiaTools
@EntityScan("tools.dynamia")
public class DynamiaChronosApplication implements CommandLineRunner {

    @Autowired
    private ViewDescriptorFactory viewDescriptorFactory;

    public static void main(String[] args) {
        SpringApplication.run(DynamiaChronosApplication.class, args);
    }

    @Bean
    public AccountServiceAPI accountServiceAPI() {
        return new NoOpAccountServiceAPI();

    }


    @Override
    public void run(String... args) throws Exception {
        viewDescriptorFactory.loadViewDescriptors();
        System.out.println("Dynamia Chronos Ready");
    }
}
