-- ============================================================
-- WOMS 精简演示数据（10 工单版）
-- 清空业务表 → 重建 admin → 2运维+3用户 → 6分类/10设备 → 10工单 → 流转记录
-- 保留：sys_role / sys_menu / sys_role_menu（权限配置不重建）
-- 注意：TRUNCATE 不可逆；所有账号密码均为 123456
-- ============================================================
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE work_order_record;
TRUNCATE work_order;
TRUNCATE device;
TRUNCATE device_category;
TRUNCATE sys_user_role;
TRUNCATE sys_user;
TRUNCATE device_log;
SET FOREIGN_KEY_CHECKS = 1;

-- ============ 1. admin（角色1=超管，已有配置） ============
INSERT INTO sys_user (id, username, password, nickname, status, create_time, update_time, deleted) VALUES
    (1, 'admin', '$2a$10$q357/WDeI1FPKV61raVTD.t4EDp8JgKteHELQRYXcBdQcSf5iJBnW', '超级管理员', 0, NOW(), NOW(), 0);
INSERT INTO sys_user_role (id, user_id, role_id, create_time) VALUES (1001, 1, 1, NOW());

-- ============ 2. 用户：2运维 + 3用户（role: 2=运维 3=用户） ============
INSERT INTO sys_user (id, username, password, nickname, status, work_days, create_time, update_time, deleted) VALUES
                                                                                                                  (10001, 'wangqiang', '$2a$10$q357/WDeI1FPKV61raVTD.t4EDp8JgKteHELQRYXcBdQcSf5iJBnW', '王强', 0, '1,2,3,4,5', '2025-03-01 09:00:00', NOW(), 0),
                                                                                                                  (10002, 'liwei',     '$2a$10$q357/WDeI1FPKV61raVTD.t4EDp8JgKteHELQRYXcBdQcSf5iJBnW', '李伟', 0, '1,2,3,4,5', '2025-04-01 09:00:00', NOW(), 0),
                                                                                                                  (10003, 'linxiao',   '$2a$10$q357/WDeI1FPKV61raVTD.t4EDp8JgKteHELQRYXcBdQcSf5iJBnW', '林晓', 0, NULL, '2025-05-01 09:00:00', NOW(), 0),
                                                                                                                  (10004, 'chenchen',  '$2a$10$q357/WDeI1FPKV61raVTD.t4EDp8JgKteHELQRYXcBdQcSf5iJBnW', '陈晨', 0, NULL, '2025-06-01 09:00:00', NOW(), 0),
                                                                                                                  (10005, 'liuyang',   '$2a$10$q357/WDeI1FPKV61raVTD.t4EDp8JgKteHELQRYXcBdQcSf5iJBnW', '刘洋', 0, NULL, '2025-07-01 09:00:00', NOW(), 0);

INSERT INTO sys_user_role (id, user_id, role_id, create_time) VALUES
                                                                  (1002, 10001, 2, NOW()),
                                                                  (1003, 10002, 2, NOW()),
                                                                  (1004, 10003, 3, NOW()),
                                                                  (1005, 10004, 3, NOW()),
                                                                  (1006, 10005, 3, NOW());

-- ============ 3. 设备分类 6 个 ============
INSERT INTO device_category (id, parent_id, category_name, sort, status, create_time, update_time, deleted) VALUES
                                                                                                                (30001, 0, '电脑',     1, 0, NOW(), NOW(), 0),
                                                                                                                (30002, 0, '打印机',   2, 0, NOW(), NOW(), 0),
                                                                                                                (30003, 0, '空调',     3, 0, NOW(), NOW(), 0),
                                                                                                                (30004, 0, '网络设备', 4, 0, NOW(), NOW(), 0),
                                                                                                                (30005, 0, '投影仪',   5, 0, NOW(), NOW(), 0),
                                                                                                                (30006, 0, '扫描仪',   6, 0, NOW(), NOW(), 0);

