


192.168.56.107

docker compose -p flashcode -f /root/emrys-java/flashcode/deploy/dev/app/docker-compose-mid.yml build


docker compose -p flashcode -f /root/emrys-java/flashcode/deploy/dev/app/docker-compose-mid.yml up -d


docker compose -f docker-compose-mid.yml restart



docker compose -f docker-compose-mid.yml down
docker compose -f docker-compose-mid.yml up -d