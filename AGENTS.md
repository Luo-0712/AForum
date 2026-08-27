# Agent Notes

## Windows PowerShell 中文编码

在 Windows PowerShell 5.1 中不要把包含中文的脚本通过 here-string 直接管道给 `python -`。PowerShell 的 `$OutputEncoding` 默认可能是 ASCII，中文会在进入 Python 前变成 `?`。

优先做法：

1. 把 Python 脚本保存为 UTF-8（最好带 BOM）文件后执行，例如 `python -X utf8 script.py`。
2. 避免在内联脚本里硬编码中文路径；用 ASCII glob、文件大小、修改时间或其他元数据定位文件。
3. 如需双向处理外部命令输出，同时设置：

```powershell
$OutputEncoding = [System.Text.UTF8Encoding]::new()
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new()
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
$env:PYTHONUTF8 = '1'
```

PowerShell 脚本文件若包含中文，也保存为 UTF-8 with BOM，避免 Windows PowerShell 5.1 按旧代码页解析。
