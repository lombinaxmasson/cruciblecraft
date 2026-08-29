# 当前已知问题

机器可读台账：[tools/known_issues/verification-debt.json](../../tools/known_issues/verification-debt.json)

- T40 闭卡曾被门闸 overlay、跨卡哈希链与超大 JSON 拖住：见
  [T40 闭卡拖延回顾](../history/work-logs/t40-closeout-delay-review.md)；
  T40-VR 修复验证基础设施，不重签 T40 lock
- T5 chemical currentness 漂移：留给后续配方卡
- T17–T31 历史 READY 与 live snapshot 解绑：T32 已停止把历史收据当日常门；重绑留给玩家发行卡
- Java CRLF：T32 已把 31 个文件转为 LF
- `full_verification_report.json` 是历史收据，不是 live currentness
