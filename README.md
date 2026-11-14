# 租房电费记录 App

这是一个 Android Kotlin + Jetpack Compose 项目，用于记录租房每日用电量，并按 15 天周期计算阶梯电价和退补金额。

## 不安装 Android 环境的构建方式

1. 在 GitHub 新建一个仓库。
2. 把本目录所有文件推送到仓库。
3. 打开 GitHub 仓库的 `Actions` 页面。
4. 选择 `Android APK` 工作流，点击 `Run workflow`。
5. 首次构建前，先按下面的“固定签名”步骤配置一次 GitHub Secrets。
6. 构建完成后，在该次运行页面底部的 `Artifacts` 下载 `electricity-bill-release-apk`。
7. 解压后把 `app-release.apk` 传到安卓手机安装。`debug` 包只用于排查问题，日常使用建议安装 `release` 包。

## 固定签名（只需做一次）

Android 更新安装要求新 APK 使用与旧 APK 相同的签名。以前的 release 包使用 GitHub runner 临时 debug key，每次构建可能不同，因此会出现“签名不一致”。现在 release 包使用固定的 keystore。

1. 打开 `Actions`，运行 `Generate stable signing key`，输入一个长期保存的 keystore 密码。
2. 下载构建产物 `electricity-release-keystore`，解压得到 `electricity-release.jks`。
3. 在仓库 `Settings -> Secrets and variables -> Actions` 添加以下 4 个 Repository secrets：
   - `ANDROID_KEYSTORE_PASSWORD`：第 1 步输入的密码。
   - `ANDROID_KEY_ALIAS`：填写 `electricity`。
   - `ANDROID_KEY_PASSWORD`：填写第 1 步相同的密码。
   - `ANDROID_KEYSTORE_BASE64`：在 Windows PowerShell 运行 ` [Convert]::ToBase64String([IO.File]::ReadAllBytes("electricity-release.jks")) `，把输出完整复制进去。
4. 以后只从 `electricity-bill-release-apk` 安装 release 包，后续更新就能直接覆盖安装并保留应用数据。

如果手机里已经安装的是旧 runner 签名的 APK，新固定签名无法直接覆盖它。请先在旧版本设置页导出完整备份，再卸载旧 APK、安装新 release，最后导入备份。Android 的系统自动备份也已开启，但它受手机厂商、账号和系统设置影响，不能替代手动备份。

## 当前版本

- 已包含核心计价逻辑。
- 已包含单日录入、批量录入、记录编辑/删除、周期汇总、差额显示。
- 已包含底部导航、概览页、记录页、设置页和历史周期展开明细。
- 记录页支持日 / 周 / 月 / 年切换，月视图以日历方式录入。
- 支持任意周期手动覆盖应执行电价，并可恢复自动计算。
- 支持登记每个周期实际已退/已补金额，默认填入计算出的应退/应补金额。
- 概览页支持按当前周期、历史周期或全部周期查看结算，不再把所有周期混成一个数字。
- 支持复制每日记录 CSV，便于粘贴到 Excel。
- 支持导出/导入包含设置、每日记录、手动电价和每周期退补记录的完整备份文件。
- 导入入口兼容旧版本导出的 `date,kwh` CSV；旧 CSV 只恢复每日记录，不会覆盖新设置。
- GitHub Actions 会同时生成 debug APK 和更适合日常使用的 release APK。
- 数据暂用本地 SharedPreferences 保存，后续可升级为 Room + DataStore。
