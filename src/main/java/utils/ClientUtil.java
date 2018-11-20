package utils;



import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;

import java.io.*;
import java.util.Properties;
import java.util.Set;


/**
 * 客服端工具类
 */
public class ClientUtil {

    /**
     * 获得一个拥有SSL认证的客服端
     * @return 获得一个拥有SSL认证的客服端
     */
    public static WebClient getNewClient(){
        WebClient client=new WebClient(BrowserVersion.getDefault());
        //设置不允许自动重定向
        client.getOptions().setRedirectEnabled(false);
        //关闭js
        client.getOptions().setJavaScriptEnabled(false);
        client.setJavaScriptTimeout(5000);
        //忽略ssl认证
        client.getOptions().setUseInsecureSSL(true);
        //禁用Css，可避免自动二次请求CSS进行渲染
        client.getOptions().setCssEnabled(false);
        //运行错误时，不抛出异常
        client.getOptions().setThrowExceptionOnScriptError(false);
        //设置超时时间
        client.getOptions().setTimeout(60000);
        // 设置Ajax异步
        client.setAjaxController(new NicelyResynchronizingAjaxController());
        //开启cookie
        client.getCookieManager().setCookiesEnabled(true);

        Set<Cookie> cookies=client.getCookieManager().getCookies();

        //根据配置设置是否使用代理
        client=getProxy(client);

        return client;
    }

    /**
     *
     * @param client
     * @return 根据配置文件设置代理
     */
    public static WebClient getProxy(WebClient client){
        Properties properties=new Properties();
        //获得项目目录下的代理配置文件
        File file = new File("proxy.properties");
        try (FileInputStream fis=new FileInputStream(file);){//如果没有找到就创建一个默认的代理配置文件,方便后面修改
            properties.load(fis);
        } catch (IOException e) {
            try ( BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));){
                bufferedWriter.write("usedProxy=false");//默认不使用代理
                bufferedWriter.newLine();
                bufferedWriter.write("proxyHost=127.0.0.1");//代理主机ip
                bufferedWriter.newLine();
                bufferedWriter.write("proxyPort=8087");//代理端口
                bufferedWriter.flush();
                try (FileInputStream fis=new FileInputStream(file);){
                    properties.load(fis);
                }catch (IOException e2){}
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        //读取配置文件信息并写入
        if(properties.getProperty("usedProxy","false").equals("true")){
            String proxyHost = properties.getProperty("proxyHost","127.0.0.1");
            String proxyPortStr =properties.getProperty("proxyPort","8087");
            Integer proxyPort = Integer.valueOf(proxyPortStr);
            ProxyConfig proxyConfig = client.getOptions().getProxyConfig();
            proxyConfig.setProxyHost(proxyHost);
            proxyConfig.setProxyPort(proxyPort);
        }
        return client;
    }
}
