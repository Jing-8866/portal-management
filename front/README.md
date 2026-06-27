# 系统门户管理平台 - 前端

## 项目简介

系统门户管理平台前端项目，基于原生 HTML5 + CSS3 + JavaScript ES6+ 构建，无需任何构建工具，浏览器直接运行。

## 技术栈

- HTML5
- CSS3（CSS Variables、Flexbox、Grid）
- JavaScript ES6+（Fetch API、async/await）
- FontAwesome 6.4（图标库，CDN引入） 图标参考地址 ： https://fontawesome.com/search
   - 常见三种前缀
     例如： ①`<i class="fas fa-database"></i>`
     ②`<i class="fa-solid fa-database"></i>`。官方推荐第②种新写法
      
  
   | 前缀 | 说明 |
   | ---- | ---- |
   | fas |  Solid（实心）| 
   | far |  Regular（线框）| 
   | fab |  Brands（品牌logo）| 

## 目录结构

```
front/
├── login.html                          # 登录页面
├── index.html                          # 门户首页（子系统卡片）
├── profile.html                        # 个人信息页面
├── README.md                           # 项目说明
├── css/
│   ├── style.css                       # 全局样式
│   └── responsive.css                  # 响应式样式
├── js/
│   ├── config.js                       # 全局配置与工具函数
│   └── main.js                         # 门户首页逻辑
└── systems/
    ├── user-management/                # 用户管理子系统
    │   ├── index.html                  # 用户管理
    │   ├── roles.html                  # 角色管理
    │   ├── role-assign.html            # 用户角色分配
    │   ├── user-roles.html             # 查询用户角色
    │   ├── perm-menu.html              # 子系统权限分配
    │   ├── perm-data.html              # 数据权限管理（开发中）
    │   ├── css/
    │   │   ├── style.css
    │   │   └── responsive.css
    │   └── js/
    │       └── main.js
    ├── order-system/                   # 订单管理子系统
    │   ├── index.html
    │   ├── css/
    │   │   ├── style.css
    │   │   └── responsive.css
    │   └── js/
    │       ├── config.js
    │       └── main.js
    ├── db-management/                  # 数据库管理子系统
    │   ├── index.html                  # 实例管理
    │   ├── tables.html                 # 表结构查看
    │   ├── css/
    │   │   ├── style.css
    │   │   └── responsive.css
    │   └── js/
    │       ├── config.js
    │       └── main.js
    └── log-analysis/                   # 日志分析子系统
        ├── index.html
        ├── css/
        │   ├── style.css
        │   └── responsive.css
        └── js/
            ├── config.js
            └── main.js
```

## 快速开始

### 环境要求

- 现代浏览器（Chrome、Firefox、Edge）
- 后端服务运行在 `http://localhost:8080`

### 运行方式

本项目为纯静态页面，无需构建，直接通过以下任一方式访问：

1. **直接打开**：用浏览器打开 `login.html`
2. **HTTP Server**（推荐，避免跨域问题）：
   ```bash
   # Python 方式
   cd front
   python -m http.server 3000

   # Node.js 方式
   npx http-server -p 3000
   ```
   访问 `http://localhost:3000/login.html`

### 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin  | 123456 | 平台管理员 |

## 功能模块

### 门户首页
- 子系统卡片展示与搜索
- 管理员可新增/编辑/删除/停用子系统
- 停用系统灰色显示，不可进入

### 用户管理子系统
- **用户管理**：CRUD、搜索筛选、前端分页、Excel导入导出、状态管理
- **角色管理**：角色CRUD、状态筛选
- **用户角色分配**：为角色批量分配用户
- **查询用户角色**：查看用户所拥有的角色
- **子系统权限分配**：按角色/按用户分配子系统访问权限
- **数据权限管理**：开发中

### 订单管理子系统
- 订单列表、统计卡片、搜索筛选、状态管理

### 数据库管理子系统
- 实例管理：卡片展示、CRUD、测试连接、刷新
- 表结构查看：字段列表、DDL复制

### 日志分析子系统
- 操作日志查询、多条件筛选（级别/来源/日期范围）
- 动态统计卡片、管理员可删除日志

## 权限控制

- 基于 JWT Token 认证
- `isAdmin()` 判断当前用户是否为管理员（PLATFORM_ADMIN 或 SUBSYSTEM_ADMIN）
- 非管理员隐藏写操作按钮（新增/编辑/删除/导入/状态切换）
- 未登录自动跳转登录页

## 配置说明

全局配置位于 `js/config.js`：

```javascript
const CONFIG = {
    API_BASE_URL: 'http://localhost:8080/api',  // 后端接口地址
    TOKEN_KEY: 'portal_token',                   // Token存储键名
    USER_KEY: 'portal_user'                      // 用户信息存储键名
};
```

如需修改后端地址，只需更改 `API_BASE_URL` 即可。

## 对应后端

后端项目位于 `../backend`，基于 Spring Boot 2.7.18 + MyBatis Plus + Spring Security + JWT。

## 浏览器兼容性

- Chrome 80+
- Firefox 75+
- Edge 80+
- Safari 13+
