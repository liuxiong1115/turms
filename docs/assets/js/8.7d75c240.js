(window.webpackJsonp=window.webpackJsonp||[]).push([[8],{208:function(t,e,l){"use strict";l.r(e);var a=l(0),v=Object(a.a)({},(function(){var t=this,e=t.$createElement,l=t._self._c||e;return l("ContentSlotsDistributor",{attrs:{"slot-key":t.$parent.slotKey}},[l("h3",{attrs:{id:"业务消息类型"}},[l("a",{staticClass:"header-anchor",attrs:{href:"#业务消息类型"}},[t._v("#")]),t._v(" 业务消息类型")]),t._v(" "),l("h4",{attrs:{id:"提醒"}},[l("a",{staticClass:"header-anchor",attrs:{href:"#提醒"}},[t._v("#")]),t._v(" 提醒")]),t._v(" "),l("p",[t._v("虽然Turms服务端默认支持传递与存储图片、视频、文件等数据，但极其不推荐使用此实现方案。\n推荐的实现方案是使用CDN技术。客户端向您的服务服务端程序请求CDN许可Token，由客户端将带着这个Token找到CDN并上传文件到CDN上，并拿着从CDN那返回的文件URL传递给Turms服务端，由Turms保存这个URL文本，而不保留文件的二进制数据。")]),t._v(" "),l("p",[t._v("又或者通过实现Turms插件来自主定制与部署文件管理服务端集群。")]),t._v(" "),l("h3",{attrs:{id:"基础内容类消息类型"}},[l("a",{staticClass:"header-anchor",attrs:{href:"#基础内容类消息类型"}},[t._v("#")]),t._v(" 基础内容类消息类型")]),t._v(" "),l("table",[l("thead",[l("tr",[l("th",{staticStyle:{"text-align":"left"}},[l("strong",[t._v("消息类型")])]),t._v(" "),l("th",{staticStyle:{"text-align":"left"}},[l("strong",[t._v("功能描述")])])])]),t._v(" "),l("tbody",[l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("文本消息")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("消息内容为普通文本")])]),t._v(" "),l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("图片消息")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("消息内容为描述部分（可选）：图片 URL 地址、尺寸、图片大小"),l("br"),t._v("图片数据（可选）")])]),t._v(" "),l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("语音消息")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("消息内容为描述部分（可选）：语音文件的 URL 地址、时长、大小、格式"),l("br"),t._v("语音数据（可选）"),l("br")])]),t._v(" "),l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("视频消息")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("消息内容为描述部分（可选）：视频文件的 URL 地址、时长、大小、格式"),l("br"),t._v("视频数据（可选）"),l("br")])]),t._v(" "),l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("文件消息")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("消息内容为描述部分（可选）：文件的 URL 地址、大小、格式"),l("br"),t._v("文件数据（可选）"),l("br"),t._v("默认最大支持 16 MB")])]),t._v(" "),l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("地理位置消息")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("消息内容为地理位置标题、地址、经度、纬度信息")])])])]),t._v(" "),l("h3",{attrs:{id:"其他消息类型"}},[l("a",{staticClass:"header-anchor",attrs:{href:"#其他消息类型"}},[t._v("#")]),t._v(" 其他消息类型")]),t._v(" "),l("table",[l("thead",[l("tr",[l("th",{staticStyle:{"text-align":"left"}},[l("strong",[t._v("消息类型")])]),t._v(" "),l("th",{staticStyle:{"text-align":"left"}},[l("strong",[t._v("功能描述")])])])]),t._v(" "),l("tbody",[l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("组合内容类消息")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("消息内容为文本信息与任意个数的其他任意内容类消息类型的消息（如一条消息既包含了文本，也包含了图片与音频）")])]),t._v(" "),l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("通知消息")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("自身并不携带消息内容的通知类消息。主要用于如添加好友、群组邀请、撤回消息、已读回执等的通知")])]),t._v(" "),l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("自定义消息")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("开发者通过“组合内容类消息”自定义的消息类型，例如红包消息、石头剪子布等形式的消息")])]),t._v(" "),l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("系统消息")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("基础内容类消息类型与组合内容类消息均可作为系统消息")])])])]),t._v(" "),l("p",[t._v("配置属性类：im.turms.turms.property.business.Message")]),t._v(" "),l("h3",{attrs:{id:"消息功能"}},[l("a",{staticClass:"header-anchor",attrs:{href:"#消息功能"}},[t._v("#")]),t._v(" 消息功能")]),t._v(" "),l("table",[l("thead",[l("tr",[l("th",{staticStyle:{"text-align":"left"}},[l("strong",[t._v("消息功能")])]),t._v(" "),l("th",{staticStyle:{"text-align":"left"}},[l("strong",[t._v("功能描述")])]),t._v(" "),l("th",[l("strong",[t._v("相关配置")])])])]),t._v(" "),l("tbody",[l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("离线消息")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("实现思路：您可以在Turms客户端每次登陆时，都<主动>向Turms服务端请求关于<该用户在离线状态时，收到的所有私聊与群聊各自具体的离线消息数量，以及各自具体的最后N条消息（默认为1条）>的数据，以此同时兼顾消息的实时性与服务的性能。 默认情况下，Turms服务端<不会>定时删除寄存在Turms服务端的任何离线消息")]),t._v(" "),l("td",[t._v("defaultAvailableMessagesNumberWithTotal")])]),t._v(" "),l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("漫游消息")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("✍在新设备登录时，由开发者自行调用Turms客户端的消息查询接口，指定数量与时段等条件，向Turms服务端请求漫游消息。"),l("br"),t._v("漫游消息的实现本质与“历史消息”的实现一样"),l("br"),t._v("（✍原因：Turms无法自行判断什么是“新设备登陆”）")]),t._v(" "),l("td")]),t._v(" "),l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("多端同步")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("当一名用户有多客户端同时在线时，Turms服务端会将消息下发给该用户所有在线的客户端")]),t._v(" "),l("td")]),t._v(" "),l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("历史消息")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("支持查询用户的历史消息。默认Turms永久存储消息（包括用户消息或系统消息）"),l("br"),t._v("历史消息的实现本质与“漫游消息”的实现一样")]),t._v(" "),l("td")]),t._v(" "),l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("发送消息")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}}),t._v(" "),l("td",[t._v("timeType"),l("br"),t._v("checkIfTargetActiveAndNotDeleted"),l("br"),t._v("maxTextLimit"),l("br"),t._v("maxRecordsSizeBytes"),l("br"),t._v("messagePersistent"),l("br"),t._v("recordsPersistent"),l("br"),t._v("messageStatusPersistent"),l("br"),t._v("messageTimeToLiveHours"),l("br"),l("br"),t._v("allowSendingMessagesToStranger"),l("br"),l("br"),t._v("allowSendingMessagesToOneself"),l("br"),l("br"),t._v("shouldDeleteMessageLogicallyByDefault"),l("br"),t._v("shouldDeletePrivateMessageAfterAcknowledged")])]),t._v(" "),l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("消息撤回")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("撤回投递成功的消息，默认允许发信人撤回距投递成功时间 5 分钟内的消息")]),t._v(" "),l("td",[t._v("allowRecallingMessage"),l("br"),t._v("availableRecallDurationSeconds")])]),t._v(" "),l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("消息编辑")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("编辑已发送成功的消息")]),t._v(" "),l("td",[t._v("allowEditingMessageBySender")])]),t._v(" "),l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("阅后即焚")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("收信人接收到发信人的消息后，收信人客户端会根据发信人预先设定（或默认）的时间按时自动销毁")]),t._v(" "),l("td")]),t._v(" "),l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("已读回执")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("✍通知私聊对象或群组成员中，当前用户已读某条消息"),l("br"),t._v("查看私聊、群组会话中对方的已读/未读状态"),l("br"),t._v("（✍原因：Turms无法得知您的用户在什么情况下算是“已读某条消息”。开发者需要自行调用turmsClient.messageService.readMessage()来告知对方，当前用户已读某条消息）")]),t._v(" "),l("td",[t._v("shouldUpdateReadDateWhenUserQueryingMessage"),l("br"),t._v("ReadReceipt.enabled"),l("br"),t._v("ReadReceipt.useServerTime")])]),t._v(" "),l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("消息转发")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("将消息转发给其他用户或群组")]),t._v(" "),l("td")]),t._v(" "),l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("@某人")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("用于特别提醒某用户。如果Turms客户端检测到已接收的消息中被@的用户为当前登陆中的用户，Turms客户端则会触发@回调函数。开发者可自行实现后续相关业务逻辑。常用于给被@的用户提醒通知。"),l("br"),t._v("群内 @ 消息与普通消息没有本质区别，仅是在被 @ 的人在收到消息时，需要做特殊处理（触发回调函数）")]),t._v(" "),l("td")]),t._v(" "),l("tr",[l("td",{staticStyle:{"text-align":"left"}},[t._v("正在输入")]),t._v(" "),l("td",{staticStyle:{"text-align":"left"}},[t._v("✍当通信中的一方正在键入文本时，告知收信人（一名或多名用户），该用户正在输入消息"),l("br"),t._v("（✍原因：Turms无法得知您的用户是否正在键入文本）")]),t._v(" "),l("td",[t._v("TypingStatus.enabled")])])])])])}),[],!1,null,null,null);e.default=v.exports}}]);