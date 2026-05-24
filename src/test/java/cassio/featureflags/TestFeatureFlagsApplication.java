package cassio.featureflags;

import org.springframework.boot.SpringApplication;

public class TestFeatureFlagsApplication {

    public static void main(String[] args) {
        SpringApplication.from(FeatureFlagsApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