-- ============ 4. 设备 10 台 ============
INSERT INTO device (id, device_code, device_name, category_id, brand, model, location, status, principal_id, user_name, purchase_date, warranty_end_date, remark, create_time, update_time, deleted) VALUES
                                                                                                                                                                                                         (20001, 'PC-001',  '联想台式电脑',   30001, '联想',   'ThinkCentre M930', '3楼办公室', 0, 10003, '林晓', '2023-03-15', '2026-03-14', '主力办公机', NOW(), NOW(), 0),
                                                                                                                                                                                                         (20002, 'PC-002',  '戴尔台式电脑',   30001, '戴尔',   'OptiPlex 7010',    '2楼办公室', 0, 10004, '陈晨', '2023-05-20', '2026-05-19', NULL, NOW(), NOW(), 0),
                                                                                                                                                                                                         (20003, 'PRT-001', '惠普激光打印机', 30002, '惠普',   'LaserJet M405',    '3楼打印区', 0, 10005, '刘洋', '2023-02-10', '2026-02-09', '公共打印', NOW(), NOW(), 0),
                                                                                                                                                                                                         (20004, 'PRT-002', '佳能激光打印机', 30002, '佳能',   'iR2206N',          '2楼打印区', 0, 10003, '林晓', '2023-04-18', '2026-04-17', NULL, NOW(), NOW(), 0),
                                                                                                                                                                                                         (20005, 'AC-001',  '格力柜式空调',   30003, '格力',   'KFR-120LW',        '3楼大厅',   0, NULL, NULL, '2022-06-01', '2025-05-31', '大功率柜机', NOW(), NOW(), 0),
                                                                                                                                                                                                         (20006, 'AC-002',  '美的挂式空调',   30003, '美的',   'KFR-35GW',         '2楼办公室', 0, NULL, NULL, '2023-05-15', '2026-05-14', NULL, NOW(), NOW(), 0),
                                                                                                                                                                                                         (20007, 'NET-001', '华为交换机',     30004, '华为',   'S5735S',           '3楼弱电间', 0, NULL, NULL, '2023-01-10', '2026-01-09', '核心交换机', NOW(), NOW(), 0),
                                                                                                                                                                                                         (20008, 'NET-002', 'TP-LINK路由器',  30004, 'TP-LINK','XDR6088',          '1楼机房',   0, NULL, NULL, '2024-04-01', '2027-03-31', '无线覆盖', NOW(), NOW(), 0),
                                                                                                                                                                                                         (20009, 'PJT-001', '爱普生投影仪',   30005, '爱普生', 'CB-X50',           '3楼大会议室',0, NULL, NULL, '2023-03-08', '2026-03-07', '培训会议', NOW(), NOW(), 0),
                                                                                                                                                                                                         (20010, 'SCN-001', '佳能扫描仪',     30006, '佳能',   'DR-C240',          '财务部',    0, 10004, '陈晨', '2024-01-10', '2027-01-09', NULL, NOW(), NOW(), 0);

-- ============ 5. 工单 10 条 ============
-- 状态：4=已完成(4条) 5=已关闭(2条) 1=待接单(1条) 2=处理中(1条) 3=待复核(1条) 6=已驳回(1条)
-- 押金：全部 20；deposit_refunded：1-6 已退=1，7-10 在途=0
INSERT INTO work_order (id, order_no, title, description, status, reporter_id, reporter_name, device_id, device_name,
                        priority, need_type, overdue, version, deposit, deposit_refunded,
                        assignee_id, assignee_name, appoint_date, time_slot, repair_content, repair_cost, evaluate_score, evaluate_comment,
                        accept_time, finish_time, close_time, create_time, update_time, deleted) VALUES
-- ① 已完成：王强 9月收入（到账明细 4 条）
(40001, 'WO202609020001', '实验室投影仪不亮', '开机无画面，电源灯正常', '4', 10003, '林晓', 20009, '爱普生投影仪',
 2, '蓝', 0, 0, 20.00, 1, 10001, '王强', '2026-09-02', '上午9:00-11:00', '更换投影灯泡并调试', 150.00, 5.0, '技术过硬',
 '2026-09-02 09:10:00', '2026-09-02 11:30:00', NULL, '2026-09-01 10:00:00', NOW(), 0),

(40002, 'WO202609050001', '办公室空调不制冷', '出风正常但不制冷', '4', 10004, '陈晨', 20005, '格力柜式空调',
 1, '红', 0, 0, 20.00, 1, 10001, '王强', '2026-09-05', '下午14:30-17:00', '补充制冷剂并清洗滤网', 260.00, 4.5, '处理及时，服务专业',
 '2026-09-05 14:40:00', '2026-09-05 17:20:00', NULL, '2026-09-04 09:00:00', NOW(), 0),

