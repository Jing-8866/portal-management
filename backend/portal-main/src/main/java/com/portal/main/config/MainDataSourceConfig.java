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
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = "com.portal.main.mapper", sqlSessionFactoryRef = "mainSqlSessionFactory")
public class MainDataSourceConfig {

    @Bean(name = "mainDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.main")
    public DataSource mainDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "mainSqlSessionFactory")
    @Primary
    public SqlSessionFactory mainSqlSessionFactory(@Qualifier("mainDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.getObject().getConfiguration().setMapUnderscoreToCamelCase(true);
        return bean.getObject();
    }

    @Bean(name = "mainSqlSessionTemplate")
    @Primary
    public SqlSessionTemplate mainSqlSessionTemplate(@Qualifier("mainSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
