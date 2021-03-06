package com.org.byBlog.config;

import com.org.byBlog.dao.RoleAccessDAO;
import com.org.byBlog.filter.CustomRolesAuthorizationFilter;
import com.org.byBlog.filter.JWTFilter;
import com.org.byBlog.pojo.po.RoleAccessPO;
import com.org.byBlog.shiro.AuthRealm;
import com.org.byBlog.shiro.JWTRealm;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import java.util.*;

@Configuration
public class ShiroConfig {

    @Autowired
    private RoleAccessDAO roleAccessDAO;

    //将自己的验证方式加入容器
    @Bean
    public AuthRealm authRealm() {
        AuthRealm authRealm = new AuthRealm();
        return authRealm;
    }

    //JWT
    public JWTRealm jwtRealm() {
        JWTRealm jwtRealm = new JWTRealm();
        return jwtRealm;
    }

    //权限管理，配置主要是Realm的管理认证
    @Bean
    public SecurityManager securityManager(CacheManager cacheManager, SessionManager sessionManager) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setSessionManager(sessionManager);
        securityManager.setRealms(Arrays.asList(authRealm(), jwtRealm()));
        securityManager.setCacheManager(cacheManager);
        return securityManager;
    }

    //Filter工厂，设置对应的过滤条件和跳转条件
    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(SecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);
        Map<String, String> map = new LinkedHashMap<>();
        map.put("/", "anon");
        map.put("/logout", "logout");
        // 用户相关
        map.put("/user/login", "anon");
        map.put("/user/register", "anon");
        map.put("/user/loginTimeout", "anon");
        // Swagger相关
        map.put("/swagger*", "anon");
        map.put("/swagger-resources/**", "anon");
        map.put("/webjars/**", "anon");
        map.put("/v2/**", "anon");
        map.put("/csrf/**", "anon");
        map.put("/configuration*", "anon");

        // 获取用户权限
        List<RoleAccessPO> roleAccessList = roleAccessDAO.getRoleAccessList();
        for (RoleAccessPO roleAccess : roleAccessList) {
            map.put(roleAccess.getPath(), "roleOrFilter[" + roleAccess.getRole() + "]");
        }
        map.put("/**", "authc");
        shiroFilterFactoryBean.setFilterChainDefinitionMap(map);
        //登录，此处改为返回未登录的接口
        shiroFilterFactoryBean.setLoginUrl("/user/loginTimeout");
        //首页
        shiroFilterFactoryBean.setSuccessUrl("/index");
        //错误页面，认证不通过跳转
        shiroFilterFactoryBean.setUnauthorizedUrl("/error");
        //添加过滤器
        Map<String, Filter> filterMap = new HashMap<>();
        filterMap.put("roleOrFilter", customRolesAuthorizationFilter());
        JWTFilter jwtFilter = new JWTFilter();
        filterMap.put("jwtFilter", jwtFilter);
        shiroFilterFactoryBean.setFilters(filterMap);
        return shiroFilterFactoryBean;
    }

    @Bean
    public CustomRolesAuthorizationFilter customRolesAuthorizationFilter() {
        CustomRolesAuthorizationFilter customRolesAuthorizationFilter = new CustomRolesAuthorizationFilter();
        return customRolesAuthorizationFilter;
    }

    //加入注解的使用，不加入这个注解不生效
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }

    @Bean
    public CacheManager cacheManager() {
        return new EhCacheManager();
    }

    @Bean
    public SessionDAO sessionDAO() {
        return new EnterpriseCacheSessionDAO();
    }

    /**
     * 管理session
     *
     * @return
     */
    @Bean
    public DefaultWebSessionManager sessionManager() {
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        // 去掉shiro登录时url里的JSESSIONID
        sessionManager.setSessionIdUrlRewritingEnabled(false);
        return sessionManager;
    }
}