(40003, 'WO202609100001', '门禁卡无法识别', '刷卡无反应', '4', 10005, '刘洋', 20007, '华为交换机',
 2, '蓝', 0, 0, 20.00, 1, 10001, '王强', '2026-09-10', '上午9:00-11:00', '重写门禁卡数据', 80.00, 4.0, '维修效果满意',
 '2026-09-10 09:20:00', '2026-09-10 11:10:00', NULL, '2026-09-09 15:00:00', NOW(), 0),

-- ② 已完成：8月（不算本月收入，算累计）
(40004, 'WO202608250001', '打印机频繁卡纸', '打印时频繁卡纸', '4', 10003, '林晓', 20003, '惠普激光打印机',
 2, '绿', 0, 0, 20.00, 1, 10001, '王强', '2026-08-25', '上午9:00-11:00', '更换搓纸轮组件', 200.00, 5.0, '响应速度快',
 '2026-08-25 09:05:00', '2026-08-25 11:40:00', NULL, '2026-08-24 10:00:00', NOW(), 0),

-- ③ 已关闭 2 条（押金已退）
(40005, 'WO202609010002', '机房空调漏水', '排水管堵塞漏水', '5', 10004, '陈晨', 20006, '美的挂式空调',
 1, '红', 0, 0, 20.00, 1, 10001, '王强', '2026-09-01', '下午14:30-17:00', '疏通排水管并加固', 120.00, 4.5, '服务态度好',
 '2026-09-01 14:30:00', '2026-09-01 16:50:00', '2026-09-08 16:00:00', '2026-08-31 09:00:00', NOW(), 0),

(40006, 'WO202609030001', '电脑运行卡顿', '开机慢，软件卡顿', '5', 10005, '刘洋', 20002, '戴尔台式电脑',
 2, '蓝', 0, 0, 20.00, 1, 10001, '王强', '2026-09-03', '上午9:00-11:00', '重装系统并更新驱动', 90.00, 4.0, '处理及时',
 '2026-09-03 09:10:00', '2026-09-03 11:00:00', '2026-09-11 10:00:00', '2026-09-02 14:00:00', NOW(), 0),

-- ④ 在途 4 条（押金未退）
(40007, 'WO202609140001', '会议室投影仪无信号', '接电脑无画面', '1', 10003, '林晓', 20009, '爱普生投影仪',
 2, '蓝', 0, 0, 20.00, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, '2026-09-14 10:00:00', NOW(), 0),

(40008, 'WO202609150001', '扫描仪扫描模糊', '扫描文件模糊不清', '2', 10004, '陈晨', 20010, '佳能扫描仪',
 2, '蓝', 0, 0, 20.00, 0, 10002, '李伟', '2026-09-15', '下午14:30-17:00', NULL, NULL, NULL, NULL,
 '2026-09-15 14:30:00', NULL, NULL, '2026-09-14 15:00:00', NOW(), 0),

(40009, 'WO202609120001', '网络交换机指示灯异常', '部分端口不通', '3', 10005, '刘洋', 20007, '华为交换机',
 1, '红', 0, 0, 20.00, 0, 10001, '王强', '2026-09-12', '上午9:00-11:00', '更换故障端口模块', 300.00, NULL, NULL,
 '2026-09-12 09:20:00', '2026-09-12 11:30:00', NULL, '2026-09-11 09:00:00', NOW(), 0),

(40010, 'WO202609080001', '空调不制热', '冬天制热效果差', '6', 10003, '林晓', 20006, '美的挂式空调',
 1, '红', 0, 0, 20.00, 0, 10001, '王强', '2026-09-08', '上午9:00-11:00', NULL, NULL, NULL, NULL,
 '2026-09-08 09:15:00', NULL, NULL, '2026-09-07 10:00:00', NOW(), 0);

