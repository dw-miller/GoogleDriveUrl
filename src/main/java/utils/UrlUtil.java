package utils;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

/**
 * 获得下载URL的工具类
 */
public class UrlUtil {
    /**
     *
     * @param url 需要获得下载链接的URL
     * @param client 传入用于执行的客服端
     * @return 真实的下载链接
     */
    public static String getDownloadUrl(String url, WebClient client){
        String prefix=url.substring(0,url.indexOf("/uc?"));//截取，获取下载前缀
        String realUnrl=null;
        try {
            //尝试爬取真实地址
            URL url1 = new URL(url);
            WebRequest webRequest = new WebRequest(url1);
            //使用head请求方式,不返回请求体,因为请求头部已经有需要的信息
            //使用head能大大减少请求所消耗的资源,增加爬取速度和稳定性
            webRequest.setHttpMethod(HttpMethod.HEAD);
            WebResponse webResponse = client.loadWebResponse(webRequest);
            //判断这个头的原因是为了判断是否爬取私有分享链接,这里只留了坑,没有填,因为已经实现了必要的需求了.
            //原因是暂时没有想到办法获得GoogleDrive的登录session.
            //想过直接从当前浏览器拿,但是想不到办法实现.如果继续填坑的话应该会直接写模拟登录会方便很多.
            String responseHeaderValue = webResponse.getResponseHeaderValue("Content-Security-Policy");
            String Location = webResponse.getResponseHeaderValue("Location");
            //根据返回结果判断是否需要二次爬取(要进行确定)
            realUnrl=getLocation(url, client, prefix, responseHeaderValue, Location,5);
        } catch (Exception e) {
            //出现运行时异常,有可能是没有权限,或者文件已失效,修改,移动等问题
            realUnrl="-2";
        }finally {
            client.close();
        }
        return realUnrl;
    }

    /**
     * 这里是通过拦截重定向请求,判断重定向的地址域名来判断是否为真实下载地址
     * 实际应用也是应用于其他文件下载爬取的一种思路
     * @param url 需要获得下载链接的URL
     * @param client 传入用于执行的客服端
     * @return 真实的下载链接
     */
    public static String getLocation(String url, WebClient client, String prefix, String responseHeaderValue, String location,int retry) throws IOException,Exception {
        String realUnrl=null;
        URL url1;
        WebRequest webRequest;
        WebResponse webResponse;
        //域名对的上,说明这是真实地址
        if(location !=null&& location.contains("docs.googleusercontent.com")){
            realUnrl=location;
        //否则,可能是文件过大,需要确定下载
        }else if(responseHeaderValue!=null&&responseHeaderValue.contains("unsafe-inline")){//认为是大文件，需要跳转确认才能下载
            //设置允许自动重定向，否则页面无法正常跳转
            //client.getOptions().setRedirectEnabled(true);
            
            client.getCookieManager().setCookiesEnabled(true);
            
            HtmlPage page = client.getPage(url);
            url=page.getWebResponse().getWebRequest().getUrl().toString();
            prefix=url.substring(0,url.indexOf("/uc?"));//截取，获取下载前缀,用于与后面uri拼接
            /*page.getWebResponse().getResponseHeaders().stream().forEach(e->{
                System.out.println(e.getName());
                System.out.println(e.getValue());
                System.out.println();
            });*/
            
            String[] split = page.getWebResponse().getResponseHeaderValue("Set-Cookie").split("; ");//获得response head里的Set-Cookie
            String[] content=split[0].split("=");//第一个是name=value的键值对,以'='分割开来
            String _name=content[0];//获得cookie的name
            String _value=content[1];//获得cookie的value
            String _domain=split[1].substring(7);//获得cookie的domain
            String _Expires=split[2].substring(8);//获得cookie的有效时间文本
            Date _Expires_date=new Date(_Expires);//根据文本转成Date格式
            String _path=split[3].substring(5);//获得cookie的path
            //简单粗暴延长cookie有效时间,因为从response获得的时间就是你进入页面的时间,所以写不入cookie,必须手动延长
            //这里是被坑了很久,没有这个cookie,会一直在确认页面打转.所以必须要手动从响应头中获取并写入
            //然后手动写入时遇到无法写入的问题,addCookie方法一直无法写入,原因就是有效期过了.
            _Expires_date.setYear(_Expires_date.getYear()+1);

            //根据获得的参数创建cookie
            Cookie cookie = new Cookie(_domain, _name, _value, _path, _Expires_date, true, true);
            //添加到client的cookies中
            //这里注意,翻看官方文档才知道,对于失效和不合法的cookie完全无法添加,而且没有任何提示和反应
            client.getCookieManager().addCookie(cookie);

            //获得confirm的地址(前缀+uri拼接)
            String href = prefix+ page.getHtmlElementById("uc-download-link").getAttribute("href");
            url1 = new URL(href);
            webRequest = new WebRequest(url1);

            //head方式请求
            webRequest.setHttpMethod(HttpMethod.HEAD);
            webResponse = client.loadWebResponse(webRequest);
            //responseHeaderValue = webResponse.getResponseHeaderValue("Content-Security-Policy");
            location = webResponse.getResponseHeaderValue("Location");
            /*client.getCookieManager().getCookies().stream().forEach(e->{
                System.out.println(e.toString());
            });*/
            if(!(location !=null&& location.contains("docs.googleusercontent.com"))){

               /* //这里是设置重置的代码,其实效果并不是太好,太影响效率,所以注释掉了
                if(retry>0){
                    realUnrl=getLocation(href, client, prefix, responseHeaderValue, href,retry-1);
                }else{
                    realUnrl="0";//返回0认为是无法正确获得下载链接
                }*/
                realUnrl="0";//返回0认为是无法正确获得下载链接
            }else{
                realUnrl=location;
            }
        }else{
            realUnrl="-1";//返回-1认为是文件是私密文件，不是共享文件，无法通过权限认证
        }
        return realUnrl;
    }
}
