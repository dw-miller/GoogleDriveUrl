package main;

import utils.ClientUtil;
import utils.UrlUtil;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class GetDownload {
    private  static ConcurrentHashMap<String,String> urlMap=new ConcurrentHashMap<>();//存储链接的map
    private  static ConcurrentHashMap<String,String> errMap=new ConcurrentHashMap<>();//存储失败链接的map
    private  static ConcurrentHashMap<String,String> logMap=new ConcurrentHashMap<>();//存储日志的map
    private static SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");//简单的日期转换器
    private static long totalCount=0;//总数目
    private static long curCount=0;//当前完成数目
    private static long curThread=0;//当前线程数目

    public static void main(String[] args) {
        final int MAX_CURRENT_THREAD_COUNT=20;
        Date start=new Date();
        File urlListFile=new File("需要转换的链接集.txt");//需要转换的URL集的文件
        File change_ListFile=new File("转换成功的链接集.txt");//转换成功的链接集的text文件
        File error_ListFile=new File("转换失败的链接集.txt");//转换失败的链接的文件
        File logFile=new File("log.txt");//日志文件
        File downlist=new File("转换成功的链接集.downlist");//转换成功的链接集的downlist文件,用于导入IDM
        //WebClient newClient = ClientUtil.getNewClient();
        //初始化文件结构
        try {
            if (!urlListFile.exists()) {
                urlListFile.createNewFile();
            }
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try(InputStreamReader urlListFile_in_count_only = new InputStreamReader(new FileInputStream(urlListFile),"UTF-8");
            InputStreamReader urlListFile_in = new InputStreamReader(new FileInputStream(urlListFile),"UTF-8");
            BufferedReader urlListFile_reader_count_only = new BufferedReader(urlListFile_in_count_only);
            BufferedReader urlListFile_reader = new BufferedReader(urlListFile_in);
            BufferedWriter change_ListFile_wr=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(change_ListFile,false)));
            BufferedWriter error_ListFile_wr=new BufferedWriter(new FileWriter(error_ListFile,false));
            BufferedWriter logFile_wr=new BufferedWriter(new FileWriter(logFile,true));
            OutputStreamWriter downlist_ows=new OutputStreamWriter(new FileOutputStream(downlist,false));
            ){
            //流只能使用一次,这里第一次只是为了读出链接总数
            totalCount=urlListFile_reader_count_only.lines().count();
            urlListFile_reader_count_only.close();
            urlListFile_in_count_only.close();
            //下面是具体获取链接所需使用的流
            Stream<String> lines = urlListFile_reader.lines();
            lines.map(line->{
                //totalCount++;
                int sp = line.lastIndexOf(" https");
                String link=line.substring(sp+1);
                String name=line.substring(0,sp);
                return new String[]{name,link};
            }).forEach(line->{
                //限制一下最大线程数目,虽然不占太多内存,但是太多请求同时访问Google估摸会被关进小黑屋
                while (curThread>=MAX_CURRENT_THREAD_COUNT){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("=======共"+totalCount+"个 / 已完成"+curCount+"个=======");
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        curThread++;
                        String downloadUrl = UrlUtil.getDownloadUrl(line[1], ClientUtil.getNewClient());
                        if ("0".equals(downloadUrl)){
                            String log_msg="--msg:真实地址获取失败，需手动获取 --time:"+simpleDateFormat.format(System.currentTimeMillis());
                            String err_msg=String.format("%s %s",line[0],line[1]);
                            toWriteToFile(log_msg, err_msg, logFile_wr, line, error_ListFile_wr);
                        }else if("-1".equals(downloadUrl)){
                            String log_msg="--msg:这是私密链接,或者文件已失效,修改,移动等问题，请分享成为公开链接或检查文件内容再试 --time:"+simpleDateFormat.format(System.currentTimeMillis());
                            String err_msg=String.format("%s %s",line[0],line[1]);
                            toWriteToFile(log_msg, err_msg, logFile_wr, line, error_ListFile_wr);
                        }else if("-2".equals(downloadUrl)){
                            String log_msg="--msg:服务器没有响应（404），网络波动或者需要一个正确的代理服务器 --time:"+simpleDateFormat.format(System.currentTimeMillis());
                            String err_msg=String.format("%s %s",line[0],line[1]);
                            toWriteToFile(log_msg, err_msg, logFile_wr, line, error_ListFile_wr);
                        }else{
                            String log_msg="--msg获取成功 --time:"+simpleDateFormat.format(System.currentTimeMillis());
                            String suc_msg=String.format("filename=%s&fileurl=%s",line[0],downloadUrl);
                            toWriteToFile(log_msg, suc_msg, logFile_wr, line, change_ListFile_wr);
                            try {
                                downlist_ows.write(suc_msg.toString()+"\r\n");
                                downlist_ows.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        curCount++;
                        try {
                            change_ListFile_wr.flush();
                            error_ListFile_wr.flush();
                            logFile_wr.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        curThread--;
                    }
                }).start();
            });
            while(curCount<totalCount){
                System.out.println("=======共"+totalCount+"个 / 已完成"+curCount+"个=======");
                Thread.sleep(1000);
            }

        }catch (IOException e){
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Date end = new Date();
        System.out.println("转换完成,耗时"+(end.getTime()-start.getTime())/1000+"秒");
    }

    private static void toWriteToFile(String log_msg, String err_msg, BufferedWriter logFile_wr, String[] line, BufferedWriter error_ListFile_wr) {
        System.out.println("["+line[0]+"]  "+log_msg);
        try {
            logFile_wr.write("["+line[0]+"]  "+log_msg);
            logFile_wr.newLine();
            error_ListFile_wr.write(err_msg);
            error_ListFile_wr.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
