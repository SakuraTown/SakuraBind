# SakuraBind

## 插件介绍

> SakuraBind源自自用的绑定插件，功能有限，经过一系列功能定制之后我决定完善其功能并发布出来。
>
> 本插件会尽量涵盖所有有用的功能，并提供丰富的配置尽可能满足您的所有需求

### 特点

* 支持 Minecraft 1.8+的服务端。包括 spigot、paper、甚至catserver等mod服务器端
* 多级配置。从权限到某类特殊物品的配置到全局统一配置灵活控制绑定物品行为
* 极致性能优化。通过内存TTL、NBT、文件存储三级缓存以减少物品匹配性能损耗,并利用布谷鸟过滤器处理缓存穿透问题
* 完美方块物品支持。支持多方快结构、装饰物，无论是流体、爆炸、活塞、丢失支撑方块等造成的方块物品变成掉落物绑定依旧存在
* 丢失物品找回功能。掉落物掉虚空、仙人掌、岩浆、被他人拿走甚至在容器中被破坏，都可以将物品送回玩家背包中，如玩家不在线则存入暂存箱中
* 全面PlaceHolderAPI支持。任何看得见的地方都能使用papi，如消息、lore
* 自动绑定功能。
* 多数据库支持(暂存箱功能)。支持 SQLite(本地|默认)、MySQL、MariaDB、Oracle、PostgreSQL、SQLServer
* ......

### 要求

* 具有 SpigotAPI 1.8以上或其下游版本的服务端
* Java8 或以上
* 第一次加载插件需要联网下载依赖

### 依赖信息

* Kotlin 1.7.21 自动下载
* Exposed 0.41.1 自动下载
* EhCache 3.10.6 自动下载
* HikariCP 4.0.3 自动下载
* 数据库依赖 视选择的数据库类型自动下载
* NBTEditor 7.18.3 已打包进插件
* bstats-bukkit 3.0.0 已打包进插件
* PlaceHolderAPI 2.11.2 软依赖插件
* AuthMe 5.6.0-SNAPSHOT 软依赖插件
* SakuraMail 软依赖插件

## 插件安装

将插件放入服务端根目录`plugins`文件夹中，重启服务器或使用热重载插件加载即可，第一次加载插件将会联网下载依赖，所以第一次加载请勿热加载否则会卡住主线程。

## 插件配置

插件的配置文件夹为 `SakuraBind`,目录里的

`config.yml`为插件核心配置、

`global-setting.yml` 为全局的绑定设置

`settings.yml`里可以添加自定义规则，用于匹配某类物品应用不同的绑定设置

`lang.yml` 里是所有插件消息，可以自行更改，支持 papi 和颜色字符`&`,1.17及以上支持16进制颜色如`#66ccff`

`database.yml`为数据库相关设置

`data`文件夹为本地缓存，储存方块物品的绑定信息，删除之后已放置的方块物品将丢失绑定信息

每个配置文件都支持`自动重载`修改完毕只需要`保存`插件就会自动重载配置。

每个配置文件里都有`详细的注释`，这里就不再重复说明

`global-setting.yml`配置内容如下

~~~ yam

# 本配置为绑定的全局权限设置，优先级最低
# 在布尔类型的选项之后加上@则表示对于物主采取相反的结果,部分没有提示消息的无效
# 如 item-deny.click@: true 表示仅允许物主拿走容器内的物品
# 如 item-deny.drop: true 表示禁止所有人丢弃绑定物品
# 如 block-deny.place@: true 表示禁止所有人放置方块，但允许物主放置
global-setting: ''

# 显示的lore,玩家名称占位符为 %player%
lore:
- '&a灵魂绑定: &6%player%'

# 显示的lore位置
lore-index: 0

# 当物品丢失时(掉虚空、消失等)归还物主(在线则发背包，否则发邮件)
send-when-lost: true

# 当容器被破坏时将绑定物品归还物主(在线则发背包，否则发邮件)
send-when-container-break: true

