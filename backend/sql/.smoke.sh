#!/bin/bash
# 临时冒烟测试脚本（非交付物，测试后删除）
B=http://localhost:8080
tok() { sed -n 's/.*"token":"\([^"]*\)".*/\1/p'; }
json() { sed -n 's/.*"message":"\([^"]*\)".*/\1/p'; }

RT=$(curl -s -X POST $B/api/auth/login -H "Content-Type: application/json" -d '{"username":"resident","password":"123456"}' | tok)
DT=$(curl -s -X POST $B/api/auth/login -H "Content-Type: application/json" -d '{"username":"doctor","password":"123456"}' | tok)
echo "resident token len=${#RT}  doctor token len=${#DT}"

post() { curl -s -X POST $B/api/health/indicators -H "Authorization: Bearer $RT" -H "Content-Type: application/json" -d "$1"; }

echo; echo "== T1 血压 165/105 (期望 HIGH + 预警) =="
post '{"indicatorType":"BLOOD_PRESSURE","numericValue":165,"secondaryValue":105,"unit":"mmHg","measuredAt":"2026-10-10 09:00:00","requestId":"smoke-bp-165"}'
echo; echo "== T2 血压 185/115 (期望 CRITICAL) =="
post '{"indicatorType":"BLOOD_PRESSURE","numericValue":185,"secondaryValue":115,"unit":"mmHg","measuredAt":"2026-10-10 09:05:00","requestId":"smoke-bp-185"}'
echo; echo "== T3 空腹血糖 7.2 (期望 HIGH) =="
post '{"indicatorType":"BLOOD_GLUCOSE","numericValue":7.2,"unit":"mmol/L","measuredAt":"2026-10-10 07:10:00","measurementContext":"FASTING","requestId":"smoke-glu-72"}'
echo; echo "== T4 空腹血糖 6.3 (期望 MEDIUM) =="
post '{"indicatorType":"BLOOD_GLUCOSE","numericValue":6.3,"unit":"mmol/L","measuredAt":"2026-10-10 07:15:00","measurementContext":"FASTING","requestId":"smoke-glu-63"}'
echo; echo "== T5 体重 70 (期望无规则、无预警) =="
post '{"indicatorType":"WEIGHT","numericValue":70,"unit":"kg","measuredAt":"2026-10-10 07:20:00","requestId":"smoke-wt-70"}'
echo; echo "== T6 心率 130 (期望 HIGH) =="
post '{"indicatorType":"HEART_RATE","numericValue":130,"unit":"bpm","measuredAt":"2026-10-10 07:25:00","requestId":"smoke-hr-130"}'
echo; echo "== T7 收缩压 500 (期望 400 拒绝) =="
post '{"indicatorType":"BLOOD_PRESSURE","numericValue":500,"secondaryValue":100,"unit":"mmHg","measuredAt":"2026-10-10 09:30:00","requestId":"smoke-bad-500"}'
echo; echo "== T8 单位错误 (期望 400) =="
post '{"indicatorType":"BLOOD_PRESSURE","numericValue":130,"secondaryValue":85,"unit":"mmol/L","measuredAt":"2026-10-10 09:31:00","requestId":"smoke-bad-unit"}'
echo; echo "== T9 血糖缺场景 (期望 400) =="
post '{"indicatorType":"BLOOD_GLUCOSE","numericValue":6.0,"unit":"mmol/L","measuredAt":"2026-10-10 09:32:00","requestId":"smoke-bad-ctx"}'
echo; echo "== T10 收缩压<舒张压 (期望 400) =="
post '{"indicatorType":"BLOOD_PRESSURE","numericValue":80,"secondaryValue":120,"unit":"mmHg","measuredAt":"2026-10-10 09:33:00","requestId":"smoke-bad-order"}'

echo; echo "== T11 居民预警列表 =="
curl -s "$B/api/health/alerts?page=1&size=5" -H "Authorization: Bearer $RT" | head -c 600
echo; echo "== T12 居民无权访问医生接口 (期望 403) =="
curl -s "$B/api/doctor/alerts" -H "Authorization: Bearer $RT"
echo; echo "== T13 医生无权访问居民接口 (期望 403) =="
curl -s "$B/api/health/indicators" -H "Authorization: Bearer $DT"
echo; echo "== T14 无 token (期望 401) =="
curl -s -o /dev/null -w "http=%{http_code} " "$B/api/health/indicators"; curl -s "$B/api/health/indicators"
