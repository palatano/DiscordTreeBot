package tree.command.data;

import java.util.List;

/**
 * Created by Valued Customer on 8/2/2017.
 */
public class GoogleResults {

    private ResponseData responseData;
    public String toString() { return "ResponseData[" + responseData + "]"; }

    public static class ResponseData {
        private List<Result> results;
        public String toString() { return "Results[" + results + "]"; }
    }

    public static class Result {
        private String url;
        private String title;
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String toString() { return "Result[url:" + url +",title:" + title + "]"; }
    }

}