# 当物品作为掉落物时延迟多少tick还物主(在线则发背包，否则发邮件), 0表示立马返回，-1关闭
send-back-delay: -1


# 物品禁用设置
item-deny:

  # 手上拿着绑定物品时禁止交互(点击方块)
  interact@: true

  # 禁止实体交互(攻击或右键)
  interact-entity@: true

  # 禁止丢弃
  drop: true

  # 禁止含有绑定物品的容器被玩家破坏
  container-break: false

  # 禁止捡起
  pickup@: true

  # 禁止拿走绑定物品
  click@: true

  # 禁止铁砧(1.9以上)
  anvil: true

  # 禁止合成
  craft: true

  # 禁止发射器射出
  dispense: true

  # 掉落物禁止被漏斗或漏斗矿车吸入
  hopper: true

  # 容器里的绑定物品不被漏斗或漏斗矿车吸走
  container-move: true

  # 禁止放入展示框
  item-frame: true

  # 禁止右键丢出(药水、雪球等投掷物)
  throw: true

  # 禁止消耗(吃)
  consume: true

  # 手上拿着绑定物品时禁止输入一下匹配命令
  command: false

  # 匹配的命令正则表达式: '.*' 表示全部。测试: https://www.bejson.com/othertools/regex/
  command-pattern:
  - .*

  # 禁止绑定物品放入特定标题的容器里
  inventory: true

  # 禁止绑定物品放入特定标题的容器里,正则表达式
  inventory-pattern:
  - ^垃圾桶$


# 方块物品相关设置
# 由于监听方块物品需要较多的资源，如果不绑定方块物品关闭以节省性能
block-deny:

  # 禁止方块物品被破坏
  break@: true

  # 禁止方块物品被放置
  place@: true

  # 禁止方块物品被互动(左右键)
  interact@: true

  # 禁止方块物品被爆炸损坏
  explode: true

  # 禁止方块物品被活塞推动/拉动
  piston: true

  # 禁止流水/岩浆破坏,如关闭被冲走的绑定物品将送回玩家或发邮件
  flow: true


# 自动绑定设置
auto-bind:

  # 是否开启自动绑定,如果全局开启将会绑定所有物品，请在setting.yml中配置开启以绑定特殊物品
  enable: false

  # 点击物品时绑定
  onClick: true

  # 捡起物品时绑定
  onPickup: true

  # 丢弃物品时绑定
  onDrop: false

# 扫描玩家时如果发现不属于这个玩家的物品则送回去
scanner-send-back: true

~~~

绑定设置的读取按照优先级从高到低分别为 `权限`、`匹配设置`、`全局设置`

如玩家具有 权限设置的格式为 `sakurabind.settings.{键名}.true|false`

如禁止玩家放置方块物品的权限为 `sakurabind.settings.block-deny.place.true`

对应的全局设置为

~~~ yaml
block-deny:
  place: true
~~~

在 `settings.yml`声明的匹配设置会覆盖全局设置，未声明的设置将继承全局设置

匹配设置的作用是将某些特殊物品区分开来，自定义程度更高,

比如你可以这样设置 所有剑都自动绑定

`settings.yml`

~~~ yaml
# matcher可以匹配某类特殊的物品以应用不同的设置
# 请勿声明名为 'global-setting '的matcher,否则会与公共设置冲突
# match 项为需要匹配的物品特征，采用正则表达式 https://www.bejson.com/othertools/regex/
# 所有 match 项都不是必须的，你可以自由组合, 但至少需要有一个子项, 只有匹配所有子项才算最终匹配到
# match 项下的 name 为 物品名字, 必须为非原版翻译名(也就是从创造物品栏拿出来的'圆石'的name为空)
# match 项下的 name-without-color 为 除去颜色代码的物品名字, 必须为非原版翻译名(也就是从创造物品栏拿出来的'圆石'的name为空)
# match 项下的 material 为 物品材质,使用正则匹配
# match 项下的 materials 为 物品材质,使用全名匹配 https://bukkit.windit.net/javadoc/org/bukkit/Material.html
# match 项下的 ids 为 物品id:子id 匹配方式 如 6578 或 6578:2
# match 项下的 materialIds 为 物品材质:子ID 匹配方式 如 STONE 或 STONE:2 ; 如果只需要匹配材质请使用效率更高的 materials 方式
# match 项下的 lore 为 物品lore 如有多行则需全匹配
# match 项下的 lore-without-color 为 物品lore除去颜色代码 如有多行则需全匹配 与 lore 互斥
# match 项下的 nbt 为 物品NBT 