-- ============ 6. 流转记录（手写，清晰对应每条工单） ============
INSERT INTO work_order_record (id, order_id, operator_id, operator_name, action, from_status, to_status, remark, create_time) VALUES
-- 40001 完成：提交→指派→复核→验收
(50001, 40001, 10003, '林晓', 'submit',  NULL, '1', '报修人提交工单', '2026-09-01 10:00:00'),
(50002, 40001, 10001, '王强', 'assign',  '1', '2', '指派处理人：王强', '2026-09-02 09:10:00'),
(50003, 40001, 10001, '王强', 'process', '2', '3', '提交维修方案待复核', '2026-09-02 11:00:00'),
(50004, 40001, 10003, '林晓', 'review',  '3', '4', '验收通过，评分：5.0', '2026-09-02 11:30:00'),
-- 40002 完成
(50005, 40002, 10004, '陈晨', 'submit',  NULL, '1', '报修人提交工单', '2026-09-04 09:00:00'),
(50006, 40002, 10001, '王强', 'assign',  '1', '2', '指派处理人：王强', '2026-09-05 14:40:00'),
(50007, 40002, 10001, '王强', 'process', '2', '3', '提交维修方案待复核', '2026-09-05 16:50:00'),
(50008, 40002, 10004, '陈晨', 'review',  '3', '4', '验收通过，评分：4.5', '2026-09-05 17:20:00'),
-- 40003 完成
(50009, 40003, 10005, '刘洋', 'submit',  NULL, '1', '报修人提交工单', '2026-09-09 15:00:00'),
(50010, 40003, 10001, '王强', 'assign',  '1', '2', '指派处理人：王强', '2026-09-10 09:20:00'),
(50011, 40003, 10001, '王强', 'process', '2', '3', '提交维修方案待复核', '2026-09-10 10:50:00'),
(50012, 40003, 10005, '刘洋', 'review',  '3', '4', '验收通过，评分：4.0', '2026-09-10 11:10:00'),
-- 40004 完成（8月）
(50013, 40004, 10003, '林晓', 'submit',  NULL, '1', '报修人提交工单', '2026-08-24 10:00:00'),
(50014, 40004, 10001, '王强', 'assign',  '1', '2', '指派处理人：王强', '2026-08-25 09:05:00'),
(50015, 40004, 10001, '王强', 'process', '2', '3', '提交维修方案待复核', '2026-08-25 11:00:00'),
(50016, 40004, 10003, '林晓', 'review',  '3', '4', '验收通过，评分：5.0', '2026-08-25 11:40:00'),
-- 40005 关闭：…→验收→归档
(50017, 40005, 10004, '陈晨', 'submit',  NULL, '1', '报修人提交工单', '2026-08-31 09:00:00'),
(50018, 40005, 10001, '王强', 'assign',  '1', '2', '指派处理人：王强', '2026-09-01 14:30:00'),
(50019, 40005, 10001, '王强', 'process', '2', '3', '提交维修方案待复核', '2026-09-01 16:20:00'),
(50020, 40005, 10004, '陈晨', 'review',  '3', '4', '验收通过，评分：4.5', '2026-09-01 16:50:00'),
(50021, 40005, 10001, '王强', 'close',   '4', '5', '验收通过后归档关闭', '2026-09-08 16:00:00'),
-- 40006 关闭
(50022, 40006, 10005, '刘洋', 'submit',  NULL, '1', '报修人提交工单', '2026-09-02 14:00:00'),
(50023, 40006, 10001, '王强', 'assign',  '1', '2', '指派处理人：王强', '2026-09-03 09:10:00'),
(50024, 40006, 10001, '王强', 'process', '2', '3', '提交维修方案待复核', '2026-09-03 10:40:00'),
(50025, 40006, 10005, '刘洋', 'review',  '3', '4', '验收通过，评分：4.0', '2026-09-03 11:00:00'),
(50026, 40006, 10001, '王强', 'close',   '4', '5', '验收通过后归档关闭', '2026-09-11 10:00:00'),
-- 40007 待接单：仅提交
(50027, 40007, 10003, '林晓', 'submit',  NULL, '1', '报修人提交工单', '2026-09-14 10:00:00'),
-- 40008 处理中：提交→指派
(50028, 40008, 10004, '陈晨', 'submit',  NULL, '1', '报修人提交工单', '2026-09-14 15:00:00'),
(50029, 40008, 10002, '李伟', 'assign',  '1', '2', '指派处理人：李伟', '2026-09-15 14:30:00'),
-- 40009 待复核：提交→指派→复核
(50030, 40009, 10005, '刘洋', 'submit',  NULL, '1', '报修人提交工单', '2026-09-11 09:00:00'),
(50031, 40009, 10001, '王强', 'assign',  '1', '2', '指派处理人：王强', '2026-09-12 09:20:00'),
(50032, 40009, 10001, '王强', 'process', '2', '3', '提交维修方案待复核', '2026-09-12 11:00:00'),
-- 40010 已驳回：提交→指派→复核→驳回
(50033, 40010, 10003, '林晓', 'submit',  NULL, '1', '报修人提交工单', '2026-09-07 10:00:00'),
(50034, 40010, 10001, '王强', 'assign',  '1', '2', '指派处理人：王强', '2026-09-08 09:15:00'),
(50035, 40010, 10001, '王强', 'process', '2', '3', '提交维修方案待复核', '2026-09-08 10:40:00'),
(50036, 40010, 10003, '林晓', 'reject',  '3', '6', '验收不通过，驳回返工', '2026-09-08 11:00:00');

