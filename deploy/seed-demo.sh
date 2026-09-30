#!/usr/bin/env bash
# Adds a demo family week to a fresh deployment:  deploy/seed-demo.sh https://1-2-3-4.sslip.io
set -euo pipefail
BASE="${1:?usage: seed-demo.sh <base-url>}"
week_start=$(date -d "last sunday" +%F 2>/dev/null || date -v-sun +%F)
[ "$(date +%u)" = 7 ] && week_start=$(date +%F)
day() { date -d "$week_start + $1 days" +%F 2>/dev/null || date -j -v+"$1"d -f %F "$week_start" +%F; }

post() { curl -fsS -o /dev/null -H 'Content-Type: application/json' -X POST "$BASE/api/tasks" -d "$1"; }

post "{\"title\":\"אימון כדורגל\",\"assigneeId\":\"itai\",\"scheduleMode\":\"FIXED\",\"date\":\"$(day 0)\",\"startTime\":\"17:00\",\"durationMinutes\":90,\"recurrence\":{\"kind\":\"WEEKLY\",\"weekDays\":[\"SUNDAY\",\"WEDNESDAY\"]}}"
post "{\"title\":\"חוג ציור\",\"assigneeId\":\"noa\",\"scheduleMode\":\"FIXED\",\"date\":\"$(day 0)\",\"startTime\":\"16:30\",\"durationMinutes\":60,\"recurrence\":{\"kind\":\"WEEKLY\",\"weekDays\":[\"TUESDAY\"]}}"
post "{\"title\":\"לסדר את החדר\",\"assigneeId\":\"itai\",\"scheduleMode\":\"DAY\",\"date\":\"$(day 0)\",\"recurrence\":{\"kind\":\"WEEKLY\",\"weekDays\":[\"FRIDAY\"]}}"
post "{\"title\":\"להחזיר ספרים לספרייה\",\"assigneeId\":\"ima\",\"scheduleMode\":\"DAY\",\"date\":\"$(day 2)\",\"importance\":2}"
post "{\"title\":\"לשלם ארנונה\",\"assigneeId\":\"aba\",\"scheduleMode\":\"DAY\",\"date\":\"$(day 3)\",\"importance\":5}"
post "{\"title\":\"אסיפת הורים\",\"assigneeId\":\"ima\",\"scheduleMode\":\"FIXED\",\"date\":\"$(day 4)\",\"startTime\":\"18:00\",\"durationMinutes\":45}"
post "{\"title\":\"להדליק נר לראש חודש\",\"assigneeId\":\"ima\",\"scheduleMode\":\"DAY\",\"date\":\"$(day 0)\",\"recurrence\":{\"kind\":\"ROSH_CHODESH\"}}"
post "{\"title\":\"יום הולדת לסבתא\",\"assigneeId\":\"ima\",\"scheduleMode\":\"DAY\",\"date\":\"$(day 0)\",\"importance\":5,\"recurrence\":{\"kind\":\"HEBREW_YEARLY\",\"hebrewMonth\":\"TEVET\",\"hebrewDay\":3}}"
post "{\"title\":\"לסדר את המחסן\",\"assigneeId\":\"aba\",\"scheduleMode\":\"AUTO\",\"durationMinutes\":90,\"deadline\":\"$(day 5)\"}"
echo "Demo data added."
