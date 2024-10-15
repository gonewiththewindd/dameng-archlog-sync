package org.gone.dameng.autogen.schedule;

import com.baomidou.mybatisplus.extension.service.IService;
import lombok.extern.slf4j.Slf4j;
import org.gone.dameng.autogen.generator.AutoGenerator;
import org.gone.dameng.autogen.generator.TablePicker;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Random;

@Slf4j
@EnableScheduling
@Component
public class Scheduler implements ApplicationContextAware {

    public static final Random RANDOM = new Random(31);

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Scheduled(fixedDelay = 30)
    public void autoGenerate() {
        // 随机表
        Class clazz = TablePicker.pick();
        // 自动生成数据
        int size = RANDOM.nextInt(100, 200);
        List entities = AutoGenerator.generate(clazz, size);
        // 批量刷入数据库
        String simpleName = clazz.getSimpleName();
        log.info("[Scheduler-Generator]auto generating entity {} data...size:{}", simpleName, size);
        String serviceName = String.valueOf(simpleName.charAt(0)).toLowerCase().concat(simpleName.concat("ServiceImpl").substring(1));
        IService service = (IService) this.applicationContext.getBean(serviceName);
        if (Objects.isNull(service)) {
            throw new IllegalArgumentException("");
        }
        service.saveBatch(entities);
    }
}
