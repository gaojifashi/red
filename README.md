# red
rob redpacket automatically

应用说明：

本应用通过监听微信发出的通知，来识别微信红包以及自动完成拆红包的操作。
开发本应用目的纯属学习及娱乐，无任何商业用途。


使用说明：

1.抢红包服务需要Accessibility权限，授予权限即开启服务；反之，取消授权则关闭服务；

2.当前聊天窗口内的红包不会识别，因为它们不是以通知的形式出现在通知栏，本应用无法识别；

3.默认情况下，服务在黑屏(或锁屏)状态下暂停(休眠)，因为黑屏状态无法获得界面信息；

4.应用支持自动唤醒功能，即在黑屏(锁屏)状态下识别红包后唤醒屏幕，但要求用户不得设置锁屏
	(即无需滑动，无需输入按键或图案密码)，否则该功能会增加耗电

5.Flyme 5及更高级的系统需打开Flyme 'Home'，因为Home = Back(操作上挺好的，编程上简直反人类)


注意(免责声明)：
1.安卓中Accessibility是系统级服务，所需权限属高危权限，非信任软件不应授予权限。
	此权限可用于监听通知(如获取短信内容，进而得到某些验证码哦~)、
	模拟点击(某些软件厂商用来实现自动安装应用哦，信不信秒秒钟帮你装个全家桶)、获取隐私信息等。。

2.国内定制安卓系统百花齐放(乱七八糟)，某些系统竟然会将系统服务杀死而不重新启动。
	为避免这种情况，可能(我不确定是由于这个原因)需将该应用添加定制的权限管理的白名单中，
	或者在多任务界面给该应用加锁。

3.手机反应速度、网络拥塞状况等都可能影响到本助手的正常运行，因为程序对界面改变的等待时间0.5s
	(考虑效率，至少比大部分人操作的速度快才能抢到红包嘛)，在该时间段内无其他事件发生则认为设备空闲
	(其实手机卡住了也无其他时间发生...)

4.根据微信官方公告，用户使用抢红包助手可能导致账号冻结哦(个人认为，微信检测不出来，
	因为在安卓系统中，每个应用都在其沙盒中运行，即使微信能获取当前运行应用列表，
	也不清楚哪个应用监听它发出的通知)

5.本应用严重依赖微信界面，红包界面改变或版本更新均有可能导致服务非正常运行。

...中间省略无数情况

上述情况的锅，我不背哈0.0
