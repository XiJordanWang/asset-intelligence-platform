package io.github.xijordanwang.assetintel.demo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoStartupRunner implements ApplicationRunner {

    private final DemoDataSeeder seeder;
    private final DemoProperties props;

    public DemoStartupRunner(DemoDataSeeder seeder, DemoProperties props) {
        this.seeder = seeder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (props.seedOnStartup()) {
            seeder.seedIfEmpty(props);
        }
    }
}
