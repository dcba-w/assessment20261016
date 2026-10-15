package cn.simple.boot.module.infra.framework.file.config;

import cn.simple.boot.module.infra.framework.file.core.client.FileClientFactory;
import cn.simple.boot.module.infra.framework.file.core.client.FileClientFactoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件配置类
 *
 * @author Simple Boot
 */
@Configuration(proxyBeanMethods = false)
public class SimpleFileAutoConfiguration {

    @Bean
    public FileClientFactory fileClientFactory() {
        return new FileClientFactoryImpl();
    }

}
