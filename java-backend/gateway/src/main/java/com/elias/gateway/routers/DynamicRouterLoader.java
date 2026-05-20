package com.elias.gateway.routers;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.elias.gateway.config.GatewayAppProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouterLoader {

    private final NacosConfigManager nacosConfigManager;
    private final RouteDefinitionWriter routeDefinitionWriter;
    private final ApplicationEventPublisher publisher;
    private final GatewayAppProperties gatewayProperties;

    /**
     * 记录动态加载过的路由，方便刷新时删除旧路由
     */
    private final Set<String> routeIds = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void initRouteConfigListener() throws NacosException {
        GatewayAppProperties.DynamicRouter dynamicRouter = gatewayProperties.getDynamicRouter();
        String configInfo = nacosConfigManager.getConfigService()
                .getConfigAndSignListener(dynamicRouter.getDataId(), dynamicRouter.getGroup(), dynamicRouter.getTimeoutMs(), new Listener() {

                    @Override
                    public Executor getExecutor() {
                        return null;
                    }

                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        log.info("监听到 Nacos 网关路由配置变化");
                        updateConfigInfo(configInfo);
                    }
                });

        // 第一次启动时读取配置
        updateConfigInfo(configInfo);
    }

    private synchronized void updateConfigInfo(String configInfo) {
        if (configInfo == null || configInfo.isBlank()) {
            log.warn("Nacos 网关路由配置为空，dataId={}", gatewayProperties.getDynamicRouter().getDataId());
            return;
        }

        try {
            // 1. 删除旧路由
            clearOldRoutes();

            // 2. 解析 YAML
            List<RouteDefinition> routeDefinitions = parseRoutes(configInfo);

            // 3. 保存新路由
            for (RouteDefinition routeDefinition : routeDefinitions) {
                routeDefinitionWriter.save(Mono.just(routeDefinition)).block();

                routeIds.add(routeDefinition.getId());

                log.info("加载网关路由成功: id={}, uri={}",
                        routeDefinition.getId(),
                        routeDefinition.getUri());
            }

            // 4. 发布路由刷新事件
            publisher.publishEvent(new RefreshRoutesEvent(this));

            log.info("网关动态路由刷新完成，共加载 {} 条", routeDefinitions.size());

        } catch (Exception e) {
            log.error("更新网关动态路由失败，配置内容：{}", configInfo, e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<RouteDefinition> parseRoutes(String configInfo) {
        Yaml yaml = new Yaml();

        Map<String, Object> root = yaml.load(configInfo);

        if (root == null || !root.containsKey("routes")) {
            throw new IllegalArgumentException("gateway-routers.yaml 中缺少 routes 配置");
        }

        List<Map<String, Object>> routes = (List<Map<String, Object>>) root.get("routes");

        return routes.stream()
                .map(this::buildRouteDefinition)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private RouteDefinition buildRouteDefinition(Map<String, Object> routeMap) {
        RouteDefinition routeDefinition = new RouteDefinition();

        String id = String.valueOf(routeMap.get("id"));
        String uri = String.valueOf(routeMap.get("uri"));

        if (id == null || id.isBlank() || "null".equals(id)) {
            throw new IllegalArgumentException("路由 id 不能为空");
        }

        if (uri == null || uri.isBlank() || "null".equals(uri)) {
            throw new IllegalArgumentException("路由 uri 不能为空，routeId=" + id);
        }

        routeDefinition.setId(id);
        routeDefinition.setUri(URI.create(uri));

        Object predicatesObj = routeMap.get("predicates");
        if (predicatesObj instanceof List<?> predicates) {
            List<PredicateDefinition> predicateDefinitions = predicates.stream()
                    .map(String::valueOf)
                    .map(PredicateDefinition::new)
                    .toList();

            routeDefinition.setPredicates(predicateDefinitions);
        }

        Object filtersObj = routeMap.get("filters");
        if (filtersObj instanceof List<?> filters) {
            List<FilterDefinition> filterDefinitions = filters.stream()
                    .map(String::valueOf)
                    .map(FilterDefinition::new)
                    .toList();

            routeDefinition.setFilters(filterDefinitions);
        }

        Object orderObj = routeMap.get("order");
        if (orderObj != null) {
            routeDefinition.setOrder(Integer.parseInt(String.valueOf(orderObj)));
        }

        return routeDefinition;
    }

    private void clearOldRoutes() {
        for (String routeId : routeIds) {
            routeDefinitionWriter.delete(Mono.just(routeId))
                    .onErrorResume(e -> {
                        log.warn("删除旧路由失败: routeId={}", routeId, e);
                        return Mono.empty();
                    })
                    .block();
        }

        routeIds.clear();
    }
}
