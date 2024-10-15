package org.gone.dameng.autogen;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
@MapperScan(basePackages = {"org.gone.dameng.autogen.mapper"})
public class AutoGenerator {

    public static void main(String[] args) {
        SpringApplication.run(AutoGenerator.class, args);
    }
}