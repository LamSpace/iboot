-- iBoot 单库 baseline：由七个 schema 的迁移结果 squash 生成
-- 生成方式：空库分别执行各模块现有迁移后，按当前所有者合并结构与种子数据
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;
SET UNIQUE_CHECKS=0;
-- ===== iboot_admin =====

/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'é…ç½®ID',
  `config_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'é…ç½®åç§°',
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'é…ç½®é”®',
  `config_value` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'é…ç½®å€¼',
  `config_type` tinyint NOT NULL DEFAULT '0' COMMENT 'é…ç½®ç±»åž‹ï¼š0-é»˜è®¤ï¼Œ1-ç³»ç»Ÿ',
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ›´æ–°äºº',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤ï¼š0-æœªåˆ é™¤ï¼Œ1-å·²åˆ é™¤',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¤‡æ³¨',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=86 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ç³»ç»Ÿé…ç½®è¡¨';

LOCK TABLES `sys_config` WRITE;
/*!40000 ALTER TABLE `sys_config` DISABLE KEYS */;
INSERT INTO `sys_config` VALUES (1,'ç³»ç»Ÿåç§°','sys.name','iBoot åŽå°ç®¡ç†ç³»ç»Ÿ',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'ç³»ç»Ÿåç§°'),(2,'ç”¨æˆ·é»˜è®¤å¯†ç ','sys.user.initPassword','123456',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'ç”¨æˆ·é»˜è®¤å¯†ç '),(3,'è´¦å·è‡ªåŠ©æ³¨å†Œ','sys.account.registerUser','false',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'æ˜¯å¦å¼€å¯ç”¨æˆ·æ³¨å†ŒåŠŸèƒ½'),(4,'ç™»å½•éªŒè¯ç å¼€å…³','sys.account.captchaEnabled','true',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'æ˜¯å¦å¼€å¯ç™»å½•éªŒè¯ç '),(5,'å…¬å‘Šæœ€å¤§å±•ç¤ºæ•°é‡','sys.notice.maxDisplay','5',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'å¼¹çª—å’Œé¦–é¡µæœ€å¤§å±•ç¤ºå…¬å‘Šæ•°é‡'),(6,'å…¬å‘Šç½®é¡¶æ•°é‡é™åˆ¶','sys.notice.topCount','3',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'å…è®¸åŒæ—¶ç½®é¡¶çš„æœ€å¤§å…¬å‘Šæ•°é‡'),(7,'æ¶ˆæ¯ä¿ç•™å¤©æ•°','sys.message.retentionDays','90',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'ç«™å†…æ¶ˆæ¯ä¿ç•™å¤©æ•°ï¼Œè¶…è¿‡è¯¥å¤©æ•°çš„æ¶ˆæ¯å°†è¢«æ¸…ç†'),(8,'æ”¶ä»¶ç®±æœ€å¤§å±•ç¤ºæ•°é‡','sys.message.maxDisplay','50',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'ç”¨æˆ·æ”¶ä»¶ç®±å•é¡µæœ€å¤§å±•ç¤ºæ¶ˆæ¯æ•°é‡'),(9,'æ–‡ä»¶ä¸Šä¼ è·¯å¾„','sys.file.uploadPath','./uploads',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'æ–‡ä»¶ä¸Šä¼ çš„å­˜å‚¨æ ¹ç›®å½•ï¼Œæ”¯æŒç›¸å¯¹è·¯å¾„å’Œç»å¯¹è·¯å¾„'),(10,'æ–‡ä»¶ä¸Šä¼ å¤§å°é™åˆ¶','sys.file.maxSize','50',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'å•ä¸ªæ–‡ä»¶ä¸Šä¼ æœ€å¤§å¤§å°ï¼Œå•ä½MB'),(11,'å…è®¸ä¸Šä¼ çš„æ–‡ä»¶æ‰©å±•å','sys.file.allowedExtensions','doc,docx,xls,xlsx,ppt,pptx,pdf,txt,md,jpg,jpeg,png,gif,bmp,svg,mp4,avi,mov,mp3,wav,zip,rar,7z,tar,gz',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'å…è®¸ä¸Šä¼ çš„æ–‡ä»¶æ‰©å±•ååˆ—è¡¨ï¼Œé€—å·åˆ†éš”'),(12,'ä¼šè¯è¶…æ—¶æ—¶é—´','sys.session.timeout','30',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'ç”¨æˆ·ä¼šè¯è¶…æ—¶æ—¶é—´ï¼Œå•ä½åˆ†é’Ÿ'),(13,'ç‹¬å ä¼šè¯å¼€å…³','sys.session.exclusive','true',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'æ˜¯å¦å¼€å¯ä¼šè¯äº’æ–¥ï¼ŒåŒä¸€ç”¨æˆ·åŒæ—¶åªèƒ½ä¿æŒä¸€ä¸ªæ´»è·ƒä¼šè¯'),(14,'ç™»å½•æœ€å¤§é‡è¯•æ¬¡æ•°','sys.login.maxRetry','5',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'è¿žç»­ç™»å½•å¤±è´¥è¾¾åˆ°è¯¥æ¬¡æ•°åŽé”å®šè´¦æˆ·'),(15,'ç™»å½•é”å®šæ—¶é—´','sys.login.lockTime','10',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'ç™»å½•å¤±è´¥é”å®šçš„æŒç»­æ—¶é—´ï¼Œå•ä½åˆ†é’Ÿ'),(16,'Rediså†…å­˜è­¦å‘Šé˜ˆå€¼','monitor.redis.memory.warn.threshold','70',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'Rediså†…å­˜ä½¿ç”¨çŽ‡è¾¾åˆ°è¯¥ç™¾åˆ†æ¯”æ—¶æ˜¾ç¤ºè­¦å‘Š'),(17,'Rediså†…å­˜é”™è¯¯é˜ˆå€¼','monitor.redis.memory.error.threshold','85',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'Rediså†…å­˜ä½¿ç”¨çŽ‡è¾¾åˆ°è¯¥ç™¾åˆ†æ¯”æ—¶æ˜¾ç¤ºé”™è¯¯'),(18,'CPUè­¦å‘Šé˜ˆå€¼','monitor.cpu.warn.threshold','80',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'CPUä½¿ç”¨çŽ‡è¾¾åˆ°è¯¥ç™¾åˆ†æ¯”æ—¶æ˜¾ç¤ºè­¦å‘Š'),(19,'CPUé”™è¯¯é˜ˆå€¼','monitor.cpu.error.threshold','90',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'CPUä½¿ç”¨çŽ‡è¾¾åˆ°è¯¥ç™¾åˆ†æ¯”æ—¶æ˜¾ç¤ºé”™è¯¯'),(20,'ç‰©ç†å†…å­˜è­¦å‘Šé˜ˆå€¼','monitor.memory.warn.threshold','80',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'ç‰©ç†å†…å­˜ä½¿ç”¨çŽ‡è¾¾åˆ°è¯¥ç™¾åˆ†æ¯”æ—¶æ˜¾ç¤ºè­¦å‘Š'),(21,'ç‰©ç†å†…å­˜é”™è¯¯é˜ˆå€¼','monitor.memory.error.threshold','90',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'ç‰©ç†å†…å­˜ä½¿ç”¨çŽ‡è¾¾åˆ°è¯¥ç™¾åˆ†æ¯”æ—¶æ˜¾ç¤ºé”™è¯¯'),(22,'JVMå†…å­˜è­¦å‘Šé˜ˆå€¼','monitor.jvm.warn.threshold','80',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'JVMå †å†…å­˜ä½¿ç”¨çŽ‡è¾¾åˆ°è¯¥ç™¾åˆ†æ¯”æ—¶æ˜¾ç¤ºè­¦å‘Š'),(23,'JVMå†…å­˜é”™è¯¯é˜ˆå€¼','monitor.jvm.error.threshold','90',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'JVMå †å†…å­˜ä½¿ç”¨çŽ‡è¾¾åˆ°è¯¥ç™¾åˆ†æ¯”æ—¶æ˜¾ç¤ºé”™è¯¯'),(24,'ç£ç›˜ç©ºé—´è­¦å‘Šé˜ˆå€¼','monitor.disk.warn.threshold','80',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'ç£ç›˜ç©ºé—´ä½¿ç”¨çŽ‡è¾¾åˆ°è¯¥ç™¾åˆ†æ¯”æ—¶æ˜¾ç¤ºè­¦å‘Š'),(25,'ç£ç›˜ç©ºé—´é”™è¯¯é˜ˆå€¼','monitor.disk.error.threshold','90',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'ç£ç›˜ç©ºé—´ä½¿ç”¨çŽ‡è¾¾åˆ°è¯¥ç™¾åˆ†æ¯”æ—¶æ˜¾ç¤ºé”™è¯¯'),(26,'Spring Boot Adminåœ°å€','monitor.sba.url','/sba',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'ç›‘æŽ§é¡µé¢ä¸­çš„Spring Boot AdminæŽ§åˆ¶å°é“¾æŽ¥åœ°å€'),(27,'Swagger UIåœ°å€','sys.swagger.ui.url','/swagger-ui.html',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'Swagger APIæ–‡æ¡£çš„è®¿é—®åœ°å€'),(28,'Swagger UIå¼€å…³','sys.swagger.enabled','true',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'æ˜¯å¦å¯ç”¨Swagger APIæ–‡æ¡£ï¼Œç”Ÿäº§çŽ¯å¢ƒå»ºè®®å…³é—­'),(29,'APIæ–‡æ¡£åœ°å€','sys.swagger.api-docs.url','/v3/api-docs',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'OpenAPIæŽ¥å£æ–‡æ¡£çš„åŸºç¡€è·¯å¾„ï¼Œå¯¹åº”springdoc.api-docs.pathé…ç½®'),(30,'Druidç›‘æŽ§åœ°å€','sys.druid.monitor.url','/druid',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'Druidæ•°æ®åº“è¿žæŽ¥æ± ç›‘æŽ§é¡µé¢åœ°å€'),(31,'Redisç›‘æŽ§è‡ªåŠ¨åˆ·æ–°é—´éš”','monitor.redis.refreshInterval','30',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'Redisç¼“å­˜ç›‘æŽ§é¡µé¢è‡ªåŠ¨åˆ·æ–°é—´éš”ï¼Œå•ä½ç§’'),(32,'åœ¨çº¿ç”¨æˆ·ç›‘æŽ§åˆ·æ–°é—´éš”','sys.online.refreshInterval','30',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'åœ¨çº¿ç”¨æˆ·ç›‘æŽ§é¡µé¢è‡ªåŠ¨åˆ·æ–°é—´éš”ï¼Œå•ä½ç§’'),(33,'æ€§èƒ½ç›‘æŽ§è‡ªåŠ¨åˆ·æ–°é—´éš”','monitor.performance.refreshInterval','30',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'ç³»ç»Ÿæ€§èƒ½ç›‘æŽ§é¡µé¢è‡ªåŠ¨åˆ·æ–°é—´éš”ï¼Œå•ä½ç§’'),(50,'æ–‡ä»¶å­˜å‚¨æ–¹å¼','sys.file.storageType','local',1,'system','2026-11-01 15:27:10',NULL,NULL,0,'æ–‡ä»¶å­˜å‚¨æ–¹å¼ï¼šlocal-æœ¬åœ°å­˜å‚¨ï¼Œminio-MinIOå¯¹è±¡å­˜å‚¨ï¼Œé»˜è®¤local'),(51,'MinIOæœåŠ¡ç«¯ç‚¹','sys.file.minio.endpoint','http://localhost:9000',1,'system','2026-11-01 15:27:10',NULL,NULL,0,'MinIOæœåŠ¡å™¨åœ°å€ï¼Œæ ¼å¼ï¼šhttp://host:port æˆ– https://host:port'),(52,'MinIOè®¿é—®å¯†é’¥','sys.file.minio.accessKey','minioadmin',1,'system','2026-11-01 15:27:10',NULL,NULL,0,'MinIOè®¿é—®å¯†é’¥IDï¼ˆAccess Keyï¼‰'),(53,'MinIOç§˜å¯†å¯†é’¥','sys.file.minio.secretKey','minioadmin',1,'system','2026-11-01 15:27:10',NULL,NULL,0,'MinIOç§˜å¯†è®¿é—®å¯†é’¥ï¼ˆSecret Keyï¼‰ï¼Œç”Ÿäº§çŽ¯å¢ƒè¯·ä½¿ç”¨å¼ºå¯†ç '),(54,'MinIOå­˜å‚¨æ¡¶åç§°','sys.file.minio.bucketName','iboot-files',1,'system','2026-11-01 15:27:10',NULL,NULL,0,'MinIOå­˜å‚¨æ¡¶åç§°ï¼Œå¿…é¡»ç¬¦åˆDNSå‘½åè§„èŒƒï¼ˆå°å†™å­—æ¯ã€æ•°å­—ã€è¿žå­—ç¬¦ï¼‰'),(55,'MinIOé¢„ç­¾åURLæœ‰æ•ˆæœŸ','sys.file.minio.urlExpireSeconds','3600',1,'system','2026-11-01 15:27:10',NULL,NULL,0,'é¢„ç­¾åURLæœ‰æ•ˆæœŸï¼ˆç§’ï¼‰ï¼Œé»˜è®¤3600ç§’ï¼ˆ1å°æ—¶ï¼‰ï¼ŒèŒƒå›´60-604800'),(56,'MinIOæŽ§åˆ¶å°åœ°å€','sys.minio.console.url','http://localhost:9001',1,'system','2026-11-01 15:27:10',NULL,NULL,0,'MinIOç®¡ç†æŽ§åˆ¶å°è®¿é—®åœ°å€'),(57,'Prometheusç›‘æŽ§åœ°å€','sys.prometheus.monitor.url','http://localhost:9090',1,'system','2026-11-01 15:27:10',NULL,NULL,0,'Prometheusç›‘æŽ§é¡µé¢åœ°å€'),(58,'Grafanaçœ‹æ¿åœ°å€','sys.grafana.monitor.url','http://localhost:3000',1,'system','2026-11-01 15:27:10',NULL,NULL,0,'Grafanaå¯è§†åŒ–çœ‹æ¿åœ°å€'),(59,'Kibana UIåœ°å€','sys.kibana.monitor.url','http://localhost:5601',1,'system','2026-11-01 15:27:10',NULL,NULL,0,'Kibanaæ—¥å¿—åˆ†æžç•Œé¢åœ°å€'),(60,'Alertmanager å‘Šè­¦ç®¡ç†åœ°å€','sys.alertmanager.monitor.url','http://localhost:9093',1,'system','2026-11-01 15:27:10',NULL,NULL,0,'Alertmanager å‘Šè­¦ç®¡ç†é¡µé¢åœ°å€'),(63,'Thanos æŸ¥è¯¢åœ°å€','sys.thanos.query.url','http://localhost:19192',1,'system','2026-11-01 15:27:12',NULL,NULL,0,'Thanos Query æŸ¥è¯¢é¡µé¢åœ°å€'),(83,'OSSæœåŠ¡ç«¯ç‚¹','sys.file.oss.endpoint','https://oss-cn-hangzhou.aliyuncs.com',1,'system','2026-11-01 15:27:12',NULL,NULL,0,'é˜¿é‡Œäº‘OSS Endpointï¼ŒæŒ‰å®žé™…åœ°åŸŸå¡«å†™ï¼Œæ ¼å¼ï¼šhttps://oss-cn-<region>.aliyuncs.com'),(84,'OSSå­˜å‚¨æ¡¶åç§°','sys.file.oss.bucketName','iboot-files',1,'system','2026-11-01 15:27:12',NULL,NULL,0,'é˜¿é‡Œäº‘OSS Bucketåç§°ï¼Œéœ€é¢„å…ˆåˆ›å»ºï¼Œåº”ç”¨ä¸ä¼šè‡ªåŠ¨å»ºæ¡¶'),(85,'OSSé¢„ç­¾åURLæœ‰æ•ˆæœŸ','sys.file.oss.urlExpireSeconds','3600',1,'system','2026-11-01 15:27:12',NULL,NULL,0,'OSSé¢„ç­¾åURLæœ‰æ•ˆæœŸï¼ˆç§’ï¼‰ï¼Œé»˜è®¤3600ç§’ï¼ˆ1å°æ—¶ï¼‰');
/*!40000 ALTER TABLE `sys_config` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'éƒ¨é—¨ID',
  `parent_id` bigint DEFAULT '0' COMMENT 'çˆ¶éƒ¨é—¨ID',
  `dept_name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'éƒ¨é—¨åç§°',
  `dept_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'éƒ¨é—¨ç¼–ç ',
  `order_num` int NOT NULL DEFAULT '0' COMMENT 'æ˜¾ç¤ºé¡ºåº',
  `leader` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'è´Ÿè´£äºº',
  `phone` varchar(11) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'è”ç³»ç”µè¯',
  `email` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'é‚®ç®±',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€ï¼š0-åœç”¨ï¼Œ1-æ­£å¸¸',
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ›´æ–°äºº',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤ï¼š0-æœªåˆ é™¤ï¼Œ1-å·²åˆ é™¤',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='éƒ¨é—¨è¡¨';

LOCK TABLES `sys_dept` WRITE;
/*!40000 ALTER TABLE `sys_dept` DISABLE KEYS */;
INSERT INTO `sys_dept` VALUES (1,0,'iBootç§‘æŠ€','IBOOT',0,'iBoot','15888888888','iboot@iboot.com',1,'system','2026-11-01 15:27:09',NULL,NULL,0),(2,1,'ç ”å‘éƒ¨é—¨','DEV',1,'ç ”å‘ç»ç†',NULL,NULL,1,'system','2026-11-01 15:27:09',NULL,NULL,0),(3,1,'å¸‚åœºéƒ¨é—¨','MARKET',2,'å¸‚åœºç»ç†',NULL,NULL,1,'system','2026-11-01 15:27:09',NULL,NULL,0);
/*!40000 ALTER TABLE `sys_dept` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'å­—å…¸æ•°æ®ID',
  `dict_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'å­—å…¸ç±»åž‹',
  `dict_label` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'å­—å…¸æ ‡ç­¾',
  `dict_value` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'å­—å…¸é”®å€¼',
  `dict_sort` int NOT NULL DEFAULT '0' COMMENT 'å­—å…¸æŽ’åº',
  `css_class` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ ·å¼å±žæ€§',
  `list_class` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'è¡¨æ ¼å›žæ˜¾æ ·å¼',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT 'æ˜¯å¦é»˜è®¤ï¼š0-å¦ï¼Œ1-æ˜¯',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€ï¼š0-åœç”¨ï¼Œ1-æ­£å¸¸',
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ›´æ–°äºº',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤ï¼š0-æœªåˆ é™¤ï¼Œ1-å·²åˆ é™¤',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¤‡æ³¨',
  PRIMARY KEY (`id`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=83 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='å­—å…¸æ•°æ®è¡¨';

LOCK TABLES `sys_dict_data` WRITE;
/*!40000 ALTER TABLE `sys_dict_data` DISABLE KEYS */;
INSERT INTO `sys_dict_data` VALUES (1,'sys_user_gender','æœªçŸ¥','0',1,NULL,NULL,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(2,'sys_user_gender','ç”·','1',2,NULL,NULL,0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(3,'sys_user_gender','å¥³','2',3,NULL,NULL,0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(4,'sys_show_hide','æ˜¾ç¤º','1',1,NULL,NULL,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(5,'sys_show_hide','éšè—','0',2,NULL,NULL,0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(6,'sys_normal_disable','æ­£å¸¸','1',1,NULL,NULL,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(7,'sys_normal_disable','åœç”¨','0',2,NULL,NULL,0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(8,'sys_user_status','æ­£å¸¸','1',1,NULL,NULL,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(9,'sys_user_status','åœç”¨','0',2,NULL,NULL,0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(10,'sys_role_status','æ­£å¸¸','1',1,NULL,NULL,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(11,'sys_role_status','åœç”¨','0',2,NULL,NULL,0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(12,'sys_notice_type','é€šçŸ¥','1',1,NULL,'warning',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(13,'sys_notice_type','å…¬å‘Š','2',2,NULL,'success',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(14,'sys_notice_status','è‰ç¨¿','0',1,NULL,'info',1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(15,'sys_notice_status','å·²å‘å¸ƒ','1',2,NULL,'success',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(16,'sys_notice_status','å·²æ’¤å›ž','2',3,NULL,'warning',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(17,'sys_message_type','ç³»ç»Ÿæ¶ˆæ¯','1',1,NULL,'primary',1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(18,'sys_message_type','å¾…åŠžæé†’','2',2,NULL,'warning',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(19,'sys_message_type','å®¡æ‰¹é€šçŸ¥','3',3,NULL,'success',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(20,'sys_message_type','é¢„è­¦é€šçŸ¥','4',4,NULL,'danger',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(21,'sys_message_priority','æ™®é€š','0',1,NULL,'info',1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(22,'sys_message_priority','é‡è¦','1',2,NULL,'warning',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(23,'sys_message_priority','ç´§æ€¥','2',3,NULL,'danger',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(24,'sys_message_status','è‰ç¨¿','0',1,NULL,'info',1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(25,'sys_message_status','å·²å‘é€','1',2,NULL,'success',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(26,'sys_message_status','å·²æ’¤å›ž','2',3,NULL,'warning',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(27,'sys_message_sender_type','ç³»ç»Ÿ','0',1,NULL,'primary',1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(28,'sys_message_sender_type','ç®¡ç†å‘˜','1',2,NULL,'success',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(29,'sys_message_receiver_type','å…¨éƒ¨ç”¨æˆ·','0',1,NULL,'primary',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(30,'sys_message_receiver_type','æŒ‡å®šç”¨æˆ·','1',2,NULL,'success',1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(31,'sys_file_category','æ–‡æ¡£','document',1,NULL,'',1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(32,'sys_file_category','å›¾ç‰‡','image',2,NULL,'success',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(33,'sys_file_category','è§†é¢‘','video',3,NULL,'warning',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(34,'sys_file_category','éŸ³é¢‘','audio',4,NULL,'info',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(35,'sys_file_category','åŽ‹ç¼©åŒ…','archive',5,NULL,'danger',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(36,'sys_file_category','å…¶ä»–','other',6,NULL,'info',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(37,'sys_job_group','é»˜è®¤','DEFAULT',1,NULL,'primary',1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(38,'sys_job_group','ç³»ç»Ÿ','SYSTEM',2,NULL,'warning',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(39,'sys_job_status','æš‚åœ','0',1,NULL,'danger',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(40,'sys_job_status','æ­£å¸¸','1',2,NULL,'success',1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(41,'sys_job_misfire','ç«‹å³æ‰§è¡Œ','1',1,NULL,'warning',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(42,'sys_job_misfire','æ‰§è¡Œä¸€æ¬¡','2',2,NULL,'info',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(43,'sys_job_misfire','æ”¾å¼ƒæ‰§è¡Œ','3',3,NULL,'primary',1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(44,'sys_job_concurrent','ç¦æ­¢','0',1,NULL,'danger',1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(45,'sys_job_concurrent','å…è®¸','1',2,NULL,'success',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(46,'sys_config_type','é»˜è®¤','0',1,NULL,'info',1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(47,'sys_config_type','ç³»ç»Ÿ','1',2,NULL,'primary',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(48,'sys_common_status','å¤±è´¥','0',1,NULL,'danger',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(49,'sys_common_status','æˆåŠŸ','1',2,NULL,'success',1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(50,'sys_business_type','å…¶å®ƒ','0',1,NULL,'info',1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(51,'sys_business_type','æ–°å¢ž','1',2,NULL,'primary',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(52,'sys_business_type','ä¿®æ”¹','2',3,NULL,'warning',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(53,'sys_business_type','åˆ é™¤','3',4,NULL,'danger',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(54,'sys_business_type','æŽˆæƒ','4',5,NULL,'success',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(55,'sys_business_type','å¯¼å‡º','5',6,NULL,'warning',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(56,'sys_business_type','å¯¼å…¥','6',7,NULL,'warning',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(57,'sys_business_type','å¼ºé€€','7',8,NULL,'danger',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(58,'sys_business_type','æ¸…ç©º','8',9,NULL,'danger',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(59,'sys_monitor_status','æ­£å¸¸','UP',1,NULL,'success',1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(60,'sys_monitor_status','è­¦å‘Š','WARN',2,NULL,'warning',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(61,'sys_monitor_status','å¼‚å¸¸','DOWN',3,NULL,'danger',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(62,'sys_monitor_type','æ•°æ®åº“','db',1,NULL,'primary',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(63,'sys_monitor_type','Redisç¼“å­˜','redis',2,NULL,'danger',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(64,'sys_monitor_type','CPU','cpu',3,NULL,'warning',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(65,'sys_monitor_type','ç‰©ç†å†…å­˜','memory',4,NULL,'warning',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(66,'sys_monitor_type','JVMå†…å­˜','jvm',5,NULL,'info',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(67,'sys_monitor_type','ç£ç›˜ç©ºé—´','disk',6,NULL,'danger',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(68,'sys_browser_type','Chrome','Chrome',1,NULL,'success',1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(69,'sys_browser_type','Firefox','Firefox',2,NULL,'warning',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(70,'sys_browser_type','Safari','Safari',3,NULL,'primary',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(71,'sys_browser_type','Edge','Edge',4,NULL,'info',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(72,'sys_browser_type','å…¶ä»–','Other',5,NULL,'',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(73,'sys_os_type','Windows','Windows',1,NULL,'primary',1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(74,'sys_os_type','macOS','macOS',2,NULL,'info',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(75,'sys_os_type','Linux','Linux',3,NULL,'success',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(76,'sys_os_type','Android','Android',4,NULL,'warning',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(77,'sys_os_type','iOS','iOS',5,NULL,'',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(78,'sys_os_type','å…¶ä»–','Other',6,NULL,'',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(79,'sys_redis_info_category','æœåŠ¡å™¨ä¿¡æ¯','server',1,NULL,'primary',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(80,'sys_redis_info_category','å†…å­˜ä¿¡æ¯','memory',2,NULL,'warning',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(81,'sys_redis_info_category','ç»Ÿè®¡ä¿¡æ¯','stats',3,NULL,'info',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(82,'sys_redis_info_category','é”®ç©ºé—´ä¿¡æ¯','keyspace',4,NULL,'danger',0,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL);
/*!40000 ALTER TABLE `sys_dict_data` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'å­—å…¸ç±»åž‹ID',
  `dict_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'å­—å…¸åç§°',
  `dict_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'å­—å…¸ç±»åž‹',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€ï¼š0-åœç”¨ï¼Œ1-æ­£å¸¸',
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ›´æ–°äºº',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤ï¼š0-æœªåˆ é™¤ï¼Œ1-å·²åˆ é™¤',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¤‡æ³¨',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='å­—å…¸ç±»åž‹è¡¨';

LOCK TABLES `sys_dict_type` WRITE;
/*!40000 ALTER TABLE `sys_dict_type` DISABLE KEYS */;
INSERT INTO `sys_dict_type` VALUES (1,'ç”¨æˆ·æ€§åˆ«','sys_user_gender',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'ç”¨æˆ·æ€§åˆ«åˆ—è¡¨'),(2,'èœå•çŠ¶æ€','sys_show_hide',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'èœå•çŠ¶æ€åˆ—è¡¨'),(3,'ç³»ç»Ÿå¼€å…³','sys_normal_disable',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'ç³»ç»Ÿå¼€å…³åˆ—è¡¨'),(4,'ç”¨æˆ·çŠ¶æ€','sys_user_status',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'ç”¨æˆ·çŠ¶æ€åˆ—è¡¨'),(5,'è§’è‰²çŠ¶æ€','sys_role_status',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'è§’è‰²çŠ¶æ€åˆ—è¡¨'),(6,'å…¬å‘Šç±»åž‹','sys_notice_type',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'ç³»ç»Ÿå…¬å‘Šç±»åž‹åˆ—è¡¨'),(7,'å…¬å‘ŠçŠ¶æ€','sys_notice_status',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'ç³»ç»Ÿå…¬å‘ŠçŠ¶æ€åˆ—è¡¨'),(8,'æ¶ˆæ¯ç±»åž‹','sys_message_type',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'æ¶ˆæ¯ä¸­å¿ƒæ¶ˆæ¯ç±»åž‹åˆ—è¡¨'),(9,'æ¶ˆæ¯ä¼˜å…ˆçº§','sys_message_priority',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'æ¶ˆæ¯ä¸­å¿ƒæ¶ˆæ¯ä¼˜å…ˆçº§åˆ—è¡¨'),(10,'æ¶ˆæ¯çŠ¶æ€','sys_message_status',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'æ¶ˆæ¯ä¸­å¿ƒæ¶ˆæ¯çŠ¶æ€åˆ—è¡¨'),(11,'æ¶ˆæ¯å‘é€è€…ç±»åž‹','sys_message_sender_type',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'æ¶ˆæ¯ä¸­å¿ƒå‘é€è€…ç±»åž‹åˆ—è¡¨'),(12,'æ¶ˆæ¯æŽ¥æ”¶è€…ç±»åž‹','sys_message_receiver_type',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'æ¶ˆæ¯ä¸­å¿ƒæŽ¥æ”¶è€…ç±»åž‹åˆ—è¡¨'),(13,'æ–‡ä»¶åˆ†ç±»','sys_file_category',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'æ–‡ä»¶åˆ†ç±»åˆ—è¡¨'),(14,'ä»»åŠ¡ç»„å','sys_job_group',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'å®šæ—¶ä»»åŠ¡çš„ä»»åŠ¡ç»„å'),(15,'ä»»åŠ¡çŠ¶æ€','sys_job_status',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'å®šæ—¶ä»»åŠ¡çš„è¿è¡ŒçŠ¶æ€'),(16,'ä»»åŠ¡é”™è¿‡æ‰§è¡Œç­–ç•¥','sys_job_misfire',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'å®šæ—¶ä»»åŠ¡é”™è¿‡æ‰§è¡Œæ—¶é—´çš„å¤„ç†ç­–ç•¥'),(17,'ä»»åŠ¡å¹¶å‘æ‰§è¡Œ','sys_job_concurrent',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'å®šæ—¶ä»»åŠ¡æ˜¯å¦å…è®¸å¹¶å‘æ‰§è¡Œ'),(18,'é…ç½®ç±»åž‹','sys_config_type',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'ç³»ç»Ÿå‚æ•°é…ç½®çš„ç±»åž‹åˆ†ç±»'),(19,'é€šç”¨çŠ¶æ€','sys_common_status',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'é€šç”¨æ“ä½œçŠ¶æ€åˆ—è¡¨ï¼Œç”¨äºŽç™»å½•æ—¥å¿—å’Œæ“ä½œæ—¥å¿—'),(20,'ä¸šåŠ¡ç±»åž‹','sys_business_type',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'æ“ä½œæ—¥å¿—ä¸šåŠ¡æ“ä½œç±»åž‹åˆ—è¡¨'),(21,'ç›‘æŽ§çŠ¶æ€','sys_monitor_status',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'ç³»ç»Ÿç›‘æŽ§æ£€æŸ¥é¡¹çš„è¿è¡ŒçŠ¶æ€'),(22,'ç›‘æŽ§é¡¹ç±»åž‹','sys_monitor_type',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'ç³»ç»Ÿç›‘æŽ§æ£€æŸ¥é¡¹çš„ç±»åž‹åˆ†ç±»'),(23,'æµè§ˆå™¨ç±»åž‹','sys_browser_type',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'åœ¨çº¿ç”¨æˆ·ç›‘æŽ§çš„æµè§ˆå™¨ç±»åž‹'),(24,'æ“ä½œç³»ç»Ÿç±»åž‹','sys_os_type',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'åœ¨çº¿ç”¨æˆ·ç›‘æŽ§çš„æ“ä½œç³»ç»Ÿç±»åž‹'),(25,'Redisä¿¡æ¯åˆ†ç±»','sys_redis_info_category',1,'system','2026-11-01 15:27:09',NULL,NULL,0,'Redisç›‘æŽ§ä¿¡æ¯çš„åˆ†ç±»æ ‡ç­¾');
/*!40000 ALTER TABLE `sys_dict_type` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'èœå•ID',
  `menu_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'èœå•åç§°',
  `parent_id` bigint DEFAULT '0' COMMENT 'çˆ¶èœå•ID',
  `menu_type` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'èœå•ç±»åž‹ï¼šM-ç›®å½•ï¼ŒC-èœå•ï¼ŒF-æŒ‰é’®',
  `path` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'è·¯ç”±åœ°å€',
  `component` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ç»„ä»¶è·¯å¾„',
  `permission` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æƒé™æ ‡è¯†',
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'èœå•å›¾æ ‡',
  `order_num` int NOT NULL DEFAULT '0' COMMENT 'æ˜¾ç¤ºé¡ºåº',
  `is_frame` tinyint NOT NULL DEFAULT '0' COMMENT 'æ˜¯å¦å¤–é“¾ï¼š0-å¦ï¼Œ1-æ˜¯',
  `is_cache` tinyint NOT NULL DEFAULT '1' COMMENT 'æ˜¯å¦ç¼“å­˜ï¼š0-ä¸ç¼“å­˜ï¼Œ1-ç¼“å­˜',
  `visible` tinyint NOT NULL DEFAULT '1' COMMENT 'æ˜¯å¦å¯è§ï¼š0-éšè—ï¼Œ1-æ˜¾ç¤º',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€ï¼š0-åœç”¨ï¼Œ1-æ­£å¸¸',
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ›´æ–°äºº',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤ï¼š0-æœªåˆ é™¤ï¼Œ1-å·²åˆ é™¤',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¤‡æ³¨',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9807 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='èœå•è¡¨';

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
INSERT INTO `sys_menu` VALUES (1,'ç³»ç»Ÿç®¡ç†',0,'M','/system',NULL,NULL,'setting',1,0,1,1,1,'system','2026-11-01 15:27:08',NULL,NULL,0,NULL),(2,'ç»„ç»‡ç®¡ç†',0,'M','/organization',NULL,NULL,'tree',2,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(3,'æ—¥å¿—ç®¡ç†',0,'M','/log',NULL,NULL,'log',3,0,1,1,1,'system','2026-11-01 15:27:09',NULL,'2026-11-01 15:27:21',0,NULL),(4,'æ¶ˆæ¯ç®¡ç†',0,'M','/message',NULL,NULL,'message',4,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(5,'æ–‡ä»¶ç®¡ç†',0,'M','/file',NULL,NULL,'upload',5,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(7,'è¿è¡Œæ—¶ç›‘æŽ§',0,'M','/monitor',NULL,NULL,'monitor',7,0,1,1,1,'system','2026-11-01 15:27:09',NULL,'2026-11-01 15:27:21',0,NULL),(8,'å®šæ—¶ä»»åŠ¡ç®¡ç†',9500,'C','/ops/job','monitor/job/index','job:list','job',1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,'2026-11-01 15:27:21',0,NULL),(9,'ç»Ÿè®¡åˆ†æž',0,'M','/analysis',NULL,NULL,'chart',10,0,1,1,1,'system','2026-11-01 15:27:09',NULL,'2026-11-01 15:27:21',0,NULL),(100,'ç”¨æˆ·ç®¡ç†',1,'C','/system/user','system/user/index','user:list','user',1,0,1,1,1,'system','2026-11-01 15:27:08',NULL,NULL,0,NULL),(101,'è§’è‰²ç®¡ç†',1,'C','/system/role','system/role/index','role:list','peoples',2,0,1,1,1,'system','2026-11-01 15:27:08',NULL,NULL,0,NULL),(102,'èœå•ç®¡ç†',1,'C','/system/menu','system/menu/index','menu:list','tree-table',3,0,1,1,1,'system','2026-11-01 15:27:08',NULL,NULL,0,NULL),(103,'å­—å…¸ç®¡ç†',1,'C','/system/dict','system/dict/index','dict:list','dict',4,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(104,'å‚æ•°é…ç½®',1,'C','/system/config','system/config/index','config:list','edit',5,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(200,'éƒ¨é—¨ç®¡ç†',2,'C','/organization/dept','organization/dept/index','dept:list','tree',1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(201,'å²—ä½ç®¡ç†',2,'C','/organization/post','organization/post/index','post:list','post',2,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(202,'ç»„ç»‡æž¶æž„å›¾',2,'C','/organization/org-chart','organization/org-chart/index','orgchart:view','chart',3,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(300,'ç™»å½•æ—¥å¿—',3,'C','/log/login-log','security/login-log/index','loginlog:list','logininfor',1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,'2026-11-01 15:27:21',0,NULL),(301,'æ“ä½œæ—¥å¿—',3,'C','/log/operate-log','security/operate-log/index','operatelog:list','form',2,0,1,1,1,'system','2026-11-01 15:27:09',NULL,'2026-11-01 15:27:21',0,NULL),(302,'è¿è¡Œæ—¥å¿—',3,'C','/log/audit-log','security/audit-log/index','auditlog:list','bug',3,0,1,1,1,'system','2026-11-01 15:27:09',NULL,'2026-11-01 15:27:21',0,NULL),(400,'ç³»ç»Ÿå…¬å‘Š',4,'C','/message/notice','message/notice/index','notice:list','notification',1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(401,'æ¶ˆæ¯ä¸­å¿ƒ',4,'C','/message/message-center','message/message-center/index','message:list','chat-dot-round',2,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(500,'æ–‡ä»¶ç®¡ç†',5,'C','/file/file-manage','file/file-manage/index','file:list','folder',1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(501,'æ–‡ä»¶ä¸Šä¼ ',500,'F',NULL,NULL,'file:upload',NULL,1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(502,'æ–‡ä»¶ä¸‹è½½',500,'F',NULL,NULL,'file:download',NULL,2,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(503,'æ–‡ä»¶åˆ é™¤',500,'F',NULL,NULL,'file:remove',NULL,3,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(504,'æ–‡ä»¶å¯¼å‡º',500,'F',NULL,NULL,'file:export',NULL,4,0,1,1,1,'system','2026-11-01 15:27:10',NULL,NULL,0,NULL),(700,'åœ¨çº¿ç”¨æˆ·ç›‘æŽ§',7,'C','/monitor/online-user','monitor/online-user/index','online:list','online',1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(701,'æœåŠ¡å¥åº·ç›‘æŽ§ï¼ˆSBAï¼‰',7,'C','/monitor/server-monitor','monitor/server-monitor/index','server:list','server',3,0,1,1,1,'system','2026-11-01 15:27:09',NULL,'2026-11-01 15:27:21',0,NULL),(702,'ç¼“å­˜ç›‘æŽ§ï¼ˆRedisï¼‰',7,'C','/monitor/redis-monitor','monitor/redis-monitor/index','redis:list','redis',5,0,1,1,1,'system','2026-11-01 15:27:09',NULL,'2026-11-01 15:27:21',0,NULL),(703,'æ•°æ®åº“ç›‘æŽ§ï¼ˆDruidï¼‰',7,'C','/monitor/sql-monitor','monitor/sql-monitor/index','monitor:sql','sql',4,0,1,1,1,'system','2026-11-01 15:27:09',NULL,'2026-11-01 15:27:21',0,NULL),(704,'ç³»ç»Ÿæ€§èƒ½ç›‘æŽ§',7,'C','/monitor/performance','monitor/performance/index','monitor:performance','dashboard',2,0,1,1,1,'system','2026-11-01 15:27:09',NULL,'2026-11-01 15:27:21',0,NULL),(705,'Swagger UI',9500,'C','/ops/swagger','monitor/swagger/index','monitor:swagger','api',3,0,1,1,1,'system','2026-11-01 15:27:09',NULL,'2026-11-01 15:27:21',0,NULL),(706,'å¯¹è±¡å­˜å‚¨ç›‘æŽ§ï¼ˆMinIOï¼‰',7,'C','/monitor/minio-monitor','monitor/minio-monitor/index','monitor:minio','box',6,0,1,1,1,'system','2026-11-01 15:27:10',NULL,'2026-11-01 15:27:21',0,NULL),(707,'Prometheus',9700,'C','/observability/prometheus-monitor','monitor/prometheus-monitor/index','monitor:prometheus','trend',2,0,1,1,1,'system','2026-11-01 15:27:10',NULL,'2026-11-01 15:27:21',0,NULL),(708,'Grafana çœ‹æ¿',9700,'C','/observability/grafana-monitor','monitor/grafana-monitor/index','monitor:grafana','histogram',1,0,1,1,1,'system','2026-11-01 15:27:10',NULL,'2026-11-01 15:27:21',0,NULL),(709,'Kibana æ—¥å¿—æ£€ç´¢',3,'C','/log/kibana-monitor','monitor/kibana-monitor/index','monitor:kibana','data-analysis',4,0,1,1,1,'system','2026-11-01 15:27:10',NULL,'2026-11-01 15:27:21',0,NULL),(710,'Alertmanager',9700,'C','/observability/alertmanager','monitor/alertmanager/index','monitor:alertmanager','warning-filled',3,0,1,1,1,'system','2026-11-01 15:27:10',NULL,'2026-11-01 15:27:21',0,NULL),(711,'Thanos æŸ¥è¯¢',9700,'C','/observability/thanos-query','monitor/thanos-query/index','monitor:thanos','trend',4,0,1,1,1,'system','2026-11-01 15:27:12',NULL,'2026-11-01 15:27:21',0,NULL),(801,'ä»»åŠ¡æŸ¥è¯¢',8,'F',NULL,NULL,'job:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(802,'ä»»åŠ¡æ–°å¢ž',8,'F',NULL,NULL,'job:add',NULL,2,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(803,'ä»»åŠ¡ä¿®æ”¹',8,'F',NULL,NULL,'job:edit',NULL,3,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(804,'ä»»åŠ¡åˆ é™¤',8,'F',NULL,NULL,'job:remove',NULL,4,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(805,'ä»»åŠ¡æ‰§è¡Œ',8,'F',NULL,NULL,'job:run',NULL,5,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(806,'çŠ¶æ€ä¿®æ”¹',8,'F',NULL,NULL,'job:changeStatus',NULL,6,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(807,'ä»»åŠ¡æ—¥å¿—',8,'F',NULL,NULL,'job:log',NULL,7,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(808,'ä»»åŠ¡å¯¼å‡º',8,'F',NULL,NULL,'job:export',NULL,8,0,1,1,1,'system','2026-11-01 15:27:10',NULL,NULL,0,NULL),(900,'æ•°æ®ç»Ÿè®¡æŠ¥è¡¨',9,'C','/analysis/report','analysis/report/index','report:view','pie-chart',1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(901,'ç³»ç»Ÿä½¿ç”¨åˆ†æž',9,'C','/analysis/usage','analysis/usage/index','usage:view','data-analysis',2,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(1001,'ç”¨æˆ·æŸ¥è¯¢',100,'F',NULL,NULL,'user:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:08',NULL,NULL,0,NULL),(1002,'ç”¨æˆ·æ–°å¢ž',100,'F',NULL,NULL,'user:add',NULL,2,0,1,1,1,'system','2026-11-01 15:27:08',NULL,NULL,0,NULL),(1003,'ç”¨æˆ·ä¿®æ”¹',100,'F',NULL,NULL,'user:edit',NULL,3,0,1,1,1,'system','2026-11-01 15:27:08',NULL,NULL,0,NULL),(1004,'ç”¨æˆ·åˆ é™¤',100,'F',NULL,NULL,'user:remove',NULL,4,0,1,1,1,'system','2026-11-01 15:27:08',NULL,NULL,0,NULL),(1005,'ç”¨æˆ·é‡ç½®å¯†ç ',100,'F',NULL,NULL,'user:resetPwd',NULL,5,0,1,1,1,'system','2026-11-01 15:27:08',NULL,NULL,0,NULL),(1006,'ç”¨æˆ·å¯¼å‡º',100,'F',NULL,NULL,'user:export',NULL,6,0,1,1,1,'system','2026-11-01 15:27:10',NULL,NULL,0,NULL),(1011,'è§’è‰²æŸ¥è¯¢',101,'F',NULL,NULL,'role:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:08',NULL,NULL,0,NULL),(1012,'è§’è‰²æ–°å¢ž',101,'F',NULL,NULL,'role:add',NULL,2,0,1,1,1,'system','2026-11-01 15:27:08',NULL,NULL,0,NULL),(1013,'è§’è‰²ä¿®æ”¹',101,'F',NULL,NULL,'role:edit',NULL,3,0,1,1,1,'system','2026-11-01 15:27:08',NULL,NULL,0,NULL),(1014,'è§’è‰²åˆ é™¤',101,'F',NULL,NULL,'role:remove',NULL,4,0,1,1,1,'system','2026-11-01 15:27:08',NULL,NULL,0,NULL),(1015,'è§’è‰²å¯¼å‡º',101,'F',NULL,NULL,'role:export',NULL,5,0,1,1,1,'system','2026-11-01 15:27:10',NULL,NULL,0,NULL),(1021,'èœå•æŸ¥è¯¢',102,'F',NULL,NULL,'menu:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:08',NULL,NULL,0,NULL),(1022,'èœå•æ–°å¢ž',102,'F',NULL,NULL,'menu:add',NULL,2,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(1023,'èœå•ä¿®æ”¹',102,'F',NULL,NULL,'menu:edit',NULL,3,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(1024,'èœå•åˆ é™¤',102,'F',NULL,NULL,'menu:remove',NULL,4,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(1025,'èœå•å¯¼å‡º',102,'F',NULL,NULL,'menu:export',NULL,5,0,1,1,1,'system','2026-11-01 15:27:10',NULL,NULL,0,NULL),(1031,'å­—å…¸æŸ¥è¯¢',103,'F',NULL,NULL,'dict:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(1032,'å­—å…¸æ–°å¢ž',103,'F',NULL,NULL,'dict:add',NULL,2,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(1033,'å­—å…¸ä¿®æ”¹',103,'F',NULL,NULL,'dict:edit',NULL,3,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(1034,'å­—å…¸åˆ é™¤',103,'F',NULL,NULL,'dict:remove',NULL,4,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(1035,'å­—å…¸å¯¼å‡º',103,'F',NULL,NULL,'dict:export',NULL,5,0,1,1,1,'system','2026-11-01 15:27:10',NULL,NULL,0,NULL),(1041,'å‚æ•°æŸ¥è¯¢',104,'F',NULL,NULL,'config:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(1042,'å‚æ•°æ–°å¢ž',104,'F',NULL,NULL,'config:add',NULL,2,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(1043,'å‚æ•°ä¿®æ”¹',104,'F',NULL,NULL,'config:edit',NULL,3,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(1044,'å‚æ•°åˆ é™¤',104,'F',NULL,NULL,'config:remove',NULL,4,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(1045,'å‚æ•°å¯¼å‡º',104,'F',NULL,NULL,'config:export',NULL,5,0,1,1,1,'system','2026-11-01 15:27:10',NULL,NULL,0,NULL),(2001,'éƒ¨é—¨æŸ¥è¯¢',200,'F',NULL,NULL,'dept:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(2002,'éƒ¨é—¨æ–°å¢ž',200,'F',NULL,NULL,'dept:add',NULL,2,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(2003,'éƒ¨é—¨ä¿®æ”¹',200,'F',NULL,NULL,'dept:edit',NULL,3,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(2004,'éƒ¨é—¨åˆ é™¤',200,'F',NULL,NULL,'dept:remove',NULL,4,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(2005,'éƒ¨é—¨å¯¼å‡º',200,'F',NULL,NULL,'dept:export',NULL,5,0,1,1,1,'system','2026-11-01 15:27:10',NULL,NULL,0,NULL),(2011,'å²—ä½æŸ¥è¯¢',201,'F',NULL,NULL,'post:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(2012,'å²—ä½æ–°å¢ž',201,'F',NULL,NULL,'post:add',NULL,2,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(2013,'å²—ä½ä¿®æ”¹',201,'F',NULL,NULL,'post:edit',NULL,3,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(2014,'å²—ä½åˆ é™¤',201,'F',NULL,NULL,'post:remove',NULL,4,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(2015,'å²—ä½å¯¼å‡º',201,'F',NULL,NULL,'post:export',NULL,5,0,1,1,1,'system','2026-11-01 15:27:10',NULL,NULL,0,NULL),(3001,'ç™»å½•æ—¥å¿—æŸ¥è¯¢',300,'F',NULL,NULL,'loginlog:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(3002,'ç™»å½•æ—¥å¿—åˆ é™¤',300,'F',NULL,NULL,'loginlog:remove',NULL,2,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(3003,'ç™»å½•æ—¥å¿—å¯¼å‡º',300,'F',NULL,NULL,'loginlog:export',NULL,3,0,1,1,1,'system','2026-11-01 15:27:10',NULL,NULL,0,NULL),(3011,'æ“ä½œæ—¥å¿—æŸ¥è¯¢',301,'F',NULL,NULL,'operatelog:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(3012,'æ“ä½œæ—¥å¿—åˆ é™¤',301,'F',NULL,NULL,'operatelog:remove',NULL,2,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(3013,'æ“ä½œæ—¥å¿—å¯¼å‡º',301,'F',NULL,NULL,'operatelog:export',NULL,3,0,1,1,1,'system','2026-11-01 15:27:10',NULL,NULL,0,NULL),(3021,'è¿è¡Œæ—¥å¿—æŸ¥è¯¢',302,'F',NULL,NULL,'auditlog:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(4001,'å…¬å‘ŠæŸ¥è¯¢',400,'F',NULL,NULL,'notice:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(4002,'å…¬å‘Šæ–°å¢ž',400,'F',NULL,NULL,'notice:add',NULL,2,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(4003,'å…¬å‘Šä¿®æ”¹',400,'F',NULL,NULL,'notice:edit',NULL,3,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(4004,'å…¬å‘Šåˆ é™¤',400,'F',NULL,NULL,'notice:remove',NULL,4,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(4005,'å…¬å‘Šå‘å¸ƒ',400,'F',NULL,NULL,'notice:publish',NULL,5,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(4006,'å…¬å‘Šå¯¼å‡º',400,'F',NULL,NULL,'notice:export',NULL,6,0,1,1,1,'system','2026-11-01 15:27:10',NULL,NULL,0,NULL),(4010,'æ¶ˆæ¯æŸ¥è¯¢',401,'F',NULL,NULL,'message:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(4011,'æ¶ˆæ¯æ–°å¢ž',401,'F',NULL,NULL,'message:add',NULL,2,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(4012,'æ¶ˆæ¯ä¿®æ”¹',401,'F',NULL,NULL,'message:edit',NULL,3,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(4013,'æ¶ˆæ¯åˆ é™¤',401,'F',NULL,NULL,'message:remove',NULL,4,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(4014,'æ¶ˆæ¯å‘é€',401,'F',NULL,NULL,'message:send',NULL,5,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(4015,'æ¨¡æ¿æŸ¥è¯¢',401,'F',NULL,NULL,'message:template:list',NULL,6,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(4016,'æ¨¡æ¿è¯¦æƒ…',401,'F',NULL,NULL,'message:template:query',NULL,7,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(4017,'æ¨¡æ¿æ–°å¢ž',401,'F',NULL,NULL,'message:template:add',NULL,8,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(4018,'æ¨¡æ¿ä¿®æ”¹',401,'F',NULL,NULL,'message:template:edit',NULL,9,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(4019,'æ¨¡æ¿åˆ é™¤',401,'F',NULL,NULL,'message:template:remove',NULL,10,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(4020,'æ¶ˆæ¯å¯¼å‡º',401,'F',NULL,NULL,'message:export',NULL,11,0,1,1,1,'system','2026-11-01 15:27:10',NULL,NULL,0,NULL),(7001,'åœ¨çº¿ç”¨æˆ·æŸ¥è¯¢',700,'F',NULL,NULL,'online:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(7002,'åœ¨çº¿ç”¨æˆ·å¼ºé€€',700,'F',NULL,NULL,'online:forceLogout',NULL,2,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(7003,'åœ¨çº¿ç”¨æˆ·å¯¼å‡º',700,'F',NULL,NULL,'online:export',NULL,3,0,1,1,1,'system','2026-11-01 15:27:10',NULL,NULL,0,NULL),(7011,'æœåŠ¡ç›‘æŽ§æŸ¥è¯¢',701,'F',NULL,NULL,'server:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(7021,'Redisç›‘æŽ§æŸ¥è¯¢',702,'F',NULL,NULL,'redis:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(7022,'ç¼“å­˜é”®åˆ é™¤',702,'F',NULL,NULL,'redis:delete',NULL,2,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(7023,'ç¼“å­˜æ¸…ç©º',702,'F',NULL,NULL,'redis:clear',NULL,3,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(7031,'SQLç›‘æŽ§æŸ¥è¯¢',703,'F',NULL,NULL,'sql:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(7041,'æ€§èƒ½ç›‘æŽ§æŸ¥è¯¢',704,'F',NULL,NULL,'performance:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(7061,'MinIOç›‘æŽ§æŸ¥è¯¢',706,'F',NULL,NULL,'minio:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:10',NULL,NULL,0,NULL),(7071,'Promethç›‘æŽ§æŸ¥è¯¢',707,'F',NULL,NULL,'prometheus:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:10',NULL,NULL,0,NULL),(7081,'Grafanaçœ‹æ¿æŸ¥è¯¢',708,'F',NULL,NULL,'grafana:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:10',NULL,NULL,0,NULL),(7091,'KibanaæŸ¥è¯¢',709,'F',NULL,NULL,'kibana:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:10',NULL,NULL,0,NULL),(7101,'Alertmanager æŸ¥è¯¢',710,'F',NULL,NULL,'alertmanager:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:10',NULL,NULL,0,NULL),(7111,'Thanos æŸ¥è¯¢',711,'F',NULL,NULL,'thanos:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:12',NULL,NULL,0,NULL),(9001,'æŠ¥è¡¨æŸ¥è¯¢',900,'F',NULL,NULL,'report:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(9011,'ä½¿ç”¨åˆ†æžæŸ¥è¯¢',901,'F',NULL,NULL,'usage:query',NULL,1,0,1,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,NULL),(9100,'å®¡æ‰¹ä¸­å¿ƒ',0,'C','/approval','approval/index','approval:apply','finished',6,0,1,1,1,'system','2026-11-01 15:27:20',NULL,'2026-11-01 15:27:21',0,NULL),(9101,'å‘èµ·è¯·å‡',9100,'F',NULL,NULL,'approval:apply',NULL,1,0,1,1,1,'system','2026-11-01 15:27:20',NULL,NULL,0,NULL),(9102,'å®¡æ‰¹æ“ä½œ',9100,'F',NULL,NULL,'approval:approve',NULL,2,0,1,1,1,'system','2026-11-01 15:27:20',NULL,NULL,0,NULL),(9103,'å…¨éƒ¨ç”³è¯·',9100,'F',NULL,NULL,'approval:list',NULL,3,0,1,1,1,'system','2026-11-01 15:27:20',NULL,NULL,0,NULL),(9104,'ç”³è¯·è¯¦æƒ…',9100,'F',NULL,NULL,'approval:query',NULL,4,0,1,1,1,'system','2026-11-01 15:27:20',NULL,NULL,0,NULL),(9200,'é€šçŸ¥æŠ•é€’',4,'C','/notification/delivery','message/NotificationDeliveryView','notification:delivery:list','message',3,0,1,1,1,'system','2026-11-01 15:27:20',NULL,'2026-11-01 15:27:21',0,NULL),(9201,'æ‰‹åŠ¨é‡è¯•',9200,'F',NULL,NULL,'notification:delivery:retry',NULL,1,0,1,1,1,'system','2026-11-01 15:27:20',NULL,NULL,0,NULL),(9300,'æ•°æ®å¤‡ä»½',9500,'C','/ops/backup','monitor/BackupView','backup:record:list','database',2,0,1,1,1,'system','2026-11-01 15:27:21',NULL,'2026-11-01 15:27:21',0,NULL),(9301,'ç«‹å³å¤‡ä»½',9300,'F',NULL,NULL,'backup:record:create',NULL,1,0,1,1,1,'system','2026-11-01 15:27:21',NULL,NULL,0,NULL),(9302,'æ¢å¤æ¼”ç»ƒ',9300,'F',NULL,NULL,'backup:record:restore',NULL,2,0,1,1,1,'system','2026-11-01 15:27:21',NULL,NULL,0,NULL),(9500,'ç³»ç»Ÿè¿ç»´',0,'M','/ops',NULL,NULL,'operation',9,0,1,1,1,'system','2026-11-01 15:27:21',NULL,'2026-11-01 15:27:21',0,NULL),(9700,'å¯è§‚æµ‹å¹³å°',0,'M','/observability',NULL,NULL,'chart',8,0,1,1,1,'system','2026-11-01 15:27:21',NULL,NULL,0,NULL),(9800,'AI å·¥ä½œå°',0,'M','/ai',NULL,NULL,'chat-dot-round',9,0,1,1,1,'system','2026-11-01 15:27:21',NULL,NULL,0,NULL),(9801,'æ™ºèƒ½å¯¹è¯',9800,'C','/ai/chat','ai/ChatView','ai:chat','chat-dot-round',1,0,1,1,1,'system','2026-11-01 15:27:21',NULL,NULL,0,NULL),(9802,'çŸ¥è¯†åº“',9800,'C','/ai/knowledge','ai/KnowledgeView','ai:kb:query','collection',2,0,1,1,1,'system','2026-11-01 15:27:21',NULL,NULL,0,NULL),(9803,'çŸ¥è¯†åº“ç®¡ç†',9802,'F',NULL,NULL,'ai:kb:manage',NULL,1,0,1,1,1,'system','2026-11-01 15:27:21',NULL,NULL,0,NULL),(9804,'æ™ºèƒ½ä½“',9800,'C','/ai/agent','ai/AgentChatView','ai:agent:use','magic-stick',3,0,1,1,1,'system','2026-11-01 15:27:21',NULL,NULL,0,NULL),(9805,'æ™ºèƒ½ä½“ç®¡ç†',9800,'C','/ai/agent-config','ai/AgentConfigView','ai:agent:manage','setting',4,0,1,1,1,'system','2026-11-01 15:27:21',NULL,NULL,0,NULL),(9806,'çŸ¥è¯†å›¾è°±',9800,'F',NULL,NULL,'ai:graph:use',NULL,5,0,1,1,1,'system','2026-11-01 15:27:22',NULL,NULL,0,NULL);
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'å²—ä½ID',
  `post_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'å²—ä½ç¼–ç ',
  `post_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'å²—ä½åç§°',
  `dept_id` bigint DEFAULT NULL COMMENT 'æ‰€å±žéƒ¨é—¨ ID',
  `post_sort` int NOT NULL DEFAULT '0' COMMENT 'å²—ä½æŽ’åº',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€ï¼š0-åœç”¨ï¼Œ1-æ­£å¸¸',
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ›´æ–°äºº',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤ï¼š0-æœªåˆ é™¤ï¼Œ1-å·²åˆ é™¤',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¤‡æ³¨',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_code` (`post_code`),
  KEY `idx_post_dept_id` (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='å²—ä½è¡¨';

LOCK TABLES `sys_post` WRITE;
/*!40000 ALTER TABLE `sys_post` DISABLE KEYS */;
INSERT INTO `sys_post` VALUES (1,'CEO','è‘£äº‹é•¿',NULL,1,1,'system','2026-11-01 15:27:09',NULL,NULL,0,'è‘£äº‹é•¿å²—ä½'),(2,'MANAGER','é¡¹ç›®ç»ç†',NULL,2,1,'system','2026-11-01 15:27:09',NULL,NULL,0,'é¡¹ç›®ç»ç†å²—ä½'),(3,'STAFF','æ™®é€šå‘˜å·¥',NULL,3,1,'system','2026-11-01 15:27:09',NULL,NULL,0,'æ™®é€šå‘˜å·¥å²—ä½');
/*!40000 ALTER TABLE `sys_post` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'è§’è‰²ID',
  `role_name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'è§’è‰²åç§°',
  `role_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'è§’è‰²ç¼–ç ',
  `role_sort` int NOT NULL DEFAULT '0' COMMENT 'è§’è‰²æŽ’åº',
  `data_scope` tinyint DEFAULT '1' COMMENT 'æ•°æ®æƒé™èŒƒå›´ï¼š1-å…¨éƒ¨ï¼Œ2-è‡ªå®šä¹‰ï¼Œ3-æœ¬éƒ¨é—¨ï¼Œ4-æœ¬éƒ¨é—¨åŠä»¥ä¸‹ï¼Œ5-ä»…æœ¬äºº',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€ï¼š0-åœç”¨ï¼Œ1-æ­£å¸¸',
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ›´æ–°äºº',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤ï¼š0-æœªåˆ é™¤ï¼Œ1-å·²åˆ é™¤',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¤‡æ³¨',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='è§’è‰²è¡¨';

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,'è¶…çº§ç®¡ç†å‘˜','ROLE_ADMIN',1,1,1,'system','2026-11-01 15:27:08',NULL,NULL,0,'è¶…çº§ç®¡ç†å‘˜'),(2,'æ™®é€šç”¨æˆ·','ROLE_USER',2,5,1,'system','2026-11-01 15:27:08',NULL,NULL,0,'æ™®é€šç”¨æˆ·');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept` (
  `role_id` bigint NOT NULL COMMENT 'è§’è‰²ID',
  `dept_id` bigint NOT NULL COMMENT 'éƒ¨é—¨ID',
  PRIMARY KEY (`role_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='è§’è‰²éƒ¨é—¨å…³è”è¡¨';

LOCK TABLES `sys_role_dept` WRITE;
/*!40000 ALTER TABLE `sys_role_dept` DISABLE KEYS */;
INSERT INTO `sys_role_dept` VALUES (1,1),(1,2),(1,3);
/*!40000 ALTER TABLE `sys_role_dept` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT 'è§’è‰²ID',
  `menu_id` bigint NOT NULL COMMENT 'èœå•ID',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='è§’è‰²èœå•å…³è”è¡¨';

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
INSERT INTO `sys_role_menu` VALUES (1,1),(1,2),(1,3),(1,4),(1,5),(1,7),(1,8),(1,9),(1,100),(1,101),(1,102),(1,103),(1,104),(1,200),(1,201),(1,202),(1,300),(1,301),(1,302),(1,400),(1,401),(1,500),(1,501),(1,502),(1,503),(1,504),(1,700),(1,701),(1,702),(1,703),(1,704),(1,705),(1,706),(1,707),(1,708),(1,709),(1,710),(1,711),(1,801),(1,802),(1,803),(1,804),(1,805),(1,806),(1,807),(1,808),(1,900),(1,901),(1,1001),(1,1002),(1,1003),(1,1004),(1,1005),(1,1006),(1,1011),(1,1012),(1,1013),(1,1014),(1,1015),(1,1021),(1,1022),(1,1023),(1,1024),(1,1025),(1,1031),(1,1032),(1,1033),(1,1034),(1,1035),(1,1041),(1,1042),(1,1043),(1,1044),(1,1045),(1,2001),(1,2002),(1,2003),(1,2004),(1,2005),(1,2011),(1,2012),(1,2013),(1,2014),(1,2015),(1,3001),(1,3002),(1,3003),(1,3011),(1,3012),(1,3013),(1,3021),(1,4001),(1,4002),(1,4003),(1,4004),(1,4005),(1,4006),(1,4010),(1,4011),(1,4012),(1,4013),(1,4014),(1,4015),(1,4016),(1,4017),(1,4018),(1,4019),(1,4020),(1,7001),(1,7002),(1,7003),(1,7011),(1,7021),(1,7022),(1,7023),(1,7031),(1,7041),(1,7061),(1,7071),(1,7081),(1,7091),(1,7101),(1,7111),(1,9001),(1,9011),(1,9100),(1,9101),(1,9102),(1,9103),(1,9104),(1,9200),(1,9201),(1,9300),(1,9301),(1,9302),(1,9500),(1,9700),(1,9800),(1,9801),(1,9802),(1,9803),(1,9804),(1,9805),(1,9806);
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ç”¨æˆ·ID',
  `username` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ç”¨æˆ·å',
  `password` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'å¯†ç ',
  `nickname` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ˜µç§°',
  `email` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'é‚®ç®±',
  `phone` varchar(11) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ‰‹æœºå·ç ',
  `gender` tinyint DEFAULT '0' COMMENT 'æ€§åˆ«ï¼š0-æœªçŸ¥ï¼Œ1-ç”·ï¼Œ2-å¥³',
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¤´åƒåœ°å€',
  `user_type` tinyint DEFAULT '2' COMMENT 'ç”¨æˆ·ç±»åž‹ï¼š1-ç³»ç»Ÿç”¨æˆ·ï¼Œ2-æ™®é€šç”¨æˆ·',
  `dept_id` bigint DEFAULT NULL COMMENT 'éƒ¨é—¨ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€ï¼š0-åœç”¨ï¼Œ1-æ­£å¸¸',
  `login_ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æœ€åŽç™»å½•IP',
  `login_time` datetime DEFAULT NULL COMMENT 'æœ€åŽç™»å½•æ—¶é—´',
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ›´æ–°äºº',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤ï¼š0-æœªåˆ é™¤ï¼Œ1-å·²åˆ é™¤',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¤‡æ³¨',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ç”¨æˆ·è¡¨';

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'admin','$2a$10$uAKzTVTib3zNyrwLAZt5UemckqbOMOaBBQfewspf01jKbkhs/ENpW','ç®¡ç†å‘˜','admin@iboot.com','18888888888',1,NULL,1,1,1,NULL,NULL,'system','2026-11-01 15:27:08',NULL,NULL,0,'ç³»ç»Ÿç®¡ç†å‘˜');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post` (
  `user_id` bigint NOT NULL COMMENT 'ç”¨æˆ·ID',
  `post_id` bigint NOT NULL COMMENT 'å²—ä½ID',
  PRIMARY KEY (`user_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ç”¨æˆ·å²—ä½å…³è”è¡¨';

LOCK TABLES `sys_user_post` WRITE;
/*!40000 ALTER TABLE `sys_user_post` DISABLE KEYS */;
INSERT INTO `sys_user_post` VALUES (1,1);
/*!40000 ALTER TABLE `sys_user_post` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT 'ç”¨æˆ·ID',
  `role_id` bigint NOT NULL COMMENT 'è§’è‰²ID',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ç”¨æˆ·è§’è‰²å…³è”è¡¨';

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
INSERT INTO `sys_user_role` VALUES (1,1);
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;

/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- ===== iboot_monitor =====

/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'æ—¥å¿—ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ç”¨æˆ·å',
  `dept_id` bigint DEFAULT NULL COMMENT 'ç™»å½•ç”¨æˆ·éƒ¨é—¨ ID',
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ç™»å½•IP',
  `login_location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ç™»å½•åœ°ç‚¹',
  `browser` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æµè§ˆå™¨',
  `os` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ“ä½œç³»ç»Ÿ',
  `status` tinyint NOT NULL COMMENT 'ç™»å½•çŠ¶æ€ï¼š0-å¤±è´¥ï¼Œ1-æˆåŠŸ',
  `message` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æç¤ºæ¶ˆæ¯',
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'ç™»å½•æ—¶é—´',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—ï¼š0-æœªåˆ é™¤ï¼Œ1-å·²åˆ é™¤',
  PRIMARY KEY (`id`),
  KEY `idx_username` (`username`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_login_dept_id` (`dept_id`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ç™»å½•æ—¥å¿—è¡¨';

LOCK TABLES `sys_login_log` WRITE;
/*!40000 ALTER TABLE `sys_login_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_login_log` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_operate_log`;
CREATE TABLE `sys_operate_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'æ—¥å¿—ID',
  `title` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ¨¡å—æ ‡é¢˜',
  `business_type` tinyint DEFAULT NULL COMMENT 'ä¸šåŠ¡ç±»åž‹ï¼š0-å…¶å®ƒï¼Œ1-æ–°å¢žï¼Œ2-ä¿®æ”¹ï¼Œ3-åˆ é™¤',
  `method` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ–¹æ³•åç§°',
  `request_method` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'è¯·æ±‚æ–¹å¼',
  `operator_type` tinyint DEFAULT NULL COMMENT 'æ“ä½œç±»åˆ«ï¼š0-å…¶å®ƒï¼Œ1-åŽå°ç”¨æˆ·ï¼Œ2-æ‰‹æœºç«¯ç”¨æˆ·',
  `operator_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ“ä½œäººå‘˜',
  `dept_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'éƒ¨é—¨åç§°',
  `dept_id` bigint DEFAULT NULL COMMENT 'æ“ä½œäººå‘˜éƒ¨é—¨ ID',
  `url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'è¯·æ±‚URL',
  `ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ä¸»æœºåœ°å€',
  `location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ“ä½œåœ°ç‚¹',
  `param` text COLLATE utf8mb4_unicode_ci COMMENT 'è¯·æ±‚å‚æ•°',
  `result` text COLLATE utf8mb4_unicode_ci COMMENT 'è¿”å›žå‚æ•°',
  `status` tinyint NOT NULL COMMENT 'æ“ä½œçŠ¶æ€ï¼š0-å¤±è´¥ï¼Œ1-æˆåŠŸ',
  `error_msg` text COLLATE utf8mb4_unicode_ci COMMENT 'é”™è¯¯æ¶ˆæ¯',
  `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'æ“ä½œæ—¶é—´',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—ï¼š0-æœªåˆ é™¤ï¼Œ1-å·²åˆ é™¤',
  `cost_time` bigint DEFAULT NULL COMMENT 'æ‰§è¡Œè€—æ—¶ï¼ˆæ¯«ç§’ï¼‰',
  PRIMARY KEY (`id`),
  KEY `idx_operator_name` (`operator_name`),
  KEY `idx_operate_time` (`operate_time`),
  KEY `idx_operate_dept_id` (`dept_id`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='æ“ä½œæ—¥å¿—è¡¨';

LOCK TABLES `sys_operate_log` WRITE;
/*!40000 ALTER TABLE `sys_operate_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_operate_log` ENABLE KEYS */;
UNLOCK TABLES;

/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- ===== iboot_file =====

/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'æ–‡ä»¶ID',
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'åŽŸå§‹æ–‡ä»¶å',
  `file_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'å­˜å‚¨è·¯å¾„',
  `file_size` bigint NOT NULL DEFAULT '0' COMMENT 'æ–‡ä»¶å¤§å°ï¼ˆå­—èŠ‚ï¼‰',
  `file_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'MIMEç±»åž‹',
  `file_ext` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ–‡ä»¶æ‰©å±•å',
  `file_category` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ–‡ä»¶åˆ†ç±»ï¼ˆå…³è”å­—å…¸ sys_file_categoryï¼‰',
  `dept_id` bigint DEFAULT NULL COMMENT 'ä¸Šä¼ äººéƒ¨é—¨ ID',
  `upload_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ä¸Šä¼ äºº',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'ä¸Šä¼ æ—¶é—´',
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ›´æ–°äºº',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤ï¼š0-æœªåˆ é™¤ï¼Œ1-å·²åˆ é™¤',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¤‡æ³¨',
  PRIMARY KEY (`id`),
  KEY `idx_file_name` (`file_name`),
  KEY `idx_file_category` (`file_category`),
  KEY `idx_file_dept_id` (`dept_id`),
  KEY `idx_upload_by` (`upload_by`),
  KEY `idx_upload_time` (`upload_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='æ–‡ä»¶ä¿¡æ¯è¡¨';

LOCK TABLES `sys_file` WRITE;
/*!40000 ALTER TABLE `sys_file` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_file` ENABLE KEYS */;
UNLOCK TABLES;

/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- ===== iboot_notify =====

/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `sys_message`;
CREATE TABLE `sys_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'æ¶ˆæ¯ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æ¶ˆæ¯æ ‡é¢˜',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æ¶ˆæ¯å†…å®¹',
  `message_type` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æ¶ˆæ¯ç±»åž‹ï¼ˆå­—å…¸ sys_message_typeï¼‰',
  `business_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ä¸šåŠ¡ç±»åž‹ï¼šAPPROVAL_TODO ç­‰',
  `priority` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT 'ä¼˜å…ˆçº§ï¼ˆå­—å…¸ sys_message_priorityï¼‰',
  `sender_type` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT 'å‘é€è€…ç±»åž‹ï¼ˆå­—å…¸ sys_message_sender_typeï¼‰',
  `sender_id` bigint DEFAULT NULL COMMENT 'å‘é€è€…ç”¨æˆ·IDï¼ˆç®¡ç†å‘˜å‘é€æ—¶è®°å½•ï¼‰',
  `receiver_type` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT 'æŽ¥æ”¶è€…ç±»åž‹ï¼ˆå­—å…¸ sys_message_receiver_typeï¼‰',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT 'çŠ¶æ€ï¼ˆå­—å…¸ sys_message_statusï¼‰',
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ›´æ–°äºº',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤ï¼š0-æœªåˆ é™¤ï¼Œ1-å·²åˆ é™¤',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¤‡æ³¨',
  PRIMARY KEY (`id`),
  KEY `idx_status_create_time` (`status`,`create_time`),
  KEY `idx_sender` (`sender_type`,`sender_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='æ¶ˆæ¯ä¸»è¡¨';

LOCK TABLES `sys_message` WRITE;
/*!40000 ALTER TABLE `sys_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_message` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_message_receiver`;
CREATE TABLE `sys_message_receiver` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'è®°å½•ID',
  `message_id` bigint NOT NULL COMMENT 'æ¶ˆæ¯ID',
  `user_id` bigint NOT NULL COMMENT 'æŽ¥æ”¶è€…ç”¨æˆ·ID',
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT 'æ˜¯å¦å·²è¯»ï¼š0-æœªè¯»ï¼Œ1-å·²è¯»',
  `read_time` datetime DEFAULT NULL COMMENT 'é˜…è¯»æ—¶é—´',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT 'ç”¨æˆ·ä¾§åˆ é™¤ï¼š0-æœªåˆ é™¤ï¼Œ1-å·²åˆ é™¤',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_user` (`message_id`,`user_id`),
  KEY `idx_user_read` (`user_id`,`is_read`,`is_deleted`),
  KEY `idx_receiver_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='æ¶ˆæ¯æŽ¥æ”¶è®°å½•è¡¨';

LOCK TABLES `sys_message_receiver` WRITE;
/*!40000 ALTER TABLE `sys_message_receiver` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_message_receiver` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_message_template`;
CREATE TABLE `sys_message_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'æ¨¡æ¿ID',
  `template_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æ¨¡æ¿ç¼–ç ',
  `template_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æ¨¡æ¿åç§°',
  `template_content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æ¨¡æ¿å†…å®¹ï¼ˆæ”¯æŒ$${å˜é‡å}å ä½ç¬¦ï¼‰',
  `message_type` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æ¶ˆæ¯ç±»åž‹ï¼ˆå­—å…¸ sys_message_typeï¼‰',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€ï¼š0-åœç”¨ï¼Œ1-æ­£å¸¸',
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ›´æ–°äºº',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤ï¼š0-æœªåˆ é™¤ï¼Œ1-å·²åˆ é™¤',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¤‡æ³¨',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_message_type` (`message_type`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='æ¶ˆæ¯æ¨¡æ¿è¡¨';

LOCK TABLES `sys_message_template` WRITE;
/*!40000 ALTER TABLE `sys_message_template` DISABLE KEYS */;
INSERT INTO `sys_message_template` VALUES (1,'APPROVAL_TODO','å®¡æ‰¹å¾…åŠžé€šçŸ¥','æ‚¨æœ‰ä¸€æ¡æ–°çš„å®¡æ‰¹å¾…åŠžï¼š$${taskName}ï¼Œå‘èµ·äººï¼š$${initiator}ï¼Œç”³è¯·æ—¶é—´ï¼š$${applyTime}ã€‚','2',1,'system','2026-11-01 15:27:22',NULL,NULL,0,NULL);
/*!40000 ALTER TABLE `sys_message_template` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'å…¬å‘ŠID',
  `notice_title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'å…¬å‘Šæ ‡é¢˜',
  `notice_type` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'å…¬å‘Šç±»åž‹ï¼ˆå­—å…¸ sys_notice_typeï¼‰',
  `notice_content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'å…¬å‘Šå†…å®¹',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT 'çŠ¶æ€ï¼ˆå­—å…¸ sys_notice_statusï¼‰',
  `is_top` tinyint NOT NULL DEFAULT '0' COMMENT 'æ˜¯å¦ç½®é¡¶ï¼š0-å¦ï¼Œ1-æ˜¯',
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ›´æ–°äºº',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤ï¼š0-æœªåˆ é™¤ï¼Œ1-å·²åˆ é™¤',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¤‡æ³¨',
  PRIMARY KEY (`id`),
  KEY `idx_status_top` (`status`,`is_top`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ç³»ç»Ÿå…¬å‘Šè¡¨';

LOCK TABLES `sys_notice` WRITE;
/*!40000 ALTER TABLE `sys_notice` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_notice` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_notice_read`;
CREATE TABLE `sys_notice_read` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'è®°å½•ID',
  `notice_id` bigint NOT NULL COMMENT 'å…¬å‘ŠID',
  `user_id` bigint NOT NULL COMMENT 'ç”¨æˆ·ID',
  `read_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'é˜…è¯»æ—¶é—´',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_user` (`notice_id`,`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='å…¬å‘Šå·²è¯»è®°å½•è¡¨';

LOCK TABLES `sys_notice_read` WRITE;
/*!40000 ALTER TABLE `sys_notice_read` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_notice_read` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_notification_delivery`;
CREATE TABLE `sys_notification_delivery` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'æŠ•é€’è®°å½•ID',
  `event_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ä¸šåŠ¡äº‹ä»¶ID',
  `event_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ä¸šåŠ¡äº‹ä»¶ç±»åž‹',
  `channel` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æ¸ é“ï¼šIN_APP/EMAIL/DINGTALK/SMS',
  `recipient_key` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æŽ¥æ”¶äººå”¯ä¸€é”®ï¼Œå¦‚ USER:123 æˆ– GROUP:default',
  `recipient_id` bigint DEFAULT NULL COMMENT 'æŽ¥æ”¶ç”¨æˆ·ID',
  `recipient_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æŽ¥æ”¶åœ°å€ï¼ˆé‚®ç®±/æ‰‹æœºå·ï¼‰',
  `template_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æ¨¡æ¿ç¼–ç ',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æ¶ˆæ¯æ ‡é¢˜',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æ¸²æŸ“åŽçš„æ¶ˆæ¯å†…å®¹',
  `params_json` text COLLATE utf8mb4_unicode_ci COMMENT 'æ¨¡æ¿å‚æ•°JSON',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'çŠ¶æ€ï¼šPENDING/SENDING/SUCCESS/FAILED/DEAD/SKIPPED',
  `attempts` int NOT NULL DEFAULT '0' COMMENT 'å·²å°è¯•æ¬¡æ•°',
  `max_attempts` int NOT NULL DEFAULT '5' COMMENT 'æœ€å¤§å°è¯•æ¬¡æ•°',
  `next_retry_time` datetime DEFAULT NULL COMMENT 'ä¸‹æ¬¡è‡ªåŠ¨é‡è¯•æ—¶é—´',
  `last_error` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æœ€åŽä¸€æ¬¡è„±æ•é”™è¯¯',
  `message_id` bigint DEFAULT NULL COMMENT 'ç«™å†…æ¶ˆæ¯IDï¼Œé‡è¯•æ—¶å¤ç”¨',
  `sent_time` datetime DEFAULT NULL COMMENT 'å‘é€æˆåŠŸæ—¶é—´',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_channel_recipient` (`event_id`,`channel`,`recipient_key`),
  KEY `idx_status_retry` (`status`,`next_retry_time`),
  KEY `idx_channel_create` (`channel`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='é€šçŸ¥æŠ•é€’è®°å½•';

LOCK TABLES `sys_notification_delivery` WRITE;
/*!40000 ALTER TABLE `sys_notification_delivery` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_notification_delivery` ENABLE KEYS */;
UNLOCK TABLES;

/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- ===== iboot_job =====

/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `QRTZ_BLOB_TRIGGERS`;
CREATE TABLE `QRTZ_BLOB_TRIGGERS` (
  `SCHED_NAME` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `BLOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `SCHED_NAME` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `QRTZ_BLOB_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `QRTZ_BLOB_TRIGGERS` WRITE;
/*!40000 ALTER TABLE `QRTZ_BLOB_TRIGGERS` DISABLE KEYS */;
/*!40000 ALTER TABLE `QRTZ_BLOB_TRIGGERS` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `QRTZ_CALENDARS`;
CREATE TABLE `QRTZ_CALENDARS` (
  `SCHED_NAME` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `CALENDAR_NAME` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `CALENDAR` blob NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`CALENDAR_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `QRTZ_CALENDARS` WRITE;
/*!40000 ALTER TABLE `QRTZ_CALENDARS` DISABLE KEYS */;
/*!40000 ALTER TABLE `QRTZ_CALENDARS` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `QRTZ_CRON_TRIGGERS`;
CREATE TABLE `QRTZ_CRON_TRIGGERS` (
  `SCHED_NAME` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `CRON_EXPRESSION` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TIME_ZONE_ID` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `QRTZ_CRON_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `QRTZ_CRON_TRIGGERS` WRITE;
/*!40000 ALTER TABLE `QRTZ_CRON_TRIGGERS` DISABLE KEYS */;
/*!40000 ALTER TABLE `QRTZ_CRON_TRIGGERS` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `QRTZ_FIRED_TRIGGERS`;
CREATE TABLE `QRTZ_FIRED_TRIGGERS` (
  `SCHED_NAME` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ENTRY_ID` varchar(95) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `INSTANCE_NAME` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `FIRED_TIME` bigint NOT NULL,
  `SCHED_TIME` bigint NOT NULL,
  `PRIORITY` int NOT NULL,
  `STATE` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `JOB_NAME` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `JOB_GROUP` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IS_NONCONCURRENT` varchar(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REQUESTS_RECOVERY` varchar(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`ENTRY_ID`),
  KEY `IDX_QRTZ_FT_TRIG_INST_NAME` (`SCHED_NAME`,`INSTANCE_NAME`),
  KEY `IDX_QRTZ_FT_INST_JOB_REQ_RCVRY` (`SCHED_NAME`,`INSTANCE_NAME`,`REQUESTS_RECOVERY`),
  KEY `IDX_QRTZ_FT_J_G` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_FT_JG` (`SCHED_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_FT_T_G` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_FT_TG` (`SCHED_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `QRTZ_FIRED_TRIGGERS` WRITE;
/*!40000 ALTER TABLE `QRTZ_FIRED_TRIGGERS` DISABLE KEYS */;
/*!40000 ALTER TABLE `QRTZ_FIRED_TRIGGERS` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `QRTZ_JOB_DETAILS`;
CREATE TABLE `QRTZ_JOB_DETAILS` (
  `SCHED_NAME` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `JOB_NAME` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `JOB_GROUP` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `DESCRIPTION` varchar(250) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `JOB_CLASS_NAME` varchar(250) COLLATE utf8mb4_unicode_ci NOT NULL,
  `IS_DURABLE` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `IS_NONCONCURRENT` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `IS_UPDATE_DATA` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REQUESTS_RECOVERY` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `JOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_J_REQ_RECOVERY` (`SCHED_NAME`,`REQUESTS_RECOVERY`),
  KEY `IDX_QRTZ_J_GRP` (`SCHED_NAME`,`JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `QRTZ_JOB_DETAILS` WRITE;
/*!40000 ALTER TABLE `QRTZ_JOB_DETAILS` DISABLE KEYS */;
/*!40000 ALTER TABLE `QRTZ_JOB_DETAILS` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `QRTZ_LOCKS`;
CREATE TABLE `QRTZ_LOCKS` (
  `SCHED_NAME` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `LOCK_NAME` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`LOCK_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `QRTZ_LOCKS` WRITE;
/*!40000 ALTER TABLE `QRTZ_LOCKS` DISABLE KEYS */;
/*!40000 ALTER TABLE `QRTZ_LOCKS` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `QRTZ_PAUSED_TRIGGER_GRPS`;
CREATE TABLE `QRTZ_PAUSED_TRIGGER_GRPS` (
  `SCHED_NAME` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `QRTZ_PAUSED_TRIGGER_GRPS` WRITE;
/*!40000 ALTER TABLE `QRTZ_PAUSED_TRIGGER_GRPS` DISABLE KEYS */;
/*!40000 ALTER TABLE `QRTZ_PAUSED_TRIGGER_GRPS` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `QRTZ_SCHEDULER_STATE`;
CREATE TABLE `QRTZ_SCHEDULER_STATE` (
  `SCHED_NAME` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `INSTANCE_NAME` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `LAST_CHECKIN_TIME` bigint NOT NULL,
  `CHECKIN_INTERVAL` bigint NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`INSTANCE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `QRTZ_SCHEDULER_STATE` WRITE;
/*!40000 ALTER TABLE `QRTZ_SCHEDULER_STATE` DISABLE KEYS */;
/*!40000 ALTER TABLE `QRTZ_SCHEDULER_STATE` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `QRTZ_SIMPLE_TRIGGERS`;
CREATE TABLE `QRTZ_SIMPLE_TRIGGERS` (
  `SCHED_NAME` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REPEAT_COUNT` bigint NOT NULL,
  `REPEAT_INTERVAL` bigint NOT NULL,
  `TIMES_TRIGGERED` bigint NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `QRTZ_SIMPLE_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `QRTZ_SIMPLE_TRIGGERS` WRITE;
/*!40000 ALTER TABLE `QRTZ_SIMPLE_TRIGGERS` DISABLE KEYS */;
/*!40000 ALTER TABLE `QRTZ_SIMPLE_TRIGGERS` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `QRTZ_SIMPROP_TRIGGERS`;
CREATE TABLE `QRTZ_SIMPROP_TRIGGERS` (
  `SCHED_NAME` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `STR_PROP_1` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STR_PROP_2` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STR_PROP_3` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `INT_PROP_1` int DEFAULT NULL,
  `INT_PROP_2` int DEFAULT NULL,
  `LONG_PROP_1` bigint DEFAULT NULL,
  `LONG_PROP_2` bigint DEFAULT NULL,
  `DEC_PROP_1` decimal(13,4) DEFAULT NULL,
  `DEC_PROP_2` decimal(13,4) DEFAULT NULL,
  `BOOL_PROP_1` varchar(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `BOOL_PROP_2` varchar(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `QRTZ_SIMPROP_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `QRTZ_SIMPROP_TRIGGERS` WRITE;
/*!40000 ALTER TABLE `QRTZ_SIMPROP_TRIGGERS` DISABLE KEYS */;
/*!40000 ALTER TABLE `QRTZ_SIMPROP_TRIGGERS` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `QRTZ_TRIGGERS`;
CREATE TABLE `QRTZ_TRIGGERS` (
  `SCHED_NAME` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `JOB_NAME` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `JOB_GROUP` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `DESCRIPTION` varchar(250) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `NEXT_FIRE_TIME` bigint DEFAULT NULL,
  `PREV_FIRE_TIME` bigint DEFAULT NULL,
  `PRIORITY` int DEFAULT NULL,
  `TRIGGER_STATE` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TRIGGER_TYPE` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL,
  `START_TIME` bigint NOT NULL,
  `END_TIME` bigint DEFAULT NULL,
  `CALENDAR_NAME` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `MISFIRE_INSTR` smallint DEFAULT NULL,
  `JOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_T_J` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_T_JG` (`SCHED_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_T_C` (`SCHED_NAME`,`CALENDAR_NAME`),
  KEY `IDX_QRTZ_T_G` (`SCHED_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_T_STATE` (`SCHED_NAME`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_N_STATE` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_N_G_STATE` (`SCHED_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_NEXT_FIRE_TIME` (`SCHED_NAME`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_ST` (`SCHED_NAME`,`TRIGGER_STATE`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_ST_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_NFT_ST_MISFIRE_GRP` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  CONSTRAINT `QRTZ_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) REFERENCES `QRTZ_JOB_DETAILS` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `QRTZ_TRIGGERS` WRITE;
/*!40000 ALTER TABLE `QRTZ_TRIGGERS` DISABLE KEYS */;
/*!40000 ALTER TABLE `QRTZ_TRIGGERS` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `db_backup_record`;
CREATE TABLE `db_backup_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'å¤‡ä»½è®°å½•ID',
  `backup_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'å¤‡ä»½ç¼–å·',
  `trigger_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'è§¦å‘æ–¹å¼ï¼šSCHEDULED/MANUAL',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'çŠ¶æ€ï¼šRUNNING/SUCCESS/FAILED',
  `database_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æºæ•°æ®åº“å',
  `storage_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å­˜å‚¨ç±»åž‹ï¼šlocal/minio/oss',
  `storage_path` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'åŠ å¯†å¤‡ä»½å¯¹è±¡è·¯å¾„',
  `ciphertext_size` bigint DEFAULT NULL COMMENT 'å¯†æ–‡å¤§å°ï¼ˆå­—èŠ‚ï¼‰',
  `plaintext_size` bigint DEFAULT NULL COMMENT 'æ˜Žæ–‡å¤§å°ï¼ˆå­—èŠ‚ï¼‰',
  `checksum_sha256` char(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ˜Žæ–‡ SHA-256',
  `encryption` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'åŠ å¯†ç®—æ³•',
  `dump_tool` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¯¼å‡ºå·¥å…·',
  `table_count` int DEFAULT NULL COMMENT 'è¡¨æ•°é‡',
  `table_stats_json` text COLLATE utf8mb4_unicode_ci COMMENT 'å¤‡ä»½æ—¶é€è¡¨è¡Œæ•°å¿«ç…§JSON',
  `started_at` datetime NOT NULL COMMENT 'å¼€å§‹æ—¶é—´',
  `finished_at` datetime DEFAULT NULL COMMENT 'ç»“æŸæ—¶é—´',
  `duration_ms` bigint DEFAULT NULL COMMENT 'è€—æ—¶ï¼ˆæ¯«ç§’ï¼‰',
  `error_message` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'è„±æ•åŽçš„é”™è¯¯ä¿¡æ¯',
  `created_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'è§¦å‘äºº',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_backup_no` (`backup_no`),
  KEY `idx_status_started` (`status`,`started_at`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='æ•°æ®åº“å¤‡ä»½è®°å½•';

LOCK TABLES `db_backup_record` WRITE;
/*!40000 ALTER TABLE `db_backup_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_backup_record` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `db_restore_record`;
CREATE TABLE `db_restore_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'æ¼”ç»ƒè®°å½•ID',
  `backup_id` bigint NOT NULL COMMENT 'å¤‡ä»½è®°å½•ID',
  `target_schema` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æ¼”ç»ƒåº“å',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'çŠ¶æ€ï¼šRUNNING/SUCCESS/MISMATCH/FAILED',
  `table_count` int DEFAULT NULL COMMENT 'æ¯”å¯¹è¡¨æ•°é‡',
  `table_stats_json` text COLLATE utf8mb4_unicode_ci COMMENT 'è¿˜åŽŸåŽé€è¡¨è¡Œæ•°JSON',
  `mismatch_json` text COLLATE utf8mb4_unicode_ci COMMENT 'è¡Œæ•°å·®å¼‚æ˜Žç»†JSON',
  `started_at` datetime NOT NULL COMMENT 'å¼€å§‹æ—¶é—´',
  `finished_at` datetime DEFAULT NULL COMMENT 'ç»“æŸæ—¶é—´',
  `duration_ms` bigint DEFAULT NULL COMMENT 'è€—æ—¶ï¼ˆæ¯«ç§’ï¼‰',
  `error_message` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'è„±æ•åŽçš„é”™è¯¯ä¿¡æ¯',
  `created_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'è§¦å‘äºº',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  KEY `idx_backup_id` (`backup_id`),
  KEY `idx_status_started` (`status`,`started_at`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='æ•°æ®åº“æ¢å¤æ¼”ç»ƒè®°å½•';

LOCK TABLES `db_restore_record` WRITE;
/*!40000 ALTER TABLE `db_restore_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `db_restore_record` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ä»»åŠ¡ID',
  `job_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ä»»åŠ¡åç§°',
  `job_group` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DEFAULT' COMMENT 'ä»»åŠ¡ç»„å',
  `dept_id` bigint DEFAULT NULL COMMENT 'åˆ›å»ºäººéƒ¨é—¨ ID',
  `invoke_target` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'è°ƒç”¨ç›®æ ‡å­—ç¬¦ä¸²(beanName.methodName)',
  `cron_expression` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'cronæ‰§è¡Œè¡¨è¾¾å¼',
  `misfire_policy` tinyint NOT NULL DEFAULT '3' COMMENT 'è®¡åˆ’æ‰§è¡Œé”™è¯¯ç­–ç•¥(1ç«‹å³æ‰§è¡Œ 2æ‰§è¡Œä¸€æ¬¡ 3æ”¾å¼ƒæ‰§è¡Œ)',
  `concurrent` tinyint NOT NULL DEFAULT '0' COMMENT 'æ˜¯å¦å¹¶å‘æ‰§è¡Œ(0ç¦æ­¢ 1å…è®¸)',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€(0æš‚åœ 1æ­£å¸¸)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'å¤‡æ³¨ä¿¡æ¯',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT 'åˆ é™¤æ ‡å¿—(0å­˜åœ¨ 1åˆ é™¤)',
  PRIMARY KEY (`id`),
  KEY `idx_job_group` (`job_group`),
  KEY `idx_job_dept_id` (`dept_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='å®šæ—¶ä»»åŠ¡è¡¨';

LOCK TABLES `sys_job` WRITE;
/*!40000 ALTER TABLE `sys_job` DISABLE KEYS */;
INSERT INTO `sys_job` VALUES (1,'ç³»ç»Ÿé»˜è®¤ï¼ˆæ— å‚ï¼‰','DEFAULT',NULL,'sampleTask.noParams','0/10 * * * * ?',3,0,0,'ç³»ç»Ÿé»˜è®¤ç¤ºä¾‹ä»»åŠ¡ï¼ˆæ— å‚æ•°ï¼‰','system','2026-11-01 15:27:23','','2026-11-01 15:27:23',0),(2,'ç³»ç»Ÿé»˜è®¤ï¼ˆæœ‰å‚ï¼‰','DEFAULT',NULL,'sampleTask.withParams(\'iboot\')','0/15 * * * * ?',3,0,0,'ç³»ç»Ÿé»˜è®¤ç¤ºä¾‹ä»»åŠ¡ï¼ˆæœ‰å‚æ•°ï¼‰','system','2026-11-01 15:27:23','','2026-11-01 15:27:23',0),(3,'ç³»ç»Ÿé»˜è®¤ï¼ˆå¤šå‚ï¼‰','DEFAULT',NULL,'sampleTask.multipleParams(\'iboot\', true, 2000L, 316.50D, 100)','0/20 * * * * ?',3,0,0,'ç³»ç»Ÿé»˜è®¤ç¤ºä¾‹ä»»åŠ¡ï¼ˆå¤šå‚æ•°ï¼‰','system','2026-11-01 15:27:23','','2026-11-01 15:27:23',0),(4,'æ•°æ®åº“å¤‡ä»½','SYSTEM',NULL,'backupTask.run','0 0 2 * * ?',3,0,1,'æ¯å¤©å‡Œæ™¨å…¨é‡å¯¼å‡ºæ•°æ®åº“ï¼ŒåŠ å¯†åŽå†™å…¥å¯¹è±¡å­˜å‚¨','system','2026-11-01 15:27:23','','2026-11-01 15:27:23',0);
/*!40000 ALTER TABLE `sys_job` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sys_job_log`;
CREATE TABLE `sys_job_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'æ—¥å¿—ID',
  `job_id` bigint NOT NULL COMMENT 'ä»»åŠ¡ID',
  `job_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ä»»åŠ¡åç§°',
  `job_group` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ä»»åŠ¡ç»„å',
  `invoke_target` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'è°ƒç”¨ç›®æ ‡å­—ç¬¦ä¸²',
  `job_message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'æ—¥å¿—ä¿¡æ¯',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT 'æ‰§è¡ŒçŠ¶æ€(0å¤±è´¥ 1æˆåŠŸ)',
  `exception_info` text COLLATE utf8mb4_unicode_ci COMMENT 'å¼‚å¸¸ä¿¡æ¯',
  `start_time` datetime DEFAULT NULL COMMENT 'å¼€å§‹æ—¶é—´',
  `end_time` datetime DEFAULT NULL COMMENT 'ç»“æŸæ—¶é—´',
  `cost_time` bigint DEFAULT '0' COMMENT 'æ‰§è¡Œè€—æ—¶(æ¯«ç§’)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  PRIMARY KEY (`id`),
  KEY `idx_job_id` (`job_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='å®šæ—¶ä»»åŠ¡æ‰§è¡Œæ—¥å¿—è¡¨';

LOCK TABLES `sys_job_log` WRITE;
/*!40000 ALTER TABLE `sys_job_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_job_log` ENABLE KEYS */;
UNLOCK TABLES;

/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- ===== iboot_workflow =====

/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `ACT_EVT_LOG`;
CREATE TABLE `ACT_EVT_LOG` (
  `LOG_NR_` bigint NOT NULL AUTO_INCREMENT,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TIME_STAMP_` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DATA_` longblob,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `IS_PROCESSED_` tinyint DEFAULT '0',
  PRIMARY KEY (`LOG_NR_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_EVT_LOG` WRITE;
/*!40000 ALTER TABLE `ACT_EVT_LOG` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_EVT_LOG` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_GE_BYTEARRAY`;
CREATE TABLE `ACT_GE_BYTEARRAY` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTES_` longblob,
  `GENERATED_` tinyint DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_BYTEAR_DEPL` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_BYTEARR_DEPL` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `ACT_RE_DEPLOYMENT` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_GE_BYTEARRAY` WRITE;
/*!40000 ALTER TABLE `ACT_GE_BYTEARRAY` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_GE_BYTEARRAY` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_GE_PROPERTY`;
CREATE TABLE `ACT_GE_PROPERTY` (
  `NAME_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `VALUE_` varchar(300) COLLATE utf8mb3_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  PRIMARY KEY (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_GE_PROPERTY` WRITE;
/*!40000 ALTER TABLE `ACT_GE_PROPERTY` DISABLE KEYS */;
INSERT INTO `ACT_GE_PROPERTY` VALUES ('common.schema.version','8.0.0.0',1),('next.dbid','1',1),('schema.history','create(8.0.0.0)',1),('schema.version','8.0.0.0',1);
/*!40000 ALTER TABLE `ACT_GE_PROPERTY` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_HI_ACTINST`;
CREATE TABLE `ACT_HI_ACTINST` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALL_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `COMPLETED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `TRANSACTION_ORDER_` int DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_ACT_INST_START` (`START_TIME_`),
  KEY `ACT_IDX_HI_ACT_INST_END` (`END_TIME_`),
  KEY `ACT_IDX_HI_ACT_INST_PROCINST` (`PROC_INST_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_HI_ACT_INST_EXEC` (`EXECUTION_ID_`,`ACT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_HI_ACTINST` WRITE;
/*!40000 ALTER TABLE `ACT_HI_ACTINST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_ACTINST` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_HI_ATTACHMENT`;
CREATE TABLE `ACT_HI_ATTACHMENT` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `URL_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CONTENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_HI_ATTACHMENT` WRITE;
/*!40000 ALTER TABLE `ACT_HI_ATTACHMENT` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_ATTACHMENT` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_HI_COMMENT`;
CREATE TABLE `ACT_HI_COMMENT` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TIME_` datetime(3) NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACTION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `MESSAGE_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `FULL_MSG_` longblob,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_HI_COMMENT` WRITE;
/*!40000 ALTER TABLE `ACT_HI_COMMENT` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_COMMENT` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_HI_DETAIL`;
CREATE TABLE `ACT_HI_DETAIL` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `VAR_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  `TIME_` datetime(3) NOT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_DETAIL_PROC_INST` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_DETAIL_ACT_INST` (`ACT_INST_ID_`),
  KEY `ACT_IDX_HI_DETAIL_TIME` (`TIME_`),
  KEY `ACT_IDX_HI_DETAIL_NAME` (`NAME_`),
  KEY `ACT_IDX_HI_DETAIL_TASK_ID` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_HI_DETAIL` WRITE;
/*!40000 ALTER TABLE `ACT_HI_DETAIL` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_DETAIL` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_HI_ENTITYLINK`;
CREATE TABLE `ACT_HI_ENTITYLINK` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `LINK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HIERARCHY_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_ENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_REF_SCOPE` (`REF_SCOPE_ID_`,`REF_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_ROOT_SCOPE` (`ROOT_SCOPE_ID_`,`ROOT_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_HI_ENTITYLINK` WRITE;
/*!40000 ALTER TABLE `ACT_HI_ENTITYLINK` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_ENTITYLINK` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_HI_IDENTITYLINK`;
CREATE TABLE `ACT_HI_IDENTITYLINK` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_USER` (`USER_ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_TASK` (`TASK_ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_PROCINST` (`PROC_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_HI_IDENTITYLINK` WRITE;
/*!40000 ALTER TABLE `ACT_HI_IDENTITYLINK` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_IDENTITYLINK` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_HI_PROCINST`;
CREATE TABLE `ACT_HI_PROCINST` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `END_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUPER_PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `END_USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `PROC_INST_ID_` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_PRO_INST_END` (`END_TIME_`),
  KEY `ACT_IDX_HI_PRO_I_BUSKEY` (`BUSINESS_KEY_`),
  KEY `ACT_IDX_HI_PRO_SUPER_PROCINST` (`SUPER_PROCESS_INSTANCE_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_HI_PROCINST` WRITE;
/*!40000 ALTER TABLE `ACT_HI_PROCINST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_PROCINST` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_HI_TASKINST`;
CREATE TABLE `ACT_HI_TASKINST` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `IN_PROGRESS_TIME_` datetime(3) DEFAULT NULL,
  `IN_PROGRESS_STARTED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CLAIM_TIME_` datetime(3) DEFAULT NULL,
  `CLAIMED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUSPENDED_TIME_` datetime(3) DEFAULT NULL,
  `SUSPENDED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `COMPLETED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `PRIORITY_` int DEFAULT NULL,
  `IN_PROGRESS_DUE_DATE_` datetime(3) DEFAULT NULL,
  `DUE_DATE_` datetime(3) DEFAULT NULL,
  `FORM_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_TASK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_INST_PROCINST` (`PROC_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_HI_TASKINST` WRITE;
/*!40000 ALTER TABLE `ACT_HI_TASKINST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_TASKINST` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_HI_TSK_LOG`;
CREATE TABLE `ACT_HI_TSK_LOG` (
  `ID_` bigint NOT NULL AUTO_INCREMENT,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `TIME_STAMP_` timestamp(3) NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DATA_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_ACT_HI_TSK_LOG_TASK` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_HI_TSK_LOG` WRITE;
/*!40000 ALTER TABLE `ACT_HI_TSK_LOG` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_TSK_LOG` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_HI_VARINST`;
CREATE TABLE `ACT_HI_VARINST` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `VAR_TYPE_` varchar(100) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_PROCVAR_NAME_TYPE` (`NAME_`,`VAR_TYPE_`),
  KEY `ACT_IDX_HI_VAR_SCOPE_ID_TYPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_VAR_SUB_ID_TYPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_PROCVAR_PROC_INST` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_PROCVAR_TASK_ID` (`TASK_ID_`),
  KEY `ACT_IDX_HI_PROCVAR_EXE` (`EXECUTION_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_HI_VARINST` WRITE;
/*!40000 ALTER TABLE `ACT_HI_VARINST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_VARINST` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_PROCDEF_INFO`;
CREATE TABLE `ACT_PROCDEF_INFO` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `INFO_JSON_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_INFO_PROCDEF` (`PROC_DEF_ID_`),
  KEY `ACT_IDX_INFO_PROCDEF` (`PROC_DEF_ID_`),
  KEY `ACT_FK_INFO_JSON_BA` (`INFO_JSON_ID_`),
  CONSTRAINT `ACT_FK_INFO_JSON_BA` FOREIGN KEY (`INFO_JSON_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_INFO_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_PROCDEF_INFO` WRITE;
/*!40000 ALTER TABLE `ACT_PROCDEF_INFO` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_PROCDEF_INFO` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_RE_DEPLOYMENT`;
CREATE TABLE `ACT_RE_DEPLOYMENT` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `DEPLOY_TIME_` timestamp(3) NULL DEFAULT NULL,
  `DERIVED_FROM_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_ROOT_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ENGINE_VERSION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_RE_DEPLOYMENT` WRITE;
/*!40000 ALTER TABLE `ACT_RE_DEPLOYMENT` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RE_DEPLOYMENT` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_RE_MODEL`;
CREATE TABLE `ACT_RE_MODEL` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LAST_UPDATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EDITOR_SOURCE_VALUE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EDITOR_SOURCE_EXTRA_VALUE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_FK_MODEL_SOURCE` (`EDITOR_SOURCE_VALUE_ID_`),
  KEY `ACT_FK_MODEL_SOURCE_EXTRA` (`EDITOR_SOURCE_EXTRA_VALUE_ID_`),
  KEY `ACT_FK_MODEL_DEPLOYMENT` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_MODEL_DEPLOYMENT` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `ACT_RE_DEPLOYMENT` (`ID_`),
  CONSTRAINT `ACT_FK_MODEL_SOURCE` FOREIGN KEY (`EDITOR_SOURCE_VALUE_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_MODEL_SOURCE_EXTRA` FOREIGN KEY (`EDITOR_SOURCE_EXTRA_VALUE_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_RE_MODEL` WRITE;
/*!40000 ALTER TABLE `ACT_RE_MODEL` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RE_MODEL` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_RE_PROCDEF`;
CREATE TABLE `ACT_RE_PROCDEF` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `VERSION_` int NOT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `RESOURCE_NAME_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DGRM_RESOURCE_NAME_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `HAS_START_FORM_KEY_` tinyint DEFAULT NULL,
  `HAS_GRAPHICAL_NOTATION_` tinyint DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `ENGINE_VERSION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_ROOT_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_VERSION_` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_PROCDEF` (`KEY_`,`VERSION_`,`DERIVED_VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_RE_PROCDEF` WRITE;
/*!40000 ALTER TABLE `ACT_RE_PROCDEF` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RE_PROCDEF` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_RU_ACTINST`;
CREATE TABLE `ACT_RU_ACTINST` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALL_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `COMPLETED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `TRANSACTION_ORDER_` int DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_RU_ACTI_START` (`START_TIME_`),
  KEY `ACT_IDX_RU_ACTI_END` (`END_TIME_`),
  KEY `ACT_IDX_RU_ACTI_PROC` (`PROC_INST_ID_`),
  KEY `ACT_IDX_RU_ACTI_PROC_ACT` (`PROC_INST_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_RU_ACTI_EXEC` (`EXECUTION_ID_`),
  KEY `ACT_IDX_RU_ACTI_EXEC_ACT` (`EXECUTION_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_RU_ACTI_TASK` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_RU_ACTINST` WRITE;
/*!40000 ALTER TABLE `ACT_RU_ACTINST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_ACTINST` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_RU_DEADLETTER_JOB`;
CREATE TABLE `ACT_RU_DEADLETTER_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_DJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_DJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_DJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_DEADLETTER_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_DEADLETTER_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_RU_DEADLETTER_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_DEADLETTER_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_DEADLETTER_JOB` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_RU_ENTITYLINK`;
CREATE TABLE `ACT_RU_ENTITYLINK` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `LINK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HIERARCHY_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_ENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_REF_SCOPE` (`REF_SCOPE_ID_`,`REF_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_ROOT_SCOPE` (`ROOT_SCOPE_ID_`,`ROOT_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_RU_ENTITYLINK` WRITE;
/*!40000 ALTER TABLE `ACT_RU_ENTITYLINK` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_ENTITYLINK` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_RU_EVENT_SUBSCR`;
CREATE TABLE `ACT_RU_EVENT_SUBSCR` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `EVENT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EVENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACTIVITY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CONFIGURATION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATED_` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EVENT_SUBSCR_CONFIG_` (`CONFIGURATION_`),
  KEY `ACT_IDX_EVENT_SUBSCR_EXEC_ID` (`EXECUTION_ID_`),
  KEY `ACT_IDX_EVENT_SUBSCR_PROC_ID` (`PROC_INST_ID_`),
  KEY `ACT_IDX_EVENT_SUBSCR_SCOPEREF_` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  CONSTRAINT `ACT_FK_EVENT_EXEC` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_RU_EVENT_SUBSCR` WRITE;
/*!40000 ALTER TABLE `ACT_RU_EVENT_SUBSCR` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_EVENT_SUBSCR` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_RU_EXECUTION`;
CREATE TABLE `ACT_RU_EXECUTION` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUPER_EXEC_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IS_ACTIVE_` tinyint DEFAULT NULL,
  `IS_CONCURRENT_` tinyint DEFAULT NULL,
  `IS_SCOPE_` tinyint DEFAULT NULL,
  `IS_EVENT_SCOPE_` tinyint DEFAULT NULL,
  `IS_MI_ROOT_` tinyint DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `CACHED_ENT_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IS_COUNT_ENABLED_` tinyint DEFAULT NULL,
  `EVT_SUBSCR_COUNT_` int DEFAULT NULL,
  `TASK_COUNT_` int DEFAULT NULL,
  `JOB_COUNT_` int DEFAULT NULL,
  `TIMER_JOB_COUNT_` int DEFAULT NULL,
  `SUSP_JOB_COUNT_` int DEFAULT NULL,
  `DEADLETTER_JOB_COUNT_` int DEFAULT NULL,
  `EXTERNAL_WORKER_JOB_COUNT_` int DEFAULT NULL,
  `VAR_COUNT_` int DEFAULT NULL,
  `ID_LINK_COUNT_` int DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EXEC_BUSKEY` (`BUSINESS_KEY_`),
  KEY `ACT_IDC_EXEC_ROOT` (`ROOT_PROC_INST_ID_`),
  KEY `ACT_IDX_EXEC_REF_ID_` (`REFERENCE_ID_`),
  KEY `ACT_FK_EXE_PROCINST` (`PROC_INST_ID_`),
  KEY `ACT_FK_EXE_PARENT` (`PARENT_ID_`),
  KEY `ACT_FK_EXE_SUPER` (`SUPER_EXEC_`),
  KEY `ACT_FK_EXE_PROCDEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_EXE_PARENT` FOREIGN KEY (`PARENT_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`) ON DELETE CASCADE,
  CONSTRAINT `ACT_FK_EXE_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_EXE_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `ACT_FK_EXE_SUPER` FOREIGN KEY (`SUPER_EXEC_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_RU_EXECUTION` WRITE;
/*!40000 ALTER TABLE `ACT_RU_EXECUTION` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_EXECUTION` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_RU_EXTERNAL_JOB`;
CREATE TABLE `ACT_RU_EXTERNAL_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_EJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_EJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_EJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  CONSTRAINT `ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_EXTERNAL_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_RU_EXTERNAL_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_EXTERNAL_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_EXTERNAL_JOB` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_RU_HISTORY_JOB`;
CREATE TABLE `ACT_RU_HISTORY_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ADV_HANDLER_CFG_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_RU_HISTORY_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_HISTORY_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_HISTORY_JOB` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_RU_IDENTITYLINK`;
CREATE TABLE `ACT_RU_IDENTITYLINK` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_IDENT_LNK_USER` (`USER_ID_`),
  KEY `ACT_IDX_IDENT_LNK_GROUP` (`GROUP_ID_`),
  KEY `ACT_IDX_IDENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_IDENT_LNK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_IDENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_ATHRZ_PROCEDEF` (`PROC_DEF_ID_`),
  KEY `ACT_FK_TSKASS_TASK` (`TASK_ID_`),
  KEY `ACT_FK_IDL_PROCINST` (`PROC_INST_ID_`),
  CONSTRAINT `ACT_FK_ATHRZ_PROCEDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_IDL_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_TSKASS_TASK` FOREIGN KEY (`TASK_ID_`) REFERENCES `ACT_RU_TASK` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_RU_IDENTITYLINK` WRITE;
/*!40000 ALTER TABLE `ACT_RU_IDENTITYLINK` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_IDENTITYLINK` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_RU_JOB`;
CREATE TABLE `ACT_RU_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_JOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_JOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_JOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_RU_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_JOB` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_RU_SUSPENDED_JOB`;
CREATE TABLE `ACT_RU_SUSPENDED_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_SJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_SJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_SJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_SUSPENDED_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_SUSPENDED_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_RU_SUSPENDED_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_SUSPENDED_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_SUSPENDED_JOB` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_RU_TASK`;
CREATE TABLE `ACT_RU_TASK` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DELEGATION_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PRIORITY_` int DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `IN_PROGRESS_TIME_` datetime(3) DEFAULT NULL,
  `IN_PROGRESS_STARTED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CLAIM_TIME_` datetime(3) DEFAULT NULL,
  `CLAIMED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUSPENDED_TIME_` datetime(3) DEFAULT NULL,
  `SUSPENDED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IN_PROGRESS_DUE_DATE_` datetime(3) DEFAULT NULL,
  `DUE_DATE_` datetime(3) DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `FORM_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IS_COUNT_ENABLED_` tinyint DEFAULT NULL,
  `VAR_COUNT_` int DEFAULT NULL,
  `ID_LINK_COUNT_` int DEFAULT NULL,
  `SUB_TASK_COUNT_` int DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_TASK_CREATE` (`CREATE_TIME_`),
  KEY `ACT_IDX_TASK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TASK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TASK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_TASK_EXE` (`EXECUTION_ID_`),
  KEY `ACT_FK_TASK_PROCINST` (`PROC_INST_ID_`),
  KEY `ACT_FK_TASK_PROCDEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_TASK_EXE` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_TASK_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_TASK_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_RU_TASK` WRITE;
/*!40000 ALTER TABLE `ACT_RU_TASK` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_TASK` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_RU_TIMER_JOB`;
CREATE TABLE `ACT_RU_TIMER_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_TIMER_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_TIMER_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_TIMER_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_TIMER_JOB_DUEDATE` (`DUEDATE_`),
  KEY `ACT_IDX_TJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_TIMER_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_TIMER_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_RU_TIMER_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_TIMER_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_TIMER_JOB` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ACT_RU_VARIABLE`;
CREATE TABLE `ACT_RU_VARIABLE` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_RU_VAR_SCOPE_ID_TYPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_RU_VAR_SUB_ID_TYPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_VAR_BYTEARRAY` (`BYTEARRAY_ID_`),
  KEY `ACT_IDX_VARIABLE_TASK_ID` (`TASK_ID_`),
  KEY `ACT_FK_VAR_EXE` (`EXECUTION_ID_`),
  KEY `ACT_FK_VAR_PROCINST` (`PROC_INST_ID_`),
  CONSTRAINT `ACT_FK_VAR_BYTEARRAY` FOREIGN KEY (`BYTEARRAY_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_VAR_EXE` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_VAR_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `ACT_RU_VARIABLE` WRITE;
/*!40000 ALTER TABLE `ACT_RU_VARIABLE` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_VARIABLE` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `FLW_RU_BATCH`;
CREATE TABLE `FLW_RU_BATCH` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `SEARCH_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY2_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) NOT NULL,
  `COMPLETE_TIME_` datetime(3) DEFAULT NULL,
  `STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BATCH_DOC_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `FLW_RU_BATCH` WRITE;
/*!40000 ALTER TABLE `FLW_RU_BATCH` DISABLE KEYS */;
/*!40000 ALTER TABLE `FLW_RU_BATCH` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `FLW_RU_BATCH_PART`;
CREATE TABLE `FLW_RU_BATCH_PART` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `BATCH_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY2_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) NOT NULL,
  `COMPLETE_TIME_` datetime(3) DEFAULT NULL,
  `STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RESULT_DOC_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `FLW_IDX_BATCH_PART` (`BATCH_ID_`),
  CONSTRAINT `FLW_FK_BATCH_PART_PARENT` FOREIGN KEY (`BATCH_ID_`) REFERENCES `FLW_RU_BATCH` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

LOCK TABLES `FLW_RU_BATCH_PART` WRITE;
/*!40000 ALTER TABLE `FLW_RU_BATCH_PART` DISABLE KEYS */;
/*!40000 ALTER TABLE `FLW_RU_BATCH_PART` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `biz_leave_application`;
CREATE TABLE `biz_leave_application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ç”³è¯·ID',
  `applicant_id` bigint NOT NULL COMMENT 'ç”³è¯·äººç”¨æˆ·ID',
  `approver_id` bigint NOT NULL COMMENT 'å®¡æ‰¹äººç”¨æˆ·ID',
  `leave_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'è¯·å‡ç±»åž‹',
  `start_time` datetime NOT NULL COMMENT 'å¼€å§‹æ—¶é—´',
  `end_time` datetime NOT NULL COMMENT 'ç»“æŸæ—¶é—´',
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'è¯·å‡äº‹ç”±',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'çŠ¶æ€ï¼šPENDING/APPROVED/REJECTED',
  `process_instance_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æµç¨‹å®žä¾‹ID',
  `approve_comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å®¡æ‰¹æ„è§æˆ–é©³å›žåŽŸå› ',
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ›´æ–°äºº',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤ï¼š0-æœªåˆ é™¤ï¼Œ1-å·²åˆ é™¤',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_process_instance_id` (`process_instance_id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_approver_id` (`approver_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='è¯·å‡ç”³è¯·';

LOCK TABLES `biz_leave_application` WRITE;
/*!40000 ALTER TABLE `biz_leave_application` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_leave_application` ENABLE KEYS */;
UNLOCK TABLES;

/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- ===== iboot_ai =====

/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `ai_agent`;
CREATE TABLE `ai_agent` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'æ™ºèƒ½ä½“ID',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æ™ºèƒ½ä½“åç§°',
  `description` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æè¿°',
  `system_prompt` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ç³»ç»Ÿæç¤ºè¯',
  `model` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ¨¡åž‹æ ‡è¯†ï¼Œç©ºè¡¨ç¤ºä½¿ç”¨æœåŠ¡é»˜è®¤æ¨¡åž‹',
  `tool_allowlist_json` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'å·¥å…·ç™½åå• JSON æ•°ç»„',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€ï¼š0-åœç”¨ï¼Œ1-å¯ç”¨',
  `created_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `deleted` bigint NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤ï¼š0-æœªåˆ é™¤ï¼Œåˆ é™¤æ—¶ç½®ä¸ºID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_name` (`name`,`deleted`),
  KEY `idx_agent_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='æ™ºèƒ½ä½“å®šä¹‰';

LOCK TABLES `ai_agent` WRITE;
/*!40000 ALTER TABLE `ai_agent` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_agent` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ai_agent_run`;
CREATE TABLE `ai_agent_run` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'è¿è¡ŒID',
  `conversation_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ä¼šè¯ID',
  `agent_id` bigint NOT NULL COMMENT 'æ™ºèƒ½ä½“ID',
  `user_id` bigint NOT NULL COMMENT 'å‘èµ·ç”¨æˆ·ID',
  `status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'RUNNING/PENDING_APPROVAL/EXECUTING/COMPLETED/REJECTED/EXPIRED/FAILED',
  `pending_tool` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¾…ç¡®è®¤å·¥å…·å',
  `pending_args_json` mediumtext COLLATE utf8mb4_unicode_ci COMMENT 'å¾…ç¡®è®¤å‚æ•° JSON',
  `pending_args_digest` char(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¾…ç¡®è®¤å‚æ•° SHA-256',
  `pending_expires_at` datetime DEFAULT NULL COMMENT 'å¾…ç¡®è®¤è¿‡æœŸæ—¶é—´',
  `approval_granted_at` datetime DEFAULT NULL COMMENT 'ç¡®è®¤é€šè¿‡æ—¶é—´',
  `step_count` int NOT NULL DEFAULT '0' COMMENT 'æ¨¡åž‹è°ƒç”¨æ­¥æ•°',
  `steps_json` mediumtext COLLATE utf8mb4_unicode_ci COMMENT 'æ­¥éª¤è½¨è¿¹ JSONï¼ˆåªå­˜æ‘˜è¦ï¼‰',
  `input_tokens` bigint NOT NULL DEFAULT '0' COMMENT 'è¾“å…¥ token ç´¯è®¡',
  `output_tokens` bigint NOT NULL DEFAULT '0' COMMENT 'è¾“å‡º token ç´¯è®¡',
  `error_code` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¤±è´¥åŽŸå› ç ',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `finished_at` datetime DEFAULT NULL COMMENT 'ç»“æŸæ—¶é—´',
  PRIMARY KEY (`id`),
  KEY `idx_run_user_time` (`user_id`,`create_time`),
  KEY `idx_run_conversation` (`conversation_id`),
  KEY `idx_run_pending` (`status`,`pending_expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='æ™ºèƒ½ä½“è¿è¡Œè®°å½•';

LOCK TABLES `ai_agent_run` WRITE;
/*!40000 ALTER TABLE `ai_agent_run` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_agent_run` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ai_document`;
CREATE TABLE `ai_document` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'æ–‡æ¡£ID',
  `kb_id` bigint NOT NULL COMMENT 'çŸ¥è¯†åº“ID',
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'åŽŸå§‹æ–‡ä»¶å',
  `file_ext` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ‰©å±•å',
  `file_size` bigint NOT NULL DEFAULT '0' COMMENT 'æ–‡ä»¶å¤§å°',
  `mime_type` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'MIMEç±»åž‹',
  `storage_key` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'å¯¹è±¡å­˜å‚¨è·¯å¾„',
  `content_hash` char(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'å†…å®¹ SHA-256',
  `owner_user_id` bigint NOT NULL COMMENT 'ä¸Šä¼ äººç”¨æˆ·ID',
  `owner_dept_id` bigint NOT NULL COMMENT 'ä¸Šä¼ äººéƒ¨é—¨ID',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/READY/FAILED',
  `error_code` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¤±è´¥åŽŸå› ç ',
  `error_message` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å¤±è´¥è¯´æ˜Ž',
  `chunk_count` int NOT NULL DEFAULT '0' COMMENT 'åˆ†å—æ•°é‡',
  `attempts` int NOT NULL DEFAULT '0' COMMENT 'å·²å°è¯•æ¬¡æ•°',
  `next_attempt_at` datetime DEFAULT NULL COMMENT 'ä¸‹æ¬¡é‡è¯•æ—¶é—´',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `deleted` bigint NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤ï¼š0-æœªåˆ é™¤ï¼Œåˆ é™¤æ—¶ç½®ä¸ºID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_hash` (`kb_id`,`content_hash`,`deleted`),
  KEY `idx_doc_kb_status` (`kb_id`,`status`),
  KEY `idx_doc_claim` (`status`,`next_attempt_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='çŸ¥è¯†åº“æ–‡æ¡£';

LOCK TABLES `ai_document` WRITE;
/*!40000 ALTER TABLE `ai_document` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_document` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ai_document_chunk`;
CREATE TABLE `ai_document_chunk` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'åˆ†å—ID',
  `doc_id` bigint NOT NULL COMMENT 'æ–‡æ¡£ID',
  `chunk_index` int NOT NULL COMMENT 'åˆ†å—åºå·',
  `page_no` int DEFAULT NULL COMMENT 'é¡µç ',
  `heading_path` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æ ‡é¢˜è·¯å¾„',
  `content` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'åˆ†å—æ­£æ–‡',
  `token_count` int NOT NULL DEFAULT '0' COMMENT 'ä¼°ç®— token æ•°',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_chunk` (`doc_id`,`chunk_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='çŸ¥è¯†åº“æ–‡æ¡£åˆ†å—';

LOCK TABLES `ai_document_chunk` WRITE;
/*!40000 ALTER TABLE `ai_document_chunk` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_document_chunk` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ai_kb_share_dept`;
CREATE TABLE `ai_kb_share_dept` (
  `kb_id` bigint NOT NULL COMMENT 'çŸ¥è¯†åº“ID',
  `dept_id` bigint NOT NULL COMMENT 'å…±äº«éƒ¨é—¨ID',
  PRIMARY KEY (`kb_id`,`dept_id`),
  KEY `idx_kb_share_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='çŸ¥è¯†åº“å…±äº«éƒ¨é—¨';

LOCK TABLES `ai_kb_share_dept` WRITE;
/*!40000 ALTER TABLE `ai_kb_share_dept` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_kb_share_dept` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `ai_knowledge_base`;
CREATE TABLE `ai_knowledge_base` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'çŸ¥è¯†åº“ID',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'çŸ¥è¯†åº“åç§°',
  `description` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'æè¿°',
  `visibility` tinyint NOT NULL DEFAULT '1' COMMENT 'å¯è§èŒƒå›´ï¼š1-å…¨éƒ¨ï¼Œ2-è‡ªå®šä¹‰ï¼Œ3-æœ¬éƒ¨é—¨ï¼Œ4-æœ¬éƒ¨é—¨åŠä»¥ä¸‹ï¼Œ5-ä»…æœ¬äºº',
  `owner_user_id` bigint NOT NULL COMMENT 'æ‰€æœ‰è€…ç”¨æˆ·ID',
  `owner_dept_id` bigint DEFAULT NULL COMMENT 'æ‰€æœ‰è€…éƒ¨é—¨ID',
  `created_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `deleted` bigint NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤ï¼š0-æœªåˆ é™¤ï¼Œåˆ é™¤æ—¶ç½®ä¸ºID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kb_owner_name` (`owner_user_id`,`name`,`deleted`),
  KEY `idx_kb_owner_dept` (`owner_dept_id`),
  KEY `idx_kb_visibility` (`visibility`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='çŸ¥è¯†åº“';

LOCK TABLES `ai_knowledge_base` WRITE;
/*!40000 ALTER TABLE `ai_knowledge_base` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_knowledge_base` ENABLE KEYS */;
UNLOCK TABLES;

/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
SET FOREIGN_KEY_CHECKS=1;
SET UNIQUE_CHECKS=1;
