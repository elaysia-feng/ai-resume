# Nacos 配置导入说明

在 Nacos 控制台创建下面这些配置：

| Data ID | Group | Format |
| --- | --- | --- |
| `gateway.yaml` | `DEFAULT_GROUP` | YAML |
| `gateway-routers.yaml` | `DEFAULT_GROUP` | YAML |
| `auth-service.yaml` | `DEFAULT_GROUP` | YAML |
| `resume-service.yaml` | `DEFAULT_GROUP` | YAML |
| `agent-service.yaml` | `DEFAULT_GROUP` | YAML |
| `interview-service.yaml` | `DEFAULT_GROUP` | YAML |
| `order-service.yaml` | `DEFAULT_GROUP` | YAML |
| `payment-service.yaml` | `DEFAULT_GROUP` | YAML |
| `notification-service.yaml` | `DEFAULT_GROUP` | YAML |

`gateway-routers.yaml` 是动态路由配置，示例在 `gateway-routers.yaml`。
其他文件是各微服务自己的业务配置，按需复制到 Nacos。
