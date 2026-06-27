package com.portal.main.config;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = "com.portal.log.mapper", sqlSessionFactoryRef = "logSqlSessionFactory")
public class LogDataSourceConfig {

    @Bean(name = "logDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.log")
    public DataSource logDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "logSqlSessionFactory")
    public SqlSessionFactory logSqlSessionFactory(@Qualifier("logDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.getObject().getConfiguration().setMapUnderscoreToCamelCase(true);
        return bean.getObject();
    }

    @Bean(name = "logSqlSessionTemplate")
    public SqlSessionTemplate logSqlSessionTemplate(@Qualifier("logSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
