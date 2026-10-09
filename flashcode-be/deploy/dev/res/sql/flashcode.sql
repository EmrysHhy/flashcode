CREATE TABLE `app` (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '应用主键ID',
  `user_id` bigint NOT NULL COMMENT '所属用户主键ID',
  `app_name` varchar(10) NOT NULL COMMENT '应用名称',
  `app_desc` varchar(200) NOT NULL COMMENT '应用描述',
  `app_doc` text DEFAULT NULL COMMENT '应用需求文档',
  `app_preview_url` varchar(100) DEFAULT NULL COMMENT '应用预览地址',
  `deploy_status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '部署状态：0=未部署，1=已部署',
  `app_url` varchar(100) DEFAULT NULL COMMENT '部署成功后应用访问网址',
  `app_type` tinyint(1) NOT NULL DEFAULT '0' COMMENT '应用类型：0=html, 1=vue3, 2=vue3_spring',
  `app_screenshot` varchar(250) DEFAULT NULL COMMENT '应用截图',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更改时间',
  PRIMARY KEY (`id`),
  KEY `idx_apps_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10000001 DEFAULT CHARSET=utf8mb4 COMMENT='应用信息表'
;

CREATE TABLE `chat_history` (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '聊天历史记录自增id',
  `app_id` bigint NOT NULL COMMENT '所属应用主键ID',
  `chat_role` tinyint(1) NOT NULL COMMENT '用户角色：0=用户, 1=大模型',
  `content` mediumtext NOT NULL COMMENT '聊天历史记录',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除：0=未删除, 1=已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更改时间',
  PRIMARY KEY (`id`),
  KEY `idx_chat_histtory_app` (`app_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10000001 DEFAULT CHARSET=utf8mb4 COMMENT='聊天历史记录表'
;