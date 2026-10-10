#!/bin/bash
# 临时冒烟测试脚本 2：医生处理流程（非交付物，测试后删除）
B=http://localhost:8080
tok() { sed -n 's/.*"token":"\([^"]*\)".*/\1/p'; }

RT=$(curl -s -X POST $B/api/auth/login -H "Content-Type: application/json" -d '{"username":"resident","password":"123456"}' | tok)
DT=$(curl -s -X POST $B/api/auth/login -H "Content-Type: application/json" -d '{"username":"doctor","password":"123456"}' | tok)

echo "== D1 医生待处理预警列表 =="
curl -s "$B/api/doctor/alerts?status=PENDING&page=1&size=3" -H "Authorization: Bearer $DT" | head -c 500
echo; echo "== D2 医生查看预警详情(alert 1) =="
curl -s "$B/api/doctor/alerts/1" -H "Authorization: Bearer $DT" | head -c 500
echo; echo "== D3 未开始直接完成 (期望 409) =="
curl -s -X POST "$B/api/doctor/alerts/1/handling" -H "Authorization: Bearer $DT" -H "Content-Type: application/json" \
 -d '{"actionType":"RESOLVE","handlingComment":"直接完成"}'
echo; echo "== D4 开始处理 (PENDING->PROCESSING) =="
curl -s -X POST "$B/api/doctor/alerts/1/handling" -H "Authorization: Bearer $DT" -H "Content-Type: application/json" \
 -d '{"actionType":"START","handlingComment":"已电话联系居民，建议复测血压"}' | sed -n 's/.*"status":"\([^"]*\)".*"statusLabel":"\([^"]*\)".*/status=\1 (\2)/p'
echo "== D5 完成处理 (PROCESSING->RESOLVED) =="
curl -s -X POST "$B/api/doctor/alerts/1/handling" -H "Authorization: Bearer $DT" -H "Content-Type: application/json" \
 -d '{"actionType":"RESOLVE","handlingComment":"已安排门诊随访，调整用药","followupRequired":true,"nextFollowupAt":"2026-10-20 09:00:00"}' \
 | sed -n 's/.*"status":"\([^"]*\)".*"statusLabel":"\([^"]*\)".*/status=\1 (\2)/p'
echo "== D6 重复完成 (期望 409) =="
curl -s -X POST "$B/api/doctor/alerts/1/handling" -H "Authorization: Bearer $DT" -H "Content-Type: application/json" \
 -d '{"actionType":"RESOLVE","handlingComment":"再次完成"}'
echo; echo "== D7 空处理意见 (期望 400) =="
curl -s -X POST "$B/api/doctor/alerts/3/handling" -H "Authorization: Bearer $DT" -H "Content-Type: application/json" \
 -d '{"actionType":"START","handlingComment":""}'
echo; echo "== D8 处理记录列表 =="
curl -s "$B/api/doctor/alerts/1/handling-records" -H "Authorization: Bearer $DT" | head -c 700
echo; echo "== D9 居民查看 alert 1 详情（状态+医生意见） =="
curl -s "$B/api/health/alerts/1" -H "Authorization: Bearer $RT" | head -c 900
echo; echo "== D10 医生查看授权居民列表 =="
curl -s "$B/api/doctor/residents" -H "Authorization: Bearer $DT"
echo; echo "== D11 医生查看该居民指标 =="
curl -s "$B/api/doctor/residents/1/indicators?page=1&size=2" -H "Authorization: Bearer $DT" | head -c 300
echo; echo "== D12 医生越权访问非授权居民指标 (期望 403) =="
curl -s "$B/api/doctor/residents/999/indicators" -H "Authorization: Bearer $DT"
echo; echo "== D13 医生访问不存在的预警 (期望 404) =="
curl -s "$B/api/doctor/alerts/99999" -H "Authorization: Bearer $DT"
echo
