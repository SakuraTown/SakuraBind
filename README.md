# SakuraBind

[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=Iseason2000_SakuraBind&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=Iseason2000_SakuraBind)[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Iseason2000_SakuraBind&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=Iseason2000_SakuraBind)[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Iseason2000_SakuraBind&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=Iseason2000_SakuraBind)

## 插件介绍

> SakuraBind源自自用的绑定插件，功能有限，经过一系列功能定制之后我决定完善其功能并发布出来。
>
> 本插件会尽量涵盖所有有用的功能，并提供丰富的配置尽可能满足您的所有需求

### 特点

* 支持 Minecraft 1.8+的服务端。包括 spigot、paper、甚至catserver等mod服务器端
* 多级配置。从权限到某类特殊物品的配置到全局统一配置灵活控制绑定物品行为
* 极致性能优化。通过内存TTL、NBT、文件存储三级缓存以减少物品匹配性能损耗,并利用布谷鸟过滤器处理缓存穿透问题
* 方块绑定支持。支持多方快结构、装饰物，无论是流体、爆炸、活塞、丢失支撑方块等造成的方块物品变成掉落物绑定依旧存在
* 丢失物品找回功能。掉落物掉虚空、仙人掌、岩浆、被他人拿走甚至在容器中被破坏，都可以将物品送回玩家背包中，如玩家不在线则存入暂存箱中
* 全面PlaceHolderAPI支持。任何看得见的地方都能使用papi，如消息、lore
* 实体绑定功能, 支持刷怪蛋绑定实体
* 自动绑定功能, 名字、lore、材质、nbt多种方式自由组合识别绑定
* 多数据库支持(暂存箱)。支持 SQLite(本地|默认)、MySQL、MariaDB、Oracle、PostgreSQL、SQLServer
* 插件在设计之初就考虑兼容性，理论上兼容大部分插件，可以提issue兼容
* 监听器开关。可通过关闭不需要的功能减少性能损耗
* 物品 -> 方块 -> 实体 -> 物品 全链路追踪绑定

插件文档: [https://iseason2000.github.io/docs/category/sakurabind](https://iseason2000.github.io/docs/category/sakurabind)

![](https://bstats.org/signatures/bukkit/SakuraBind.svg)

