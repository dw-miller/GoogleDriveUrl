# GoogleDriveUrl
GoogleDrive中文件夹下载的小工具之一,弃坑前版本,写完后不久换电脑后只用Ubuntu,IDM用不了,也改用了rclone管理云盘,所以就直接弃坑了,当然单纯下载肯定IDM速度和便利性更高.请结合自带的第三方应用download link generator,IDM,和IDM批量导入助手使用,可以实现GoogleDrive文件夹下载.(将文件夹内所有文件通过IDM有层次地将文件夹结构批量下载)

# 使用方式
因为没有模拟登录,所以解析的文件夹需要分享为公共文件(下完了就取消掉就好了).
要用到的首先是GoogleDrive第三方应用"Download Link Generator For Google Drive"(直接在GoogleDriven添加第三方应用,搜索Download Link Generator添加即可)
有了它可以在GoogleDrive中对文件夹右键,用它打开,就能批量得到文件夹内文件的下载链接.将输出的内容全部贴到"需要转换的链接集.txt"中,包括前面的文件名噢.
可以在proxy.propertiese配置代理.运行主函数.下载链接和文件名会以工具 IDM批量导入助手.exe 的导入格式输出到 "需要转换的链接集"的两个文件中.
其中.downlist后缀的文件是IDM批量导入助手.exe 导入的格式,用IDM批量导入助手选择文件导入,就可以在IDM中进行下载,下载好的文件就是按照GoogleDrive中的目录层次结构放置的

"IDM批量导入助手"还请自行搜索并下载.
如果导入乱码,请记事本打开.downlist并另存为编码为ANSI格式.
