# turms-client-js

## 介绍

turms-client-js基于WebSocket协议与严格模式的Typescript编写，对外接口遵循良好的设计理念，并提供.d.ts文件用来保证代码提示功能，让开发者在甚至从没用过turms-client-js接口与读过文档的情况下，也能在1分钟内直接上手写业务逻辑。开发者仅需根据自己的业务场景调用相关回调函数与发起Promise请求即可实现“业务实现列表”中的所有功能。

特别注意：turms-client-js不允许类似于socket.io的WebSocket服务降级处理操作，因此除了WebSocket的握手请求外，turms-client-js不会使用到HTTP(S)协议。

## 对外逻辑结构

提醒：turms-client-js**所有的请求服务操作都基于Promise编写**。

- TurmsClient：turms-client-js唯一对外暴露的类，一个TurmsClient代表着一个客户端与服务端之间的连接。以下变量是TurmsClient对外的成员变量。
  - driver：TurmsClient的运行驱动。负责连接的开起关闭、消息的发送接收与心跳控制等操作。
  - userService：用户相关请求服务。如用户登陆、添加好友、添加关系人分组、发送/处理好友请求等操作。
  - groupService：群组相关请求服务。如创建群组、变更群主、修改群成员角色、修改群信息等操作。
  - messageService：消息相关操作。如发送消息、修改已发送消息、查询各类消息与其状态、撤回消息等操作。

## 业务逻辑的认证与授权

对于客户端发来的请求参数，Turms服务端的态度是“客户端的请求参数均不可信”，因此Turms服务端会根据您在Turms服务端处所设定的业务配置，自行做各种必要的权限判断。

如“修改已发送消息”功能，就会触发一系列判定逻辑。Turms会先判断目标消息是该用户发出的，再根据您在Turms服务端配置的allowEditingMessageBySender（默认为true），来判断是否允许用户修改已发送消息，若您设置其为false，则在客户端处会catch到一个TurmsError对象，而它由业务状态码code与reason状态码描述信息组成。

再比如对于一个“简单”的“发送消息”请求，Turms服务端就会判断您是否设置了“允许发送消息给陌生人（非关系人）”、消息发送者是否在黑名单中。如果接收方是群组，那么消息发送者是否是群成员，并且是否处于禁言状态等等逻辑判断。而您仅仅只需调用一个sendMessage接口即可。

## Quick Start

注意事项：**由于现代浏览器自身限制，您的执行环境必须运行于http(s)协议之上**，例如：http://localhost:63342/turms-client-js/demo/demo.html?_ijt=mtubsl7jusb2fp2qjl1l3blodt（用Intellij Idea直接打开HTML页面，即可直接运行于HTTP服务器上）。否则，turms-client-js将无法执行客户端的登陆操作。

1. 引入js文件<script src="../dist/turms-client.min.js"></script>
   （之后会发布到公共仓库）
2. 编写业务逻辑。如下所示。

鉴于TurmsClient清晰易懂的设计与一目了然的接口，故不讲解下述代码。

```javascript
const client = new TurmsClient();

client.driver.onClose = (error) => {
    if (error) {
        console.error(error);
    } else {
        console.info('bye bye');
    }
};

client.driver.onMessage = (notification) => {
    if (notification.relayedRequest) {
    	console.log('Notification: ' + JSON.stringify(notification));
    }
};

client.userService.login(1, "123")
    .then(() => {
    	client.userService.queryUsersIdsNearby(
            139.667651,
            35.792657,
        	100,
        	10)
        	.then(ids => {
        		console.log("user ids: ", ids);
    		});
    	client.messageService.sendMessage(
        	"PRIVATE",
        	1,
        	new Date(),
        	"Hello Turms",
        	null,
        	30)
        	.then(id => {
            	console.log('message ' + id + ' has been sent');
        	});
    	client.groupService.createGroup(
            'Turms Developers Group',
            'This is a group for the developers who are interested in Turms',
            'nope',
            null,
            null)
            .then(id => {
            	console.log('group ' + id + ' has been created');
        	});
	})
    .catch(reason => {
    	console.error(reason);
	});
```

### 深入详解（TODO）

