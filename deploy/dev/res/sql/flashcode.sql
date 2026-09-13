CREATE TABLE `app` (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '应用主键ID',
  `user_id` bigint NOT NULL COMMENT '所属用户主键ID',
  `app_name` varchar(10) NOT NULL COMMENT '应用名称',
  `app_desc` varchar(100) NOT NULL COMMENT '应用描述',
  `app_doc` text DEFAULT NULL COMMENT '应用需求文档',
  `app_preview_url` varchar(100) DEFAULT NULL COMMENT '应用预览地址',
  `app_type` tinyint(1) NOT NULL DEFAULT '0' COMMENT '应用类型：0=html, 1=vue3, 2=vue3_spring',
  `app_screenshot` varchar(100) DEFAULT NULL COMMENT '应用截图',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更改时间',
  PRIMARY KEY (`id`),
  KEY `idx_apps_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10000001 DEFAULT CHARSET=utf8mb4 COMMENT='应用信息表'