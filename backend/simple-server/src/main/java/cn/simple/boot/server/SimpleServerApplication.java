package cn.simple.boot.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目的启动类
 *
 * @author Simple Boot
 */
@SuppressWarnings("SpringComponentScan") // 忽略 IDEA 无法识别 ${simple.info.base-package}
@SpringBootApplication(scanBasePackages = {"${simple.info.base-package}.server", "${simple.info.base-package}.module"})
public class SimpleServerApplication {

    public static void main(String[] args) {

        SpringApplication.run(SimpleServerApplication.class, args);
//        new SpringApplicationBuilder(SimpleServerApplication.class)
//                .applicationStartup(new BufferingApplicationStartup(20480))
//                .run(args);
    }

}
