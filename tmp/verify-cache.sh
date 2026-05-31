#!/bin/bash
echo '=== 1. 登录 ==='
RESP=$(docker exec backend sh -c "wget -q -O- --post-data='{\"username\":\"test\",\"password\":\"123456\"}' --header='Content-Type: application/json' http://localhost:8088/user/login")
echo "Login: ${RESP:0:100}..."
TOKEN=$(echo "$RESP" | grep -o '"data":"[^"]*"' | head -1 | sed 's/"data":"//;s/"//')
echo "Token: ${TOKEN:0:30}..."

echo ''
echo '=== 2. 获取图片广场数据(UUID) ==='
SQUARE=$(docker exec backend sh -c "wget -q -O- 'http://localhost:8088/image/square?page=1&limit=2'")
echo "Square: ${SQUARE:0:200}..."
IMAGE_UUID=$(echo "$SQUARE" | grep -o '"uuid":"[^"]*"' | head -1 | sed 's/"uuid":"//;s/"//')
IMAGE_UID=$(echo "$SQUARE" | grep -o '"userId":[0-9]*' | head -1 | sed 's/"userId"://')
echo "Image UUID: $IMAGE_UUID, UserId: $IMAGE_UID"

echo ''
echo '=== 3. 触发图片详情缓存 ==='
docker exec backend sh -c "wget -q -O- --header 'satoken: ${TOKEN}' http://localhost:8088/image/${IMAGE_UUID}" | head -c 200
echo ''

echo ''
echo '=== 4. 触发我的图片列表 ==='
docker exec backend sh -c "wget -q -O- --header 'satoken: ${TOKEN}' 'http://localhost:8088/image/list?page=1&limit=2'" | head -c 200
echo ''

sleep 1

echo ''
echo '=== 5. Redis Keys ==='
docker exec redis redis-cli -a 'Lz1472583690.' KEYS '*' 2>/dev/null | sort

echo ''
echo '=== 6. TTL随机化验证 ==='
for k in $(docker exec redis redis-cli -a 'Lz1472583690.' KEYS 'image:entity:*' 2>/dev/null | head -5); do
  ttl=$(docker exec redis redis-cli -a 'Lz1472583690.' TTL "$k" 2>/dev/null)
  echo "  $k -> TTL: ${ttl}s"
done
for k in $(docker exec redis redis-cli -a 'Lz1472583690.' KEYS 'user:entity:*' 2>/dev/null | head -5); do
  ttl=$(docker exec redis redis-cli -a 'Lz1472583690.' TTL "$k" 2>/dev/null)
  echo "  $k -> TTL: ${ttl}s"
done

echo ''
echo '=== 7. 查询不存在的UUID(布隆过滤器拦截) ==='
docker exec backend sh -c "wget -q -O- --header 'satoken: ${TOKEN}' http://localhost:8088/image/00000000-0000-0000-0000-000000000000" | head -c 200
echo ''

echo ''
echo '=== 8. 评论/分页缓存确认移除 ==='
COMMENT=$(docker exec redis redis-cli -a 'Lz1472583690.' KEYS 'comment:*' 2>/dev/null)
PAGE=$(docker exec redis redis-cli -a 'Lz1472583690.' KEYS 'image:page:*' 2>/dev/null)
echo "comment keys: ${COMMENT:-[EMPTY — OK]}"
echo "page keys: ${PAGE:-[EMPTY — OK]}"

echo ''
echo '=== 9. BloomFilter 启动日志 ==='
docker logs backend 2>&1 | grep -i 'bloom\|Bloom'

echo ''
echo '=== 10. mutex锁 ==='
docker exec redis redis-cli -a 'Lz1472583690.' KEYS 'mutex:*' 2>/dev/null
echo '(should be empty)'
