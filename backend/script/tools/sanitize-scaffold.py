# One-shot sanitizer for SQL dumps and Java @author lines.
import re
from pathlib import Path

ROOT = Path(r"d:\personal_project\template")
BOOT = ROOT / "simple-boot-new"

# Real cloud keys are intentionally not stored here. Add leftover key
# strings in a local untracked file before running this sanitizer.
SQL_REPLACEMENTS = [
    ("http://test.simple.iocoder.cn", "http://127.0.0.1:48080"),
    ("http://tengxun-oss.iocoder.cn", "http://127.0.0.1:48080"),
    ("http://ali-oss.iocoder.cn", "http://127.0.0.1:48080"),
    ("https://www.iocoder.cn", "http://127.0.0.1"),
    ("https://doc.iocoder.cn/", "http://127.0.0.1/"),
    ("https://doc.iocoder.cn", "http://127.0.0.1"),
    ("https://cloud.iocoder.cn", "http://127.0.0.1"),
    ("www.iocoder.cn", "localhost"),
    ("zsxq.iocoder.cn", "localhost"),
    ("test.iocoder.cn", "localhost"),
    ("simple@iocoder.cn", "simple@example.com"),
    ("yuanma@iocoder.cn", "yuanma@example.com"),
    ("/Users/yunai/tmp/file", "./data/file"),
    ("bucket\":\"ruoyi-vue-pro-private", "bucket\":\"your-private-bucket"),
    ("bucket\":\"ruoyi-vue-pro", "bucket\":\"your-bucket"),
    ("作者动态", "外部链接"),
    ("芋道的公众", "系统公告"),
    ("芋道源码", "Simple"),
    ("芋艿", "Admin"),
    ("芋道1", "User"),
    ("芋道", "Simple"),
    ("ruoyi-vue-pro", "simple-project"),
]

JAVA_AUTHOR = ("@author 芋道源码", "@author Simple Boot")


def patch_mysql_file_config(text: str) -> str:
    text = text.replace(
        "VALUES (22, '七牛存储器（示例）', 20, '请换成你自己的密钥！！！', b'1'",
        "VALUES (22, '七牛存储器（示例）', 20, '请换成你自己的密钥！！！', b'0'",
    )
    text = text.replace(
        "VALUES (29, '本地存储（示例）', 10, 'mac/linux 使用 /，windows 使用 \\\\', b'0'",
        "VALUES (29, '本地磁盘', 10, '默认主存储器，路径相对运行目录', b'1'",
    )
    # drop junk config id=35
    lines = []
    for line in text.splitlines(keepends=True):
        if "infra_file_config" in line and "VALUES (35," in line:
            continue
        lines.append(line)
    return "".join(lines)


def patch_other_file_master(text: str) -> str:
    # dialect dumps use '1'/'0' strings for master
    text = text.replace(
        "七牛存储器（示例）', 20, '请换成你自己的密钥！！！', '1'",
        "七牛存储器（示例）', 20, '请换成你自己的密钥！！！', '0'",
    )
    text = text.replace(
        "七牛存储器（示例）', 20, N'请换成你自己的密钥！！！', N'1'",
        "七牛存储器（示例）', 20, N'请换成你自己的密钥！！！', N'0'",
    )
    text = text.replace(
        "本地存储（示例）', 10, 'mac/linux 使用 /，windows 使用 \\', '0'",
        "本地磁盘', 10, '默认主存储器', '1'",
    )
    text = text.replace(
        "本地存储（示例）', 10, N'mac/linux 使用 /，windows 使用 \\', N'0'",
        "本地磁盘', 10, N'默认主存储器', N'1'",
    )
    return text


def main() -> None:
    sql_files = list((BOOT / "sql").rglob("*.sql"))
    for path in sql_files:
        if path.name.lower() == "license":
            continue
        raw = path.read_text(encoding="utf-8")
        text = raw
        if path.name.startswith("simple-boot"):
            if "mysql" in str(path).replace("\\", "/"):
                text = patch_mysql_file_config(text)
            else:
                text = patch_other_file_master(text)
        for old, new in SQL_REPLACEMENTS:
            text = text.replace(old, new)
        if text != raw:
            path.write_text(text, encoding="utf-8")
            print(f"patched sql {path}")

    java_files = list(BOOT.rglob("*.java"))
    for path in java_files:
        if "target" in path.parts:
            continue
        raw = path.read_text(encoding="utf-8")
        text = raw.replace(*JAVA_AUTHOR)
        text = re.sub(r"https://gitee\.com/\S+", "", text)
        text = text.replace("ruoyi-vue-pro", "simple-project")
        if text != raw:
            path.write_text(text, encoding="utf-8")
            print(f"patched java {path.relative_to(BOOT)}")

    web = ROOT / "simple-boot-web"
    for path in list(web.rglob("*.vue")) + list(web.rglob("*.ts")) + list(web.rglob("*.js")):
        if "node_modules" in path.parts or "dist" in path.parts:
            continue
        raw = path.read_text(encoding="utf-8")
        text = (
            raw.replace("芋艿", "maintainer")
            .replace("ruoyi-vue3", "vue3")
            .replace("com.ruoyi.system", "cn.simple.boot.module.system")
            .replace("ruoyi-vue", "simple-boot")
        )
        if text != raw:
            path.write_text(text, encoding="utf-8")
            print(f"patched web {path.relative_to(web)}")


if __name__ == "__main__":
    main()
