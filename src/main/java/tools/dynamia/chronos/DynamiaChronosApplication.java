package tools.dynamia.chronos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.dynamia.app.EnableDynamiaTools;
import tools.dynamia.modules.saas.api.AccountServiceAPI;
import tools.dynamia.modules.saas.api.NoOpAccountServiceAPI;
import tools.dynamia.navigation.DefaultPageProvider;
import tools.dynamia.ui.icons.IconsProvider;
import tools.dynamia.viewers.ViewDescriptorFactory;
import tools.dynamia.zk.ui.FontAwesomeIconsProvider;


@SpringBootApplication
@EnableDynamiaTools
@EnableScheduling
@EnableAsync
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

    @Bean
    public DefaultPageProvider defaultPageProvider() {
        return () -> "main/dashboard";
    }

    @Bean
    public IconsProvider iconsProvider() {
        return new FontAwesomeIconsProvider();
    }


    @Override
    public void run(String... args) throws Exception {
        viewDescriptorFactory.loadViewDescriptors();
        System.out.println("Dynamia Chronos Ready");
    }
}
