# Seata 资源说明

这个目录只放 Seata 接入需要的配置和 SQL，不直接替换业务服务代码。

## 文件用途

- `server/application.yml`：Seata Server 自身配置，使用 Nacos 做注册/配置中心。
- `nacos/seataServer.properties`：写入 Nacos 的 Seata 配置，包含 MySQL 存储配置和事务分组映射。
- `sql/seata_server_mysql.sql`：在 `seata` 库执行，创建 Seata Server 的全局事务表。
- `sql/undo_log_mysql.sql`：在每个参与 AT 模式事务的业务库执行，例如 `ai_resume`。
- `client/application-seata-example.yaml`：业务服务 `application.yaml` 中可复制的 Seata 客户端配置。

## 当前约定

- Nacos：`127.0.0.1:8848`
- Seata Server 应用名：`seata-server`
- Seata Server DB：`seata`
- 业务事务分组：`default_tx_group`
- Seata 集群名：`default`

如果 Seata Server 跑在 Docker 容器里，`127.0.0.1` 指容器内部。连接宿主机 MySQL/Nacos 时，需要改成宿主机 IP 或 Docker network 服务名。