-- ============ 7. 收尾：设备状态重算 + 超时标记 ============
-- 设备状态：有待接单=离线(1)、有在修=维修中(2)、否则在线(0)
UPDATE device SET status = 0 WHERE deleted = 0;
UPDATE device d SET status = 1 WHERE d.deleted = 0 AND EXISTS
    (SELECT 1 FROM work_order w WHERE w.device_id = d.id AND w.deleted = 0 AND w.status = '1');
UPDATE device d SET status = 2 WHERE d.deleted = 0 AND EXISTS
    (SELECT 1 FROM work_order w WHERE w.device_id = d.id AND w.deleted = 0 AND w.status IN ('2','3','6'));

-- 待接单超过 2 天标记超时（演示工单监控用）
UPDATE work_order SET overdue = 1 WHERE deleted = 0 AND status = '1'
                                    AND create_time < DATE_SUB(NOW(), INTERVAL 2 DAY);

-- ============ 8. 校验（跑完看结果） ============
SELECT status, COUNT(*) AS cnt FROM work_order GROUP BY status ORDER BY status;
-- 王强收入：累计 = 150+260+80+200 = 690；本月(9月) = 490；本月接单 = 3
SELECT COALESCE(SUM(repair_cost),0) AS total_income,
       COALESCE(SUM(CASE WHEN finish_time >= '2026-09-01' THEN repair_cost ELSE 0 END),0) AS month_income,
       COUNT(*) AS month_orders
FROM work_order WHERE assignee_id = 10001 AND status = '4';
-- 林晓押金：累计 5×20=100；在途 2×20=40；已退 3×20=60
SELECT COALESCE(SUM(CASE WHEN deposit_refunded=0 THEN deposit ELSE 0 END),0) AS pending,
       COALESCE(SUM(CASE WHEN deposit_refunded=1 THEN deposit ELSE 0 END),0) AS refunded,
       COALESCE(SUM(deposit),0) AS total
FROM work_order WHERE reporter_id = 10003;






SELECT id, order_no, status, assignee_id, assignee_name
FROM work_order
WHERE order_no = '你接的工单号';




UPDATE work_order
SET status = '1', assignee_id = NULL, assignee_name = NULL,
    accept_time = NULL, appoint_date = NULL, time_slot = NULL
WHERE status = '2' AND assignee_id IS NULL;





-- 1) 还有没有"处理中但处理人为空"的坏工单？
SELECT COUNT(*) FROM work_order WHERE status = '2' AND assignee_id IS NULL;

-- 2) 待接单池里有多少可接的？
SELECT COUNT(*) FROM work_order WHERE status = '1';

-- 3) 你接过的单处理人是谁？（换成你刚接的单号）
SELECT id, order_no, status, assignee_id, assignee_name FROM work_order WHERE order_no = '实际单号';




-- 1) 工单按状态分布（看现在到底有什么工单）
SELECT status, COUNT(*) FROM work_order GROUP BY status;

-- 2) 最近 20 条工单明细（看处理人是谁）
SELECT id, order_no, status, assignee_id, assignee_name
FROM work_order ORDER BY create_time DESC LIMIT 20;



