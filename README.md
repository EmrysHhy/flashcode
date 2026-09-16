


192.168.56.107

docker compose -p flashcode -f /root/emrys-java/flashcode/deploy/dev/app/docker-compose-mid.yml build


docker compose -p flashcode -f /root/emrys-java/flashcode/deploy/dev/app/docker-compose-mid.yml up -d


docker compose -f docker-compose-mid.yml restart



docker compose -f docker-compose-mid.yml down
docker compose -f docker-compose-mid.yml up -d


# 最近 100 行
docker logs --tail 100 flashcode-bite-admin-service-1

# portal 日志
docker logs --tail 300 flashcode-bite-portal-service-1

# 持续跟踪（Ctrl+C 停）
docker logs -f flashcode-bite-portal-service-1

# 只看启动成功或失败
docker logs flashcode-bite-portal-service-1 2>&1 | grep -E 'Started BitePortal|APPLICATION FAILED'
# 远程部署
clean deploy -pl bite-portal/bite-portal-service -am -DskipTests
# Spring启动日志
docker exec flashcode-userapp-preview tail -n 50 /workspace/user-preview/10000005/app.log