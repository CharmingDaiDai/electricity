# 租房电费记录 App

这是一个 Android Kotlin + Jetpack Compose 项目，用于记录租房每日用电量，并按 15 天周期计算阶梯电价和退补金额。

## 不安装 Android 环境的构建方式

1. 在 GitHub 新建一个仓库。
2. 把本目录所有文件推送到仓库。
3. 打开 GitHub 仓库的 `Actions` 页面。
4. 选择 `Android APK` 工作流，点击 `Run workflow`。
5. 构建完成后，在该次运行页面底部的 `Artifacts` 下载 `electricity-bill-debug-apk`。
6. 解压后把 `app-debug.apk` 传到安卓手机安装。

## 当前版本

- 已包含核心计价逻辑。
- 已包含单日录入、批量录入、周期汇总、差额显示。
- 数据暂用本地 SharedPreferences 保存，后续可升级为 Room + DataStore。
- GitHub Actions 会运行单元测试并生成 debug APK。
