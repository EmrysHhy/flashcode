# 1. 初始化脚手架相关的业务表
# 注意: 此前如果修改过脚手架业务数据库名称，此处需确保名称一致

SET NAMES utf8mb4;

use `flashcode_dev`;


drop table if exists `sys_argument`;
create table `sys_argument` (
                                `id` bigint(20) unsigned not null auto_increment primary key comment '自增主键',
                                `name` varchar(64) default '' comment '参数名称',
                                `config_key` varchar(64) default '' comment '参数键名',
                                `value` varchar(1024) default '' comment '参数键值',
                                `remark` varchar(64) default '' comment '备注',
                                unique index idx_config_key (`config_key`)
) engine = innodb auto_increment = 10000001 character set = utf8mb4 comment = '系统参数表';

INSERT INTO sys_argument (name,config_key,value) VALUES
    ('热门城市','sys_hot_city','35,108,234,236,289,342');

drop table if exists `sys_dictionary_type`;
create table `sys_dictionary_type` (
                                   `id` bigint(20) unsigned not null auto_increment primary key comment '自增主键',
                                   `type_key` varchar(64) default '' comment '字典类型键',
                                   `value` varchar(64) default '' comment '字典类型值',
                                   `remark` varchar(64) default '' comment '备注',
                                   `status` tinyint(1) default 1 comment '字典类型状态 1正常 0停用',
                                   unique index idx_type_key (`type_key`)
) engine = innodb auto_increment = 10000001 character set = utf8mb4 comment = '字典类型表';

INSERT INTO sys_dictionary_type (type_key,value,remark,status) VALUES
                                                                                     ('admin','管理员','sit',1),
                                                                                     ('common_status','公共状态','',1);

drop table if exists `sys_dictionary_data`;
create table `sys_dictionary_data` (
                                       `id` bigint(20) unsigned not null auto_increment primary key comment '自增主键',
                                       `type_key` varchar(64) default '' comment '字典类型键',
                                       `data_key` varchar(64) default '' comment '字典数据键',
                                       `value` varchar(64) default '' comment '字典数据值',
                                       `remark` varchar(64) default '' comment '备注',
                                       `sort` int(11) default 1 comment '排序',
                                       `status` tinyint(1) default 1 comment '字典数据状态 1正常 0停用',
                                       key idx_type_key (`type_key`),
                                       unique index ui(`type_key`, `data_key`)
) engine = innodb auto_increment = 10000001 character set = utf8mb4 comment = '字典数据表';

INSERT INTO sys_dictionary_data (type_key,data_key,value,remark,sort,status) VALUES
                                                                                 ('admin','super_admin','超级管理员','ipsum',1,1),
                                                                                 ('admin','platform_admin','平台管理员','ipsum',1,1),
                                                                                 ('common_status','enable','启用','',1,1),
                                                                                 ('common_status','disable','停用','',1,1);


CREATE TABLE `app_user`  (
                             `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增主键',
                             `nick_name` varchar(64) NULL DEFAULT NULL COMMENT '昵称',

                             `phone_number` varchar(64) NULL DEFAULT NULL COMMENT '电话',
                             `email` varchar(255) NULL DEFAULT NULL COMMENT '邮箱',
                             `open_id` varchar(64) NULL DEFAULT NULL COMMENT '微信openId',
                             `avatar` varchar(255) NULL DEFAULT NULL COMMENT '头像',
                             PRIMARY KEY (`id`) USING BTREE,
                             UNIQUE INDEX `uk_phone`(`phone_number`) USING BTREE,
                             UNIQUE INDEX `uk_email`(`email`) USING BTREE,
                             UNIQUE INDEX `uk_open_id`(`open_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10000001 CHARACTER SET = utf8mb4 COMMENT = '应用端人员表';

-- sys_user definition

CREATE TABLE `sys_user` (
                            `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
                            `nick_name` varchar(64) NOT NULL COMMENT '昵称',
                            `phone_number` varchar(64) NOT NULL COMMENT '电话',
                            `password` varchar(255) NOT NULL COMMENT '密码',
                            `identity` varchar(16) NOT NULL COMMENT '身份',
                            `remark` varchar(50) DEFAULT NULL COMMENT '备注',
                            `status` varchar(10) NOT NULL COMMENT '状态',
                            PRIMARY KEY (`id`) USING BTREE,
                            UNIQUE KEY `uk_phone` (`phone_number`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10000001 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='管理端人员表';

INSERT INTO sys_user (nick_name,phone_number,password,`identity`,remark,status) VALUES
    ('admin','e64c5f44dc95e4ca77d99136ea2c88c6','15e2b0d3c33891ebb0f1ef609ec419420c20e320ce94c65fbc8c3312448eb225','super_admin',NULL,'enable');

commit ;