# settings 项为此matcher独立的设置，完全兼容global-setting中的选项
# settings 项中以 '@' 结尾的布尔类型的项，其物主将使用与他人相反的设置
#          如 block-deny 下的 break: true 表示所有人都不能破坏此方块物品
#          但如果是 break@: true 表示所有人都不能破坏,但物主可以破坏此方块物品
# settings 中不存在的项将继承 global-setting.yml 中的同名项
readme: ''
# 为提高性能，匹配过一次的物品在绑定之后将会把匹配到的设置键存入物品NBT，此为NBT的路径 '.' 为路径分隔符
nbt-cache-path: sakura_bind_setting_cache
matchers:
  autobind: # 这个键名随意取，不冲突就行，但是不能为 global-setting
    match:
      material: 'SWORD$' #正则表达式匹配所有 SWORD 结尾的材质
    settings:
      send-back-delay: 100 #丢弃100tick(5秒)之后返还物主
      auto-bind:
        enable: true  # 开启自动绑定
      lore:
        - "&a这是专属于 &6%player% &a的宝剑" #独立于全局设置的lore
      item-deny:
        drop: true    #禁止丢弃
        interact-entity@: true  #禁止实体左右键，但是主人可以
~~~

## 插件命令

根命令全称 `sakurabind`,别名为 `sBind`, `sb`, `sab`, `bind`

~~~ text
/sakurabind bind <player> [-noLore]  绑定某玩家手上的物品
/sakurabind bindTo <player> [-noLore]  绑定手上的物品给某玩家
/sakurabind unBind <player>  解绑定某玩家手上的物品
/sakurabind bindAll <player> [-noLore]  绑定某玩家背包里的所有物品
/sakurabind unBindAll <player>  解绑定某玩家背包的物品
/sakurabind getLost  获取暂存箱物品
/sakurabind autoBind  给手上的物品添加自动绑定的NBT
/sakurabind debug  切换debug模式
~~~

在任何命令后面加上 `-silent` 可以隐藏消息

## 插件变量

插件有的变量暂时只有一个 `%sakurabind_has_lost%` 判断玩家暂存箱是否有物品,为数据库查询操作，请勿高频使用

## 插件权限

### 命令

权限格式为 `sakurabind.节点名称` 如 `/sakurabind bind`命令的权限为`sakurabind.sakurabind.bind`

玩家默认拥有 `/sakurabind getLost` 的权限，其他均为OP权限

`sakurabind.sakurabind.*`所有权限的命令

### 其他

绑定权限`sakurabind.bypass.all`,与OP一致不受绑定限制，但没有命令的权限

绑定全局设置权限 `sakurabind.settings.{键名}.true|false` 不支持`键名@`的形式

绑定设置权限 `sakurabind.setting.{设置名}.{键名}.true|false` 设置名为 `settings.yml` 匹配键,覆盖全局权限 不支持`键名@`
的形式

如禁止玩家放置方块物品的权限为 `sakurabind.settings.block-deny.place.true`

绑定设置权限`sakurabind.bypass.{玩家uuid}`拥有此权限将享受与该uuid物主相同的权限，即可以多人共用一个绑定物品

## 已知问题

* spigot1.12.2以下的版本由于sqlitejdbc旧版本被内嵌在服务端内，导致无法使用，请使用非sqlite连接数据库
* spigot1.8.x 版本的配置编码会乱码，请自行更改编码或在服务端启动参数里加上 `-Dfile.encoding=UTF-8`
* 部分mod服务端的漏斗、发射器禁用功能失效
