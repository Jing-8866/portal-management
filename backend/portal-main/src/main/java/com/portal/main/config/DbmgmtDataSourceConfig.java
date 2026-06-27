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
@MapperScan(basePackages = "com.portal.dbmgmt.mapper", sqlSessionFactoryRef = "dbmgmtSqlSessionFactory")
public class DbmgmtDataSourceConfig {

    @Bean(name = "dbmgmtDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.dbmgmt")
    public DataSource dbmgmtDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "dbmgmtSqlSessionFactory")
    public SqlSessionFactory dbmgmtSqlSessionFactory(@Qualifier("dbmgmtDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.getObject().getConfiguration().setMapUnderscoreToCamelCase(true);
        return bean.getObject();
    }

    @Bean(name = "dbmgmtSqlSessionTemplate")
    public SqlSessionTemplate dbmgmtSqlSessionTemplate(@Qualifier("dbmgmtSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