SELECT id, order_no, status, assignee_id, assignee_name
FROM work_order
WHERE id IN (2100161665446031361, 2100161642696126465, 2100161610546786305, 40008, 40007, 40009);


SELECT id, status, assignee_id, assignee_name, appoint_date, time_slot
FROM work_order
WHERE assignee_id = 3;



-- 1) 消息表加对话线字段
ALTER TABLE work_order_message
    ADD COLUMN assignee_id BIGINT NULL COMMENT '所属处理人对话线(orderId+assigneeId)' AFTER order_id;

-- 2) 历史消息回填：把旧消息挂到工单当前处理人名下（无处理人的保持 NULL，仅管理员可见）
UPDATE work_order_message m
    LEFT JOIN work_order w ON w.id = m.order_id
SET m.assignee_id = w.assignee_id
WHERE m.assignee_id IS NULL AND w.assignee_id IS NOT NULL;



-- ===== ① 类型修正：系统管理→目录(0)，五个子菜单→菜单(1) =====
UPDATE sys_menu SET menu_type = 0 WHERE id = 5;              -- 系统管理
UPDATE sys_menu SET menu_type = 1 WHERE id IN (6, 7, 8, 9, 10); -- 用户/角色/菜单/操作日志/设备日志

-- ===== ② 排序重排（按 Layout 实际菜单顺序，去重）=====
-- 数据统计1、工单管理2、我的工单3(新)、我的接单4(新)、工单监控5、设备管理6、设备分类7、个人用户8(新)、系统管理9
UPDATE sys_menu SET sort = 5 WHERE id = 11;  -- 工单监控（原3，去重）
UPDATE sys_menu SET sort = 6 WHERE id = 3;   -- 设备管理（原3）
UPDATE sys_menu SET sort = 7 WHERE id = 4;   -- 设备分类（原4）
UPDATE sys_menu SET sort = 9 WHERE id = 5;   -- 系统管理（原5）



-- 1) 排行 SQL 的分组结果（看"3"是否在里面、名字是什么）
SELECT assignee_id, assignee_name, COUNT(*) AS cnt
FROM work_order
WHERE deleted = 0 AND status IN ('2','3','4','5') AND assignee_id IS NOT NULL
GROUP BY assignee_id, assignee_name
ORDER BY cnt DESC;

-- 2) 运维"3"的已完成单明细（确认 assignee 数据是否写全）
SELECT id, order_no, status, assignee_id, assignee_name, finish_time
FROM work_order
WHERE assignee_id = 2100159544294223874;


-- ===== ③ 补菜单项：我的工单 / 我的接单 / 个人用户 =====

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, status, create_time, update_time, deleted) VALUES
                                                                                                                                              (2099000000000000001, 0, '我的工单', 0, '/workorder/my',    'workorder/MyWorkOrders', 'workorder:my',   'Document', 3, 0, NOW(), NOW(), 0),
                                                                                                                                              (2099000000000000002, 0, '我的接单', 0, '/workorder/mine',  'workorder/MyWorkOrder',  'workorder:mine', 'Calendar', 4, 0, NOW(), NOW(), 0),
                                                                                                                                              (2099000000000000003, 0, '个人用户', 0, '/profile',         'profile',                'profile:view',  'Avatar',   8, 0, NOW(), NOW(), 0);
-- 回填你自己创建的测试设备（把编号换成实际列表；username 换成你的登录名）
UPDATE device SET creator_id = (SELECT id FROM sys_user WHERE username = '4' LIMIT 1)
WHERE device_code IN ('123', '12312', '333444', '333')
  AND creator_id IS NULL AND deleted = 0;



-- 回填你自己创建的测试分类（把分类名换成实际的；username 换成你的登录名）
UPDATE device_category SET creator_id = (SELECT id FROM sys_user WHERE username = '4' LIMIT 1)
WHERE category_name IN ('...') AND creator_id IS NULL AND deleted = 0;



-- 确认列是否存在
SHOW COLUMNS FROM work_order_message;


UPDATE work_order_message m
    JOIN work_order w ON w.id = m.order_id
SET m.assignee_id = w.assignee_id
WHERE m.assignee_id IS NULL;
