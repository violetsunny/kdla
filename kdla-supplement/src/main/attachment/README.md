### kdla-sequence-no-starter
> 本starter工具包，提供了便捷的序列编号生成能力，引入本jar包后即可通过注入使用bean SequenceNoGenerator，直接使用该能力。
序号生成可以一次批量生成，缓存在本地，缓存号码耗尽后才会再次生成新号，该功能可以通过表中配置字段来配置。
目前更新到了V2版本，直接使用V2生成器调用方法即可。
然鹅，V2版本对表结构、规则都做了极大的改动，如果要使用V2，请先升级表结构，并且确保规则为V2的格式。

* 1.依赖与限制
> 本starter包，依赖了MySQL、Redis 中间件，操作媒介是 mybatis、redisson。
如果要使用本starter包，需要在自己的MySQL数据库中新增一张配置表，用来记录配置编号生成规则。

    - 你也可以使用自己的RedissonClient，如果没有配置自己的RedissonClient，本包则会基于RDFA的配置创建一个RedissonClient。

* 2.使用SQL
   - 请参考SQL附件

* 3.配置
    - 3.1）top.kdla.framework.sequence.no.enable = true
    设置为true才能让本starter包生效
    - 3.2）top.kdla.framework.sequence.lock.key.prefix
    用来定义分布式锁的key值

* 4.规则配置
    以下规则介绍为V2版本
    - 4.1)规则样例
    ```
    {
        "epochRule":{
            "epochRuleTypeEnum":"DATE_TIME",
            "content":"yyyyMMddHH"
        },
        "rules":[
            {
                "order":1,
                "ruleType":"FIX",
                "ruleContent":{
                    "content":"TEST_"
                }
            },
            {
                "order":2,
                "ruleType":"DT",
                "ruleContent":{
                    "format":"yyyy-MM-dd"
                }
            },
            {
                "order":3,
                "ruleType":"SEQ",
                "ruleContent":{
                    "radix":"16",
                    "size":6
                }
            },
            {
                "order":5,
                "ruleType":"RAN",
                "ruleContent":{
                    "size":6,
                    "typeList":[
                        "UPPER_CASE",
                        "NUMBER"
                    ],
                    "customizePool":[
                        "@",
                        "*"
                    ]
                }
            },
            {
                "order":4,
                "ruleType":"FIX",
                "ruleContent":{
                    "content":"|"
                }
            }
        ]
    }
    ```
  - 4.2)规则解释
  - 4.2.1)epochRule 
>epoch纪元，所有SEQ序列增长，都是基于纪元的。相同的纪元epoch，序列持续递增。当epoch发生变化的时候，序列则重新开始增长。
>epochRule定义的是epoch的变化规则,分为两种类型，NONE代表纪元不发生变化。DATE_TIME代表纪元按照时间日期格式发生变化。
  - 4.2.2)rules
>rules定义了各种具体的生成规则。order代表该规则的位置顺序，ruleType为规则类型，ruleContent为规则内容。
>目前有四种规则类型。FIX（固定字符串）、RAN（随机字符串）、DT（时间日期字符串）、SEQ（序列增长字符串）