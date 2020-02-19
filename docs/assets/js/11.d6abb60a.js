(window.webpackJsonp=window.webpackJsonp||[]).push([[11],{216:function(t,e,r){"use strict";r.r(e);var l=r(0),a=Object(l.a)({},(function(){var t=this,e=t.$createElement,r=t._self._c||e;return r("ContentSlotsDistributor",{attrs:{"slot-key":t.$parent.slotKey}},[r("h3",{attrs:{id:"管理接口"}},[r("a",{staticClass:"header-anchor",attrs:{href:"#管理接口"}},[t._v("#")]),t._v(" 管理接口")]),t._v(" "),r("p",[t._v("具体API接口文档，请查阅"),r("a",{attrs:{href:"https://github.com/turms-im/turms/blob/develop/turms/docs/html/swagger.html",target:"_blank",rel:"noopener noreferrer"}},[t._v("Turms的Swagger文档"),r("OutboundLink")],1),t._v("，或在dist/config/application.yaml配置文件下，添加“spring.profiles.active=dev”属性，并在服务端重启后，访问http://localhost:9510/swagger-ui.html")]),t._v(" "),r("h4",{attrs:{id:"接口设计准则"}},[r("a",{staticClass:"header-anchor",attrs:{href:"#接口设计准则"}},[t._v("#")]),t._v(" 接口设计准则")]),t._v(" "),r("p",[t._v("为了让接口顾名思义，保证开发者能一目了然，turms的管理接口设计在参考RESTful设计上做了进一步优化与统一，具体遵循以下准则：")]),t._v(" "),r("ul",[r("li",[t._v("URL的路径部分代表目标资源（如“/users/relationships”），或是资源的表现形式（如“/users/relationships/page”表示以分页的形式返回资源）")]),t._v(" "),r("li",[t._v("POST方法用于Create资源，DELETE方法用于Delete资源，PUT方法用于Update资源，GET方法用于Query资源，以及比较特殊的HEAD方法用于Check资源（类似于GET，但无Response body，仅通过HTTP状态码交互）")]),t._v(" "),r("li",[t._v("请求的Query string用于定位资源或是附加指令。如：“?ids=1,2,3”（定位资源）或“?shouldReset=true”（附加指令）")]),t._v(" "),r("li",[t._v("请求的Body用于描述要创建或更新的数据")])]),t._v(" "),r("h4",{attrs:{id:"管理接口使用对象"}},[r("a",{staticClass:"header-anchor",attrs:{href:"#管理接口使用对象"}},[t._v("#")]),t._v(" 管理接口使用对象")]),t._v(" "),r("p",[t._v("①您的前端或后端程序发出HTTP请求进行调用；")]),t._v(" "),r("p",[t._v("②作为内容统计管理系统与集群监控管理系统的"),r("a",{attrs:{href:"https://github.com/turms-im/turms/tree/develop/turms-admin",target:"_blank",rel:"noopener noreferrer"}},[t._v("turms-admin"),r("OutboundLink")],1),t._v("使用。")]),t._v(" "),r("h3",{attrs:{id:"类别"}},[r("a",{staticClass:"header-anchor",attrs:{href:"#类别"}},[t._v("#")]),t._v(" 类别")]),t._v(" "),r("h4",{attrs:{id:"非业务相关类"}},[r("a",{staticClass:"header-anchor",attrs:{href:"#非业务相关类"}},[t._v("#")]),t._v(" 非业务相关类")]),t._v(" "),r("table",[r("thead",[r("tr",[r("th",{staticStyle:{"text-align":"left"}},[r("strong",[t._v("种类")])]),t._v(" "),r("th",{staticStyle:{"text-align":"left"}},[r("strong",[t._v("Controller")])]),t._v(" "),r("th",[r("strong",[t._v("补充")])])])]),t._v(" "),r("tbody",[r("tr",[r("td",{staticStyle:{"text-align":"left"}},[t._v("管理员管理")]),t._v(" "),r("td",{staticStyle:{"text-align":"left"}},[t._v("AdminController")]),t._v(" "),r("td",[t._v("每个Turms集群默认存在一个角色为“ROOT”，userId为“turms”，且password随机生成的超级管理员账号")])]),t._v(" "),r("tr",[r("td",{staticStyle:{"text-align":"left"}},[t._v("管理员操作日志管理")]),t._v(" "),r("td",{staticStyle:{"text-align":"left"}},[t._v("AdminActionLogController")]),t._v(" "),r("td")]),t._v(" "),r("tr",[r("td",{staticStyle:{"text-align":"left"}},[t._v("管理员权限管理")]),t._v(" "),r("td",{staticStyle:{"text-align":"left"}},[t._v("AdminRoleController")]),t._v(" "),r("td",[t._v("每个Turms集群默认存在一个角色为“ROOT”的超级管理员角色，其具有所有权限")])]),t._v(" "),r("tr",[r("td",{staticStyle:{"text-align":"left"}},[t._v("集群管理")]),t._v(" "),r("td",{staticStyle:{"text-align":"left"}},[t._v("ClusterController")]),t._v(" "),r("td")]),t._v(" "),r("tr",[r("td",{staticStyle:{"text-align":"left"}},[t._v("原因管理")]),t._v(" "),r("td",{staticStyle:{"text-align":"left"}},[t._v("ReasonController")]),t._v(" "),r("td",[t._v("（对用户开放）服务降级。主要用于当浏览器客户端登陆/连接失败时，其可通过该Controller所申明的接口查询失败原因")])]),t._v(" "),r("tr",[r("td",{staticStyle:{"text-align":"left"}},[t._v("路由管理")]),t._v(" "),r("td",{staticStyle:{"text-align":"left"}},[t._v("RouterController")]),t._v(" "),r("td",[t._v("（对用户开放）服务降级。当浏览器客户端因试图连接不对其负责的Turms服务端而登录失败时，其可通过该Controller所申明的接口来查询应对该用户负责的其他Turms服务端IP")])])])]),t._v(" "),r("h4",{attrs:{id:"业务相关类"}},[r("a",{staticClass:"header-anchor",attrs:{href:"#业务相关类"}},[t._v("#")]),t._v(" 业务相关类")]),t._v(" "),r("table",[r("thead",[r("tr",[r("th",{staticStyle:{"text-align":"left"}},[r("strong",[t._v("种类")])]),t._v(" "),r("th",{staticStyle:{"text-align":"left"}},[r("strong",[t._v("Controller")])])])]),t._v(" "),r("tbody",[r("tr",[r("td",{staticStyle:{"text-align":"left"}},[t._v("用户管理")]),t._v(" "),r("td",{staticStyle:{"text-align":"left"}},[t._v("UserController"),r("br"),t._v("UserOnlineInfoController"),r("br"),t._v("UserRelationshipController"),r("br"),t._v("UserRelationshipGroupController"),r("br"),t._v("UserFriendRequestController")])]),t._v(" "),r("tr",[r("td",{staticStyle:{"text-align":"left"}},[t._v("群组管理")]),t._v(" "),r("td",{staticStyle:{"text-align":"left"}},[t._v("GroupController"),r("br"),r("br"),t._v("GroupTypeController"),r("br"),r("br"),t._v("GroupQuestionController"),r("br"),t._v("GroupMemberController"),r("br"),r("br"),t._v("GroupBlacklistController"),r("br"),r("br"),t._v("GroupInvitationController"),r("br"),t._v("GroupJoinRequestController")])]),t._v(" "),r("tr",[r("td",{staticStyle:{"text-align":"left"}},[t._v("消息管理")]),t._v(" "),r("td",{staticStyle:{"text-align":"left"}},[t._v("MessageController"),r("br"),t._v("MessageStatusController")])])])])])}),[],!1,null,null,null);e.default=a.exports}}]);