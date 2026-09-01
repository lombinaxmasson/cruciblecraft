# 当前已知问题

机器可读台账：[tools/known_issues/verification-debt.json](../../tools/known_issues/verification-debt.json)

- 早期 Electrolyzer 闭卡曾被门闸 overlay、跨卡哈希链与超大 JSON 拖住：现场记录在
  `docs/history/work-logs/`。后续验证基础设施修复不重签该波 production lock
- 历史 census/topology `--check` 曾会因后波 composed v2 / profiles 变更把已关闭卡
  重算成 incomplete：已由 closeout seal 修复，不重签 Bath remainder lock
- 化学 currentness 漂移：留给后续配方工作
- 历史 READY 与 live snapshot 解绑：日常门不再消费历史收据；重绑留给玩家发行卡
- Java CRLF：工程卫生阶段已把当时的 31 个文件转为 LF
- `full_verification_report.json` 是历史收据，不是 live currentness
- 配方生成器 / 运行时曾把签发卡号当成类型，并在 Bath 上混用青铜化学信封与
  GT6 remainder compact。待重构，见
  [recipe-wave-workflow §4.3.1](recipe-wave-workflow.md)。现在不要为了改名去动
  已封板路径：closeout seal 钉了 closed-card `--check`
- 语义命名活代码表层已停。剩余 TXX（`build_t*.py`、绑定 ID、夹具、`t34_gt6`
  贴图、GameTest 方法名）故意留下，见
  [semantic-naming.md](semantic-naming.md)
