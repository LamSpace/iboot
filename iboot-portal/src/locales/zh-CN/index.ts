import common from "./common.json";
import login from "./login.json";
import register from "./register.json";
import dashboard from "./dashboard.json";
import home from "./home.json";

// System module
import systemUser from "./system/user.json";
import systemRole from "./system/role.json";
import systemMenu from "./system/menu.json";
import systemDict from "./system/dict.json";
import systemConfig from "./system/config.json";

// Organization module
import organizationPost from "./organization/post.json";
import organizationDept from "./organization/dept.json";
import organizationOrgChart from "./organization/org-chart.json";

// Security module
import securityLoginLog from "./security/login-log.json";
import securityOperateLog from "./security/operate-log.json";
import securityAuditLog from "./security/audit-log.json";

// Monitor module
import monitorOnlineUser from "./monitor/online-user.json";
import monitorJob from "./monitor/job.json";
import monitorServer from "./monitor/server.json";
import monitorRedis from "./monitor/redis.json";
import monitorSql from "./monitor/sql.json";
import monitorSwagger from "./monitor/swagger.json";
import monitorPerformance from "./monitor/performance.json";
import monitorPrometheus from "./monitor/prometheus.json";
import monitorGrafana from "./monitor/grafana.json";
import monitorKibana from "./monitor/kibana.json";
import monitorAlertmanager from "./monitor/alertmanager.json";
import monitorThanos from "./monitor/thanos.json";
import monitorMinio from "./monitor/minio.json";

// Message module
import messageNotice from "./message/notice.json";
import messageCenter from "./message/message-center.json";

// Analysis module
import analysisReport from "./analysis/report.json";
import analysisUsage from "./analysis/usage.json";

// File module
import fileManage from "./file/file-manage.json";

export default {
  common,
  login,
  register,
  dashboard,
  home,
  system: {
    user: systemUser,
    role: systemRole,
    menu: systemMenu,
    dict: systemDict,
    config: systemConfig,
  },
  organization: {
    post: organizationPost,
    dept: organizationDept,
    orgChart: organizationOrgChart,
  },
  security: {
    loginLog: securityLoginLog,
    operateLog: securityOperateLog,
    auditLog: securityAuditLog,
  },
  monitor: {
    onlineUser: monitorOnlineUser,
    job: monitorJob,
    server: monitorServer,
    redis: monitorRedis,
    sql: monitorSql,
    swagger: monitorSwagger,
    performance: monitorPerformance,
    prometheus: monitorPrometheus,
    grafana: monitorGrafana,
    kibana: monitorKibana,
    alertmanager: monitorAlertmanager,
    thanos: monitorThanos,
    minio: monitorMinio,
  },
  message: {
    notice: messageNotice,
    messageCenter: messageCenter,
  },
  analysis: {
    report: analysisReport,
    usage: analysisUsage,
  },
  file: {
    fileManage: fileManage,
  },
};